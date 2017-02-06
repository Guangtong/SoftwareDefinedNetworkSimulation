import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        
        for(int id = 1; id <= controller.numNodes; id ++) {
        	if(controller.originalBwGraph[target.id - 1][id - 1] != 0) {
        		resp.neighbors.add(controller.nodeMap.get(id));
        	}
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(resp);
            byte[] buf = bos.toByteArray();
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(target.hostName), target.port);
            controller.socket.send(p); //ip and port are already in receivePacket
            //For LOG
            resp.printRegisterResponse(controller, target.id);
            
        } catch (IOException e) {
			controller.log.errPrintln("Controller Sending REGISTER_RESPONSE To Switch-" + target.id + "failed");
		}
	}
	
	public void printRegisterResponse(Controller controller, int swID) {
		//For LOG
		controller.log.println("Controller Sending REGISTER_RESPONSE To Switch-" + swID);
        controller.log.println("===== Neighbors of Switch-" + swID + " ====");
		controller.log.println("ID\tHostName\tPort\tAlive");
        for(Node n : this.neighbors) {
			controller.log.println(n.id + "\t" + n.hostName + "\t" + n.port +"\t" + n.alive);
		}
	}


}
