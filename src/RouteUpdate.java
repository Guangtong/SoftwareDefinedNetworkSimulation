import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

@SuppressWarnings("serial")
public class RouteUpdate implements java.io.Serializable{

//	public Controller controller;
	public int[] nextHopToDestination;
//	public Node node;
	public RouteUpdate() {
//		this.controller = controller;
		this.nextHopToDestination = new int[3];
//		this.node = node;
	}
	
	
	public void sendRouteUpdateToOneNode(Controller controller, int[] nexthoptodes, Node node) {
		RouteUpdate rupd = new RouteUpdate();
		rupd.nextHopToDestination = nexthoptodes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(rupd);
            byte[] buf = bos.toByteArray();
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.hostName), node.port);
            controller.socket.send(p); //ip and port are already in receivePacket
        } catch (IOException e) {
			controller.log.errPrintln("Controller sending ROUTE_UPDATE to id:" + node.id + "failed");
		}
	}
}
