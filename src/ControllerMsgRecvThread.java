import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashSet;

public class ControllerMsgRecvThread extends Thread {
	
	private Controller controller;
	private byte[] buffer;
	private DatagramPacket recvPacket;
	private static final int BUF_SIZE = 10240;
	
	public ControllerMsgRecvThread(Controller controller) {
		this.controller = controller;
		buffer = new byte[BUF_SIZE];
		recvPacket = new DatagramPacket(buffer, BUF_SIZE);
	}
	
	public void run() {
		//receive from switch's "TopologyUpdate" or "RegisterRequest" 
		
		while(true) {
			Object obj = this.recv();
			
			if(obj == null) {
				continue;
			}
			String msgType = obj.getClass().getSimpleName();
			
			switch(msgType) {
			case "MsgTopologyUpdate":
				MsgTopologyUpdate msg1 = (MsgTopologyUpdate)obj;
				Node n1 = controller.nodeMap.getOrDefault(msg1.id, null);
				
				if(n1 == null) {
					//not required in this project:
					//when controller restarts but switches still sending TopologyUpdate
					//should register
					break;
				}
				
				//For LOG
				if(msg1.needUpdate) {
					controller.log.println("Controller Received TOPOLOGY_UPDATE from Switch ID: " + msg1.id);
				}
				
				boolean needUpdate = false;
				
				synchronized (controller) {
					//1. Remember alive nodes, find node from alive to dead in periodic check
					controller.aliveNodeArr[msg1.id - 1] = 1;
					//2. Node from dead to alive,  needUpdate immediately
					if(!n1.alive) {
						n1.update(msg1.id, recvPacket.getAddress().getHostName(), recvPacket.getPort(), true);
						needUpdate = true;
						//For LOG
						controller.log.println("Found Switch-" + n1.id + "Became Alive!");
					}
	
					//3. Update edges
					if(msg1.needUpdate) {
						HashSet<Integer> neighborIdSet = new HashSet<Integer>(msg1.neighborIds);
						for(int id = 1; id <= controller.numNodes; id ++) {
							if(controller.aliveBwGraph[msg1.id - 1][id - 1] == 0 && neighborIdSet.contains(id)) {
								//new active edge
								needUpdate = true;
								controller.nodeMap.get(id).setAlive(true);
								controller.aliveBwGraph[msg1.id - 1][id - 1] = controller.originalBwGraph[msg1.id - 1][id - 1];
								controller.aliveBwGraph[id - 1][msg1.id - 1] = controller.originalBwGraph[id - 1][msg1.id - 1];
								controller.log.println("Detected a New Active Link Between " + "Switch-" + msg1.id + " and " + "Switch-" + id);
							}
							else if(controller.aliveBwGraph[msg1.id - 1][id - 1] != 0 && !neighborIdSet.contains(id)) {
								//new inactive edge
								needUpdate = true;
								controller.aliveBwGraph[msg1.id - 1][id - 1] = 0;
								controller.aliveBwGraph[id - 1][msg1.id - 1] = 0;
								controller.log.println("Detected a New Inactive Link Between " + "Switch-" + msg1.id + " and " + "Switch-" + id);
								//if all this id's edges are failed, set node id to be dead
//								int sum = 0;
//								for(int i : controller.aliveBwGraph[id - 1]) {
//									sum += i;
//								}
//								if(sum == 0) {
//									controller.nodeMap.get(id).setAlive(false);
//								}
								
								
							}
						}
					}
				}	
				//4. Broadcast new route table
				if(needUpdate) {  //found graph change after check
					//for LOG
					controller.log.println("Computing New Route Table");
					synchronized(controller) {
						RoutingStrategy routingStrategy = new RoutingStrategy(controller);
						int[][] routingtable = routingStrategy.computeRouteTable();
						RouteUpdateToAll routeupdatetoall = new RouteUpdateToAll(controller, routingtable);
						routeupdatetoall.sendRouteUpdateToAllNodes();
					}
					
				}else if(msg1.needUpdate){
					//msg request update but found no graph change
					if(controller.regSet.size() == controller.numNodes){
						controller.log.println("Route table already updated");
					}else{
						controller.log.println("Waiting for all nodes registering");
					}
				}

				break;
			case "MsgRegisterRequest":
				MsgRegisterRequest msg2 = (MsgRegisterRequest)obj;

				//1. Read in switch info
				int swID = msg2.id;
				InetAddress swIP = recvPacket.getAddress();
				int swPort = recvPacket.getPort();
				
				//For LOG
				controller.log.println("Received REGISTER_REQUEST From Switch-" + swID);
				
				//2. Check the switch availability
				Node n = controller.nodeMap.getOrDefault(swID, null);
				if(n == null ) {
					continue;   
					//when id is not in initial graph, do not respond to this switch
				}
				
				//3.Update switch Node and edge info, and the edges related
				synchronized (controller) {
					n.update(swID, swIP.getHostName(), swPort, true);
					controller.aliveNodeArr[swID - 1] = 1;
					//set edges from n to active
					//set edges to n to active
					for(int id = 1; id <= controller.numNodes; id ++) {
						if(controller.originalBwGraph[swID - 1][id - 1] != 0) {
							if(controller.nodeMap.get(id).alive) {
								controller.aliveBwGraph[swID - 1][id - 1] = controller.originalBwGraph[swID - 1][id - 1];
								controller.aliveBwGraph[id - 1][swID - 1] = controller.originalBwGraph[id - 1][swID - 1];
							}
						}
					}
				}
				
				
				//4. send REGISTER_RESPONSE with all the neighbors of the switch
				MsgRegisterResponse.send(controller, n);
				
				//5. trigger route compute when all switches regsitered
				controller.regSet.add(n);
				if(controller.regSet.size() == controller.numNodes) {
					controller.allRegisteredSignal.release();
				}
				
				break;
			default:
				break;
			
			
			}
		}

	}
	

	
	public Object recv() {
		recvPacket.setLength(BUF_SIZE);
		try {
			controller.socket.receive(recvPacket); //receive() is thread safe
		} catch (IOException e1) {
			controller.log.errPrintln("Controller Receive Error 001");
		}
		try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			return in.readObject();
		} catch (ClassNotFoundException e) {
			controller.log.errPrintln("Controller Receive Error 002");
		} catch (IOException e1) {
			controller.log.errPrintln("Controller Receive Error 003");
		}
		return null;
	}	
	

}
