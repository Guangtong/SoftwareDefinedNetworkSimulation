import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;

public class Switch {
	
	int id;
	List<Integer> failedIds;
	DatagramSocket socket;
	int port;
	int controllerPort;
	String controllerHostName;
	InetAddress controllerIPAddress;
	
	HashMap<Integer, Node> neighborMap; 
	
	public Switch(int swID, String hostName, int controllerPort, List<Integer> failedIds) {
		this.id = swID;
		this.controllerHostName = hostName;
		this.failedIds = failedIds;
		this.controllerPort = controllerPort;
		this.port = this.controllerPort + id;
		this.neighborMap = new HashMap<Integer, Node>();
		
		try {
			this.socket = new DatagramSocket(this.port); 
		} catch (SocketException e) {
			System.out.println("Cannot open port: " + this.port);
		}
	    try {
			this.controllerIPAddress = InetAddress.getByName(hostName);
			//System.out.println(this.controllerIPAddress.getHostAddress());
		} catch (UnknownHostException e) {
			System.out.println("Cannot find host: " + hostName);
		}
	}

	public static void main(String[] args) {
		//0: parse args
		
		int swID = 1;
		String hostName = "localhost";
		int controllerPort = 30000;
		
//		if(args.length < 3) {
//			System.out.println("Invalid Argument");
//			return;
//		}
//		swID = Integer.parseInt(args[0]);
//		hostName = args[1];
//		controllerPort = Integer.parseInt(args[2]);
		
		List<Integer> failedIds = new ArrayList<>(); 
		if(args.length >= 5 && args[3].equals("-f")) {
			for(int i = 4; i < args.length; i++) {
				failedIds.add(Integer.parseInt(args[i]));
			}
		}

		//1: create switch socket
		Switch sw = new Switch(swID, hostName, controllerPort, failedIds);
		
		//2: send REGISTER_REQUEST
		sw.sendRegisterRequest();
		byte[] buffer = new byte[10240];
		DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
		try {
			sw.recvRegisterResponse(recvPacket);
		} catch (IOException e) {
			System.out.println("REGISTER_RESPONSE Reading Error ");
		}
		
		sw.printNeighbors();
		//3. send KEEP_ALIVE to active neighbors
		
	}
	
	private void printNeighbors() {
		//For Test and LOG
		for(Node n : this.neighborMap.values()) {
			System.out.println(n.id + "," + n.hostName + "," + n.port +":");
		}	
		
	}

	private void recvRegisterResponse(DatagramPacket recvPacket) throws IOException {
		RegisterResponse resp = null;
		while(resp == null) {
			this.socket.receive(recvPacket);
			try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
			     ObjectInput in = new ObjectInputStream(bis)) {
				Object obj = in.readObject();
				if(!obj.getClass().getSimpleName().equals("RegisterResponse")) {
					continue;
				}else {
					resp = (RegisterResponse)obj;
				}
			} catch (ClassNotFoundException e) {
				System.out.println("REGISTER_RESPONSE Reading Error ");
			}
		}
		//Read in neighbors
		for(Node n : resp.neighbors) {
			n.noResponseTime = 0;
			this.neighborMap.put(n.id, n);			
		}

	}

	private void sendRegisterRequest() {
		RegisterRequest req = new RegisterRequest(id);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(req);
            byte[] sendBuffer = bos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, controllerIPAddress, controllerPort);
            System.out.println(sendPacket.getSocketAddress());
            this.socket.send(sendPacket); 
        } catch (IOException e) {
			System.out.println(id + " Sending to controller failed");
		}
	}

}
