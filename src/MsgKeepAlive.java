import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.*;

@SuppressWarnings("serial")
public class MsgKeepAlive implements java.io.Serializable {
	
	int id;
	public MsgKeepAlive(int id) {
		this.id = id;  //sender's id
	}
	
	
	public static MsgKeepAlive recv(Switch sw, DatagramPacket recvPacket) {
		try {
			sw.socket.receive(recvPacket); //receive() is thread safe
		} catch (IOException e1) {
			System.err.println("KEEP_ALIVE Reading Error 001");
		}
		try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			Object obj = in.readObject();
			if(!obj.getClass().getSimpleName().equals("MsgKeepAlive")) {
				return null;
			}else {
				return (MsgKeepAlive)obj;
				
			}
		} catch (ClassNotFoundException e) {
			System.err.println("KEEP_ALIVE Reading Error 002");
		} catch (IOException e1) {
			System.err.println("KEEP_ALIVE Reading Error 003");
		}
		return null;
	}	
	
	
	
	public static void send(Switch sw, Node target){
		if(sw == null || target == null) return;
		MsgKeepAlive msg = new MsgKeepAlive(sw.id);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(msg);
            byte[] buf = bos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName(target.hostName), target.port);
            sw.socket.send(sendPacket); 
        } catch (IOException e) {
        	System.err.println(sw.id + " sending KeepAlive to " + target.id + " failed");
		}
		
		
	}

}
