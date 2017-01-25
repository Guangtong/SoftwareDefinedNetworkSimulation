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
	
	public static MsgRegisterRequest recv(Controller controller, DatagramPacket recvPacket) {
		
		try {
			controller.socket.receive(recvPacket);
		} catch (IOException e1) {
			System.err.println("REGISTER_REQUEST Reading Error 001");
		}

		try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			
			Object obj = in.readObject();
			
			if(!obj.getClass().getSimpleName().equals("MsgRegisterRequest")) {
				return null;
			}else {
				return (MsgRegisterRequest)obj;
			}
		} catch (ClassNotFoundException e) {
			System.err.println("REGISTER_REQUEST Reading Error 002");
		} catch (IOException e1) {
			System.err.println("REGISTER_REQUEST Reading Error 003");
		}
		return null;
		
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
