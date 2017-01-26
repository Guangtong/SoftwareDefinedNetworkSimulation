import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;

@SuppressWarnings("serial")
public class MsgRegisterRequest implements java.io.Serializable {
	
	int id;
	public MsgRegisterRequest(int id) {
		this.id = id;
	}
	public static void send(Switch sw) {
		MsgRegisterRequest req = new MsgRegisterRequest(sw.id);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(req);
            byte[] sendBuffer = bos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, sw.controllerIPAddress, sw.controllerPort);
            
            //System.out.println(sendPacket.getSocketAddress());
            sw.socket.send(sendPacket); 
        } catch (IOException e) {
			System.err.println(sw.id + " sending REGISTER_REQUEST to controller failed");
		}
		
	}

}
