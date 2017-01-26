import java.net.*;
import java.util.*;

public class Switch {
	
	int id;
	Set<Integer> failedIds;

	DatagramSocket socket;
	int port;
	int controllerPort;
	String controllerHostName;
	InetAddress controllerIPAddress;
	
	HashMap<Integer, Node> neighborMap; 
	//NOTE: the switch's view of neighbor nodes, alive node just means its link to here is active
	
	
	Set<Node> aliveNeighborSet;  //store node
	
	public Switch(int swID, String hostName, int controllerPort, Set<Integer> failedIds) {
		this.id = swID;
		this.controllerHostName = hostName;
		this.failedIds = failedIds;
		this.controllerPort = controllerPort;
		this.port = this.controllerPort + id;
		this.neighborMap = new HashMap<Integer, Node>();
		this.aliveNeighborSet = new HashSet<Node>();
		
		try {
			this.socket = new DatagramSocket(this.port); 
		} catch (SocketException e) {
			System.err.println("Cannot open port: " + this.port);
		}
	    try {
			this.controllerIPAddress = InetAddress.getByName(hostName);
			//System.err.println(this.controllerIPAddress.getHostAddress());
		} catch (UnknownHostException e) {
			System.err.println("Cannot find host: " + hostName);
		}
	}

	public static void main(String[] args) {
		//0: parse args
		
		int swID = 1;
		String hostName = "localhost";
		int controllerPort = 30000;
		
//		if(args.length < 3) {
//			System.err.println("Invalid Argument");
//			return;
//		}
		swID = Integer.parseInt(args[0]);
//		hostName = args[1];
//		controllerPort = Integer.parseInt(args[2]);
		
		Set<Integer> failedIds = new HashSet<>(); 
		if(args.length >= 5 && args[3].equals("-f")) {
			for(int i = 4; i < args.length; i++) {
				failedIds.add(Integer.parseInt(args[i]));
			}
		}

		//1: create switch socket
		Switch sw = new Switch(swID, hostName, controllerPort, failedIds);

		//2: send REGISTER_REQUEST
		MsgRegisterRequest.send(sw);
		
		
		//3: create receive threads to receive  "KEEP_ALIVE" and ROUTE_UPDATE
		//NOTE: only after registered may it possibly receive "KEEP_ALIVE" or ROUTE_UPDATE 
		(new SwitchMsgRecvThread(sw)).start();
//		(new SwitchRouteUpdateThread(sw)).start();
		
		
		//4: recv REGISTER_RESPONSE
		
		
		
		//5. send KEEP_ALIVE to alive neighbors

		for(Node n : sw.neighborMap.values()) {
			if(n.alive && !failedIds.contains(n)) {
				MsgKeepAlive.send(sw, n);
			}
		}
		//6. start the timer task
		
		
	}


	
	


	public void printNeighbors() {
		//For LOG
		for(Node n : this.neighborMap.values()) {
			System.out.println(n.id + "," + n.hostName + "," + n.port +":");
		}	
		
	}

	

	

}
