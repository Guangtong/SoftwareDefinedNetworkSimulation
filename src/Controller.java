

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
    static int M = 3;
    static int K = 5;
	//Feng added end
	
	Controller(String hostName, int port) throws Exception{
		this.nodeMap = new HashMap<Integer, Node>();
		this.regSet = new HashSet<Node>();
		this.port = port;
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
	    String now = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	    this.log = new Log("LOG_Controller_"+ now +".txt");
	}
	
	
	
	public static void main(String[] args) {
		//0:parse args
		String hostname = "localhost";
		int port = 30000;
		String filename = "topo_3nodes.txt";
		
		if(args.length == 3) {
			hostname = args[0];
			port = Integer.parseInt(args[1]);
			filename = args[2];
		}
		
		//1: Read graph file, after this, originalBwGraph and originalDelayGraph already loaded
		Controller controller;
		try {
			controller = new Controller(hostname, port);
		} catch (Exception e1) {
			System.err.println("Controller exists because of error.");
			return;
		}
		controller.readNodeMap(filename);	
		//controller.printGraph();
		
		//Feng added
		//After reading the file, can initialize V, alive_graph, original_graph(original_graph will stay unchanged forever)
		controller.numNodes = controller.getNodeNum(controller.nodeMap);
		controller.originalBwGraph = new int[controller.numNodes][controller.numNodes];
		controller.originalDelayGraph = new int[controller.numNodes][controller.numNodes];
		controller.aliveBwGraph = new int[controller.numNodes][controller.numNodes];
		controller.aliveBwGraph = controller.initializeAliveGraph(controller.numNodes);
		controller.readEgdes(filename);	
		controller.aliveNodeArr = controller.initializeAliveNodeArr(controller.numNodes);
		//Feng added end
		
		
		controller.printGraphTest(controller.originalBwGraph);
		
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
			
			//For LOG
			controller.log.println("All Switches Registered");
			//break;
			//4: compute and send RouteUpdate 
			//Feng added
			synchronized (controller) {
				RoutingStrategy routingStrategy = new RoutingStrategy(controller);
				int[][] routingtable = routingStrategy.computeRouteTable();
				RouteUpdateToAll routeupdatetoall = new RouteUpdateToAll(controller, routingtable);
				routeupdatetoall.sendRouteUpdateToAllNodes();
			}
			
		}
		
	}
	
          //read number of switch
	
	

	private void readNodeMap(String fileName) {
		
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
            // Always close files.
            bufferedReader.close();      
            fileReader.close();
        }
        catch(IOException ex) {
            log.errPrintln("Error reading file '" + fileName + "'" + "in function readNodeMap");      
        }     				
	}
	
	private void readEgdes(String fileName) {
		String line = null;
        BufferedReader bufferedReader = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
          //read number of switch
            line = bufferedReader.readLine();
          //read edges
            while((line = bufferedReader.readLine()) != null) {
            	String[] splited = line.split("\\s+");
            	int id1 = Integer.parseInt(splited[0]);
            	int id2 = Integer.parseInt(splited[1]);
            	int bw = Integer.parseInt(splited[2]);
            	int delay = Integer.parseInt(splited[3]);
            	//Feng added
            	originalBwGraph[id1 - 1][id2 - 1] = originalBwGraph[id2 - 1][id1 - 1] = bw;
            	originalDelayGraph[id1 - 1][id2 - 1] = originalDelayGraph[id2 - 1][id1 - 1] = delay;
            	//Feng added end
            }
            	// Always close files.
                bufferedReader.close();      
                fileReader.close();   
        }
        catch(IOException ex) {
            log.errPrintln("Error reading file '" + fileName + "'" + "in function readEdges");      
        } 
	}
	
			
	
	private int getNodeNum(HashMap<Integer, Node> nodeMap) {
		int V = nodeMap.size();
		return V;
	}
	
	
	
	private int[][] initializeAliveGraph(int V) {
		int[][] alive_graph = new int[V][V];
		for(int i = 0; i < V; i ++) {
			for(int j = 0; j < V; j ++) {
				alive_graph[i][j] = 0;
			}
		}
		return alive_graph;
	}
	
	private int[] initializeAliveNodeArr(int V) {
		int[] aliveNodeArr = new int[V];
		for(int i = 0; i < V; i ++) {
			aliveNodeArr[i] = 0;
		}
		return aliveNodeArr;
	}
	
	private void printGraphTest(int[][] graph) {
		for(int i = 0; i < graph.length; i ++) {
			for(int j = 0; j < graph.length; j ++) {
				this.log.print(graph[i][j] + "  ");
			}
			this.log.println();
		}
	}

}
