import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MsgTopologyUpdate implements java.io.Serializable {
	int id;
	boolean needUpdate = false;
	public ArrayList<Integer> neighborIds;
	public MsgTopologyUpdate(int id, boolean needUpdate, ArrayList<Integer> neighborIds) {
		this.id = id;
		this.needUpdate = needUpdate;
		if(needUpdate) {
			this.neighborIds = neighborIds;
		}else {
			this.neighborIds = null;
		}
		
	}
	//para: the switch which is sending the msg, whether the msg needUpdate
	public static void send(Switch sw, boolean needUpdate) {
		ArrayList<Integer> neighborsIds = null;
		if(needUpdate) {
			neighborsIds = new ArrayList<Integer>(); //only alive neighbors (means active links) are sent
			for(Node n : sw.neighborMap.values()){
				if(n.alive) {
					neighborsIds.add(n.id);
				}
			}
		}
		MsgTopologyUpdate msg = new MsgTopologyUpdate(sw.id, needUpdate, neighborsIds);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(msg);
            byte[] buf = bos.toByteArray();
            DatagramPacket p = new DatagramPacket(buf, buf.length, sw.controllerIPAddress, sw.controllerPort);
            sw.socket.send(p); //ip and port are already in receivePacket
        } catch (IOException e) {
			System.err.println(sw.id + " Sending TOPOLOGY_UPDATE to controller failed");
		}
	}

	
}
