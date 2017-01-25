import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MsgTopologyUpdate implements java.io.Serializable {
	boolean needUpdate = false;
	public ArrayList<Node> neighbors;
	public MsgTopologyUpdate(boolean needUpdate, ArrayList<Node> neighbors) {
		this.needUpdate = needUpdate;
		if(needUpdate) {
			this.neighbors = neighbors;
		}else {
			this.neighbors = null;
		}
		
	}
	//para: the switch which is sending the msg, whether the msg needUpdate
	public static void send(Switch sw, boolean needUpdate) {
		ArrayList<Node> neighbors = null;
		if(needUpdate) {
			neighbors = new ArrayList<Node>(sw.neighborMap.values());
		}
		MsgTopologyUpdate msg = new MsgTopologyUpdate(needUpdate, neighbors);
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
