import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
	
	public static void send(Switch sw, Node target){
		if(sw == null || target == null) return;
		MsgKeepAlive msg = new MsgKeepAlive(sw.id);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(msg);
            byte[] buf = bos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName(target.hostName), target.port);
            sw.socket.send(sendPacket); 
            //For LOG:
            if(sw.isVerbose()){
            	sw.log.println("Switch-"+sw.id + " sending KEEP_ALIVE to Switch-" + target.id);
            }
            
        } catch (IOException e) {
        	sw.log.errPrintln("Switch-"+sw.id + " sending KEEP_ALIVE to Switch-" + target.id + " failed");
		}

	}

}
