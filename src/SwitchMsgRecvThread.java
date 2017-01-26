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
				sw.printNeighbors();
				break;
			
			case "MsgKeepAlive":
				MsgKeepAlive msg2 = (MsgKeepAlive)obj;
				Node n = sw.neighborMap.getOrDefault(msg2.id, null);
				if(n == null) break; //KeepAlive may come before RegisterResponse, the neighborMap is still empty
				System.out.println("Received KEEP_ALIVE from id:" + msg2.id);

				//if node was not alive before, update routing
				if(!n.alive) {
					n.update(msg2.id, recvPacket.getAddress().getHostName(), recvPacket.getPort(), true);
					MsgTopologyUpdate.send(sw, true);
				}
				sw.aliveNeighborSet.add(n);
				break;
				
			case "MsgRouteUpdate":
				
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
			System.err.println("Switch Receive Error 001");
		}
		try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			return in.readObject();
		} catch (ClassNotFoundException e) {
			System.err.println("Switch Receive Error 002");
		} catch (IOException e1) {
			System.err.println("Switch Receive Error 003");
		}
		return null;
	}	
	
	
	
}
