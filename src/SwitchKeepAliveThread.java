import java.net.DatagramPacket;

public class SwitchKeepAliveThread extends Thread {
	Switch sw;
	public SwitchKeepAliveThread(Switch sw) {
		this.sw = sw;
	}
	
	public void run() {
		//1: receive from neighbor's "KEEP_ALIVE" msg
		byte[] buffer = new byte[1024];
		DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
		while(true) {
			recvPacket.setLength(buffer.length);
			MsgKeepAlive msg = MsgKeepAlive.recv(sw, recvPacket);
			
			//check the node, add the node to keep_alive buffer set
			if(msg != null) {
				Node n = sw.neighborMap.get(msg.id);
				
				System.out.println("Received KEEP_ALIVE from id:" + msg.id);
				
				
				//if node was not alive before, update routing
				if(!n.alive) {
					n.update(msg.id, recvPacket.getAddress().getHostName(), recvPacket.getPort(), true);
					MsgTopologyUpdate.send(sw, true);
				}
				sw.aliveNeighborSet.add(n);
			}
		}

	}
	

	
	
}
