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
		
		
		//3: create receive threads to receive REGISTER_UPDATE, KEEP_ALIVE and ROUTE_UPDATE
		//NOTE: only after registered may it possibly receive KEEP_ALIVE or ROUTE_UPDATE 
		(new SwitchMsgRecvThread(sw)).start();

		//4. start the timer task
		Timer timer = new Timer("Switch Repeated Task");
		long K = 10;      //Send KEEP_ALIVE every K sec, 
		int M = 3;  	  //mark a neighbor as dead after no response for K*M sec 
        timer.schedule(new SwitchPeriodicTask(sw, M), K * 1000 , K * 1000);  //public void schedule(TimerTask task, long delayms, long periodms)
		
        //main exits
	}


	
	


	public void printNeighbors() {
		//For LOG
		System.out.println("===== Neighbors of SW ID: " + id + " ====");
		System.out.println("ID\tHostName\tport\tAlive");
		
		for(Node n : this.neighborMap.values()) {
			System.out.println(n.id + "\t" + n.hostName + "\t" + n.port +"\t" + n.alive);
		}	
		
	}

	

	

}
