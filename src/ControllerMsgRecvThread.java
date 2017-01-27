import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
				if(!msg1.needUpdate) 
					break;
				HashSet<Integer> neighborIdSet = new HashSet<Integer>(msg1.neighborIds);
				
				//NOTES:
				//one case is new alive neighbor is added, this results in a new active edge (not an new alive node, because the node won't send until controller knows it's alive)
				//the other case is some node becomes not alive, this results in a new inactive edge (not a new inactive node, because the switch does not know whether the neighbor is alive or not)
				//so we only need to check edgeMap
				ArrayList<Edge> edges = controller.edgeMap.get(controller.nodeMap.get(msg1.id));
				boolean needUpdate = false;
				for(Edge edge : edges){
					//For TEST:
					System.out.println("Edge Info: " + edge.from.id + " to " + edge.to.id + ", activity: " + edge.active + " Feedback: " + neighborIdSet.contains(edge.to.id));
					
					
					
					if(edge.active && !neighborIdSet.contains(edge.to.id)) {
						//edge no long active
						edge.active = false;
						needUpdate = true;
					}else if(!edge.active && neighborIdSet.contains(edge.to.id)){
						//edge becomes active
						edge.active = true;
						needUpdate = true;
					}
				}
				if(needUpdate) {  //really needs update after check
					//for LOG
					System.out.println("Needs Route Update");
					//TODO
					//compute route
					//send MsgRouteUpdate
				}else {
					System.out.println("Needs Not Route Update");
				}
				
				
				
				break;
			case "MsgRegisterRequest":
				MsgRegisterRequest msg2 = (MsgRegisterRequest)obj;

				//1. Read in switch info
				int swID = msg2.id;
				InetAddress swIP = recvPacket.getAddress();
				int swPort = recvPacket.getPort();
				
				//For LOG
				System.out.println("Received REGISTER Request From Id: " + swID);
				
				//2. Check the switch availability
				Node n = controller.nodeMap.getOrDefault(swID, null);
				if(n == null || controller.regSet.contains(n)) {
					continue;   
					//when id is not in initial graph, do not respond to this switch
					//when id is repeatedly registered, with another port for example, do not respond to this switch
				}
				
				//3.Update switch Node info, and the edges related
				n.update(swID, swIP.getHostName(), swPort, true);
				//set edges from n to active
				for(Edge edge : controller.edgeMap.get(n)) {
					if(!edge.to.alive) 
						continue;
					
					edge.active = true;
					//set edges to n to active
					for(Edge edge2 : controller.edgeMap.get(edge.to)) {
						if(edge2.to == n) {
							edge2.active = true;
							//For TEST:
							System.out.println("Edge " + edge2.from.id + " to " + edge2.to.id + " is set active");
						}
					}
				}
				
				//4. send REGISTER_RESPONSE with all the neighbors of the switch
				MsgRegisterResponse.send(controller, n);
				
				//5. trigger route compute when all switches regsitered
				controller.regSet.add(n);
				if(controller.regSet.size() == controller.nodeMap.size()) {
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
			System.err.println("Controller Receive Error 001");
		}
		try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			return in.readObject();
		} catch (ClassNotFoundException e) {
			System.err.println("Controller Receive Error 002");
		} catch (IOException e1) {
			System.err.println("Controller Receive Error 003");
		}
		return null;
	}	
	

}
