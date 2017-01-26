

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Controller {

	HashMap<Integer, Node> nodeMap;
	HashMap<Node, ArrayList<Edge>> edgeMap;
	HashSet<Node> regSet;
	Semaphore allRegisteredSignal = new Semaphore(0);
	
	int port;
	DatagramSocket socket;
    InetAddress IPAddress;
	
	Controller(String hostName, int port){
		this.nodeMap = new HashMap<Integer, Node>();
		this.edgeMap = new HashMap<Node, ArrayList<Edge>>();
		this.regSet = new HashSet<Node>();
		this.port = port;
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			System.err.println("Cannot open port: " + port);
		}
	    try {
			this.IPAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			System.err.println("Cannot find host: " + hostName);
		}
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
		
		//1: Read graph file
		Controller controller = new Controller(hostname, port);
		controller.readGraph(filename);	
		controller.printGraph();
		
		//2: Begin receive after graph read
		(new ControllerMsgRecvThread(controller)).start();
		
		//3: wait all REGISTER_REQUEST
		while(true) {
			try {
				controller.allRegisteredSignal.acquire();   //wait all switches to be registered
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//For LOG
			System.out.println("All Switches Registered");
			
			
			//4: compute and send RouteUpdate 
			
			
		}
	}

	private void readGraph(String fileName) {
		
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
    			ArrayList<Edge> neighbors = new ArrayList<>();
    			edgeMap.put(n, neighbors);
    		}
            
            //read edges
            while((line = bufferedReader.readLine()) != null) {
            	String[] splited = line.split("\\s+");
            	int id1 = Integer.parseInt(splited[0]);
            	int id2 = Integer.parseInt(splited[1]);
            	int bw = Integer.parseInt(splited[2]);
            	int delay = Integer.parseInt(splited[2]);
            	
            	Node n1,n2;
        		Edge e1,e2;
        		ArrayList<Edge> l1,l2;
        		n1 = nodeMap.get(id1);
        		n2 = nodeMap.get(id2);
        		e1 = new Edge(n1,n2,bw,delay);
        		e2 = new Edge(n2,n1,bw,delay);
        		l1 = edgeMap.get(n1);
        		l2 = edgeMap.get(n2);
        		l1.add(e1);
        		l2.add(e2);
            }   

            // Always close files.
            bufferedReader.close();      
            fileReader.close();
            
            
        }
        catch(IOException ex) {
            System.err.println("Error reading file '" + fileName + "'");      
        }
		
           				
	}
	
	public void printGraph() {
		//For LOG
		for(HashMap.Entry<Node, ArrayList<Edge>> ent : this.edgeMap.entrySet()) {
			Node n = ent.getKey();
			System.out.println(n.id + "," + n.hostName + "," + n.port +":");
			ArrayList<Edge> l = ent.getValue();
			for(Edge e : l) {
				System.out.println("    " + e.from.id + " - " + e.to.id + " BW:" + e.bw + " DLY:" + e.delay + " ACT:" + e.active);
			}
			
		}		
	}

}
