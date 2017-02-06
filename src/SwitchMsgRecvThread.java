import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;

public class SwitchMsgRecvThread extends Thread {
	private Switch sw;
	private byte[] buffer;
	private DatagramPacket recvPacket;
	private static final int BUF_SIZE = 10240;
	
	public SwitchMsgRecvThread(Switch sw) {
		this.sw = sw;
		buffer = new byte[BUF_SIZE];
		recvPacket = new DatagramPacket(buffer, BUF_SIZE);
	}
	
	public void run() {
		//1: receive from neighbor's "KEEP_ALIVE" or controller's "ROUTE_UPDATE" 
		
		while(true) {
			Object obj = this.recv();
			if(obj == null) {
				continue;
			}
			String msgType = obj.getClass().getSimpleName();
			
			switch(msgType) {

			case "MsgRegisterResponse":
				MsgRegisterResponse msg1 = (MsgRegisterResponse)obj;
				//Read in neighbors
				for(Node n : msg1.neighbors) {
					n.noResponseTime = 0;
					sw.neighborMap.put(n.id, n);
				}
				//For LOG
				sw.log.println("Switch ID: " + sw.id + "received REGISTER_RESPONSE:");
				sw.printNeighbors();
				
				
				//Immediately send KEEP_ALIVE to alive neighbors
				for(Node n : sw.neighborMap.values()) {
					if(n.alive && !sw.failedIds.contains(n.id)) {
						MsgKeepAlive.send(sw, n);
					}
				}
				break;
			
			case "MsgKeepAlive":
				MsgKeepAlive msg2 = (MsgKeepAlive)obj;
				
				if(sw.failedIds.contains(msg2.id)) break;   //simulate failed linked
				
				Node n = sw.neighborMap.getOrDefault(msg2.id, null);
				if(n == null) break; //KeepAlive may come before RegisterResponse, the neighborMap is still empty
				
				//For LOG
				if(!sw.failedIds.contains(n.id)) {
					sw.log.println("Received KEEP_ALIVE from ID: " + msg2.id);
				}

				//if node was not alive before, update routing
				if(!n.alive) {
					//For LOG
					sw.log.println("Found New Alive Switch ID:" + msg2.id);
					n.update(msg2.id, recvPacket.getAddress().getHostName(), recvPacket.getPort(), true);
					MsgTopologyUpdate.send(sw, true);  //update immediately as the project requires.
				}
				sw.aliveNeighborSet.add(n);
				
				break;
				
			case "RouteUpdate":
				//For LOG
				sw.log.println("New Routing Table Received");
				RouteUpdate msg = (RouteUpdate)obj;
				int[] table = msg.nextHopToDestination;
				for(int i = 0; i < table.length; i ++) {
					sw.log.print(table[i] + "  ");
				}
				sw.log.println();		
				break;
			default:
				break;
			
			
			}
		}

	}
	

	
	public Object recv() {
		recvPacket.setLength(BUF_SIZE);
		try {
			sw.socket.receive(recvPacket); //receive() is thread safe
		} catch (IOException e1) {
			sw.log.errPrintln("Switch Receive Error 001");
		}
		try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			return in.readObject();
		} catch (ClassNotFoundException e) {
			sw.log.errPrintln("Switch Receive Error 002");
		} catch (IOException e1) {
			sw.log.errPrintln("Switch Receive Error 003");
		}
		return null;
	}	
	
	
	
}
