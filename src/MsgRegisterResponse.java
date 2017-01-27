import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MsgRegisterResponse implements java.io.Serializable{

	public ArrayList<Node> neighbors;
	public MsgRegisterResponse() {
		neighbors = new ArrayList<Node>();
	}
	
	
	public static void send(Controller controller, Node target) {
		MsgRegisterResponse resp = new MsgRegisterResponse();
        ArrayList<Edge> edges = controller.edgeMap.get(target);
       
        for(Edge edge : edges) {
        	resp.neighbors.add(edge.to);
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(resp);
            byte[] buf = bos.toByteArray();
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(target.hostName), target.port);
            controller.socket.send(p); //ip and port are already in receivePacket
            //For LOG
            System.out.println("Controller sending REGISTER_RESPONSE to id:" + target.id);
            
        } catch (IOException e) {
			System.err.println("Controller sending REGISTER_RESPONSE to id:" + target.id + "failed");
		}
	}
	

}
