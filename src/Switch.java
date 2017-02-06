import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Switch {
	
	int id;
	Set<Integer> failedIds;
	Log log;

	DatagramSocket socket;
	int port;
	int controllerPort;
	String controllerHostName;
	InetAddress controllerIPAddress;
	
	HashMap<Integer, Node> neighborMap; 
	//NOTE: the switch's view of neighbor nodes, alive node just means its link to here is active
	
	static int K = 5;      //Send KEEP_ALIVE and TOPOLOGY_UPDATE every K sec, 
	static int M = 3;  	  //mark a neighbor as dead after no response for K*M sec 
	
	Set<Node> aliveNeighborSet;  //store node
	
	public Switch(int swID, String hostName, int controllerPort, Set<Integer> failedIds) throws Exception {
		this.id = swID;
		this.controllerHostName = hostName;
		this.failedIds = failedIds;
		this.controllerPort = controllerPort;
		this.port = this.controllerPort + id;
		this.neighborMap = new HashMap<Integer, Node>();
		this.aliveNeighborSet = new HashSet<Node>();
		String now = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		this.log = new Log("LOG_Switch_ID-"+swID+"_" + now +".txt");
		
		try {
			this.socket = new DatagramSocket(this.port); 
		} catch (SocketException e) {
			log.errPrintln("Cannot open port: " + this.port);
			throw e;
		}
	    try {
			this.controllerIPAddress = InetAddress.getByName(hostName);
			//sw.log.errPrintln(this.controllerIPAddress.getHostAddress());
		} catch (UnknownHostException e) {
			log.errPrintln("Cannot find host: " + hostName);
			throw e;
		}
	}

	public static void main(String[] args) {
		//0: parse args
		
		int swID = 1;
		String hostName = "localhost";
		int controllerPort = 30000;
		
//		if(args.length < 3) {
//			sw.log.errPrintln("Invalid Argument");
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
		Switch sw;
		try {
			sw = new Switch(swID, hostName, controllerPort, failedIds);
		} catch (Exception e) {
			System.err.println("Switch "+ swID + " exists because of error.");
			return;
		}
		
		
		//2: send REGISTER_REQUEST
		MsgRegisterRequest.send(sw);
		
		
		//3: create receive threads to receive REGISTER_UPDATE, KEEP_ALIVE and ROUTE_UPDATE
		//NOTE: only after registered may it possibly receive KEEP_ALIVE or ROUTE_UPDATE 
		(new SwitchMsgRecvThread(sw)).start();

		//4. start the timer task
		Timer timer = new Timer("Switch Periodic Task");
		long period = (long)K * 1000;
        timer.schedule(new SwitchPeriodicTask(sw, M), period, period);  //public void schedule(TimerTask task, long delayms, long periodms)
		
        //main exits
	}


	
	


	public void printNeighbors() {
		//For LOG
		log.println("===== Neighbors of SW ID: " + id + " ====");
		log.println("ID\tHostName\tport\tAlive");
		
		for(Node n : this.neighborMap.values()) {
			log.println(n.id + "\t" + n.hostName + "\t" + n.port +"\t" + n.alive);
		}	
		
	}

	

	

}
