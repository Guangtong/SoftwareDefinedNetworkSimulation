
/**
 * Start Controller proess in the terminal in the following ways
 *  $ java Controller 												  	//read file from ./topo_3nodes.txt 
 *	$ java Controller  <graph file path and name>  						//localhost:30000 will be used
 *	$ java Controller  <graph file path and name> <controller port>  	//localhost will be used
 *	$ java Controller  <graph file path and name> <controller hostname> <controller port>
 */


import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Controller {

	HashMap<Integer, Node> nodeMap;
	HashSet<Node> regSet;
	Semaphore allRegisteredSignal = new Semaphore(0);
	Log log;
	
	int port;
	DatagramSocket socket;
    InetAddress IPAddress;
    
    //Feng added
    int[] aliveNodeArr;  //store node for controller periodic task: judge nodes as dead
    int[][] originalBwGraph; //store the graph read from original file
    int[][] aliveBwGraph; //not binary, used for route re-computation
    int[][] originalDelayGraph;
    int numNodes;
    static int M = 5;
    static int K = 5;
	//Feng added end
	
	Controller(String hostName, int port) throws Exception{
		this.nodeMap = new HashMap<Integer, Node>();
		this.regSet = new HashSet<Node>();
		this.port = port;
		String now = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	    this.log = new Log("LOG_Controller_"+ now +".txt");
	    
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			log.errPrintln("Cannot open port: " + port);
			throw e;
		}
	    try {
			this.IPAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			log.errPrintln("Cannot find host: " + hostName);
			throw e;
		}
	    
	}
	
	public static void main(String[] args) {
		//0:parse args
		String hostname = "localhost";
		int port = 30000;
		String filename = "topo_3nodes.txt";
		
		if(args.length == 0) {
			System.out.println("Read graph from default file: ./" + filename);
		}else if(args.length == 1) {
			filename = args[0];
		}else if (args.length == 2) {
			filename = args[0];
			port = Integer.parseInt(args[1]);
		}else if(args.length == 3) {
			filename = args[0];
			hostname = args[1];
			port = Integer.parseInt(args[2]);
			
		}else {
			System.err.println("Input Parameter Error.");
			return;
		}
		
		
		//1: Read graph file into nodeMap and edge matrix
		
		Controller controller;
		try {
			controller = new Controller(hostname, port);
			controller.readGraph(filename);
		} catch (Exception e1) {
			System.err.println("Controller exists because of initializing error!");
			return;
		}
		controller.printGraph(controller.originalBwGraph);
		
		//2: Begin receive after graph read
		(new ControllerMsgRecvThread(controller)).start();

		//4. controller periodic check begins
		Timer timer = new Timer("Controller Periodic Task");
		long period = (long)K * M * 1000;
        timer.schedule(new ControllerPeriodicTask(controller), period, period);  //public void schedule(TimerTask task, long delayms, long periodms)

		
		//3: wait all REGISTER_REQUEST
		while(true) {
			try {
				controller.allRegisteredSignal.acquire();   //wait all switches to be registered
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			controller.log.println("All Switches Registered");
			//4: compute and send RouteUpdate 
			synchronized (controller) {
				RoutingStrategy routingStrategy = new RoutingStrategy(controller);
				int[][] routingtable = routingStrategy.computeRouteTable();
				RouteUpdateToAll routeupdatetoall = new RouteUpdateToAll(controller, routingtable);
				routeupdatetoall.sendRouteUpdateToAllNodes();
			}
			
		}
		
	}
	
          //read number of switch
	
	

	private void readGraph(String fileName) throws IOException {
		
        String line = null;
        BufferedReader bufferedReader = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            //read number of switch
            line = bufferedReader.readLine();
            int num = Integer.parseInt(line);  //id = 1 to num
            
    		for(int i = 1; i <= num; i++) {
    			Node n = new Node(i);   //NOTE: Node is not alive when created, unless they registered and keep the heartbeat
    			nodeMap.put(i,n);
    		}
    		numNodes = num;
            originalBwGraph = new int[num][num];
            originalDelayGraph = new int[num][num];
            aliveBwGraph = new int[num][num];
            aliveNodeArr = new int[num];
            
            while((line = bufferedReader.readLine()) != null) {
            	String[] splited = line.split("\\s+");
            	int id1 = Integer.parseInt(splited[0]);
            	int id2 = Integer.parseInt(splited[1]);
            	int bw = Integer.parseInt(splited[2]);
            	int delay = Integer.parseInt(splited[3]);
            	originalBwGraph[id1 - 1][id2 - 1] = originalBwGraph[id2 - 1][id1 - 1] = bw;
            	originalDelayGraph[id1 - 1][id2 - 1] = originalDelayGraph[id2 - 1][id1 - 1] = delay;
            }
           
            // Always close files.
            bufferedReader.close();      
            fileReader.close();
        }
        catch(IOException e) {
            log.errPrintln("Error reading file :" + fileName); 
            throw e;
        }     				
	}
	
			
	
	private void printGraph(int[][] graph) {
		log.println("Adjacency Matrix of Input Graph");
		log.print("BW\t");
		for(int i = 1; i <= graph.length; i++) {
			log.print(i+"\t");
		}
		this.log.println();
		for(int i = 0; i < graph.length; i++) {
			log.print(i+1+"\t");
			for(int j = 0; j < graph.length; j++) {
				this.log.print(graph[i][j] + "\t");
			}
			this.log.println();
		}
	}

}
