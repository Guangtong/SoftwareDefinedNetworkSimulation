

import java.io.*;
import java.net.*;
import java.util.*;

public class Controller {

	HashMap<Integer, Node> nodemap;
	HashMap<Node, ArrayList<Edge>> edgemap;
	
	int port;
	DatagramSocket socket;
    InetAddress IPAddress;
	
	Controller(String hostname, int port){
		this.nodemap = new HashMap<Integer, Node>();
		this.edgemap = new HashMap<Node, ArrayList<Edge>>();
		this.port = port;
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			System.out.println("Cannot open port: " + port);
		}
	    try {
			this.IPAddress = InetAddress.getByName(hostname);
		} catch (UnknownHostException e) {
			System.out.println("Cannot find host: " + hostname);
		}
	}
	
	
	
	public static void main(String[] args) {
		//0:parse args
		String hostname = "localhost";
		int port = 3000;
		String filename = "./src/"+"topo_3nodes.txt";
		
		if(args.length == 3) {
			hostname = args[0];
			port = Integer.parseInt(args[1]);
			filename = args[2];
		}
		
		//1: Read graph file
		Controller controller = new Controller(hostname, port);
		controller.readGraph(filename);	
		controller.printGraph();
		
		//2: wait all REGISTER_REQUEST
		int count = controller.numSwitches();
        byte[] receiveBuffer = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

		while(count > 0) {
            try {
            	//2.1: Read in a packet for REGISTER_REQUEST, update switch Node info
            	receivePacket.setLength(1024); //reset buffer length
            	Node n = controller.registerSwitch(receivePacket);
            	if(n == null) {
            		continue; //Not a valid switch register request
            	}
            	count--;
	            
	            //2.2 send REGISTER_RESPONSE with all its neighbors to the switch
            	controller.respondToSwitch(n, receivePacket);
            	
			} catch (IOException e) {
				
				System.out.println("Receive Error");
			}
		}
	}



	private int numSwitches() {
		return nodemap.size();
	}
	
	private Node registerSwitch(DatagramPacket receivePacket) throws IOException {
		
		this.socket.receive(receivePacket);
		RegisterRequest req = null;
		
		try (ByteArrayInputStream bis = new ByteArrayInputStream(receivePacket.getData(), 0, receivePacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			req = (RegisterRequest)in.readObject();
			if(!req.type.equals("REGISTER_REQUEST")) {
				return null;
			}
		} catch (ClassNotFoundException e) {
			System.out.println("REGISTER_REQUEST Reading Error ");
		}
		
		//Read in switch info
		int swID = req.id;
		InetAddress swIP = receivePacket.getAddress();
        int swPort = receivePacket.getPort();
        
        //update switch Node info
        Node n = this.nodemap.getOrDefault(swID, null);
        if(n != null) {
        	n.update(swID, swIP.getHostName(), swPort, true);
        }
		return n;
	}
	
	private void respondToSwitch(Node n, DatagramPacket receivePacket) {
		RegisterResponse resp = new RegisterResponse();
        ArrayList<Edge> edges = this.edgemap.get(n);
        for(Edge edge : edges) {
        	resp.neighbors.add(edge.to);
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(resp);
            byte[] sendBuffer = bos.toByteArray();
            receivePacket.setData(sendBuffer);
            this.socket.send(receivePacket); //ip and port are just received
        } catch (IOException e) {
			System.out.println("Sending to id:" + n.id + "failed");
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
    			Node n = new Node(i);
    			nodemap.put(i,n);
    			ArrayList<Edge> neighbors = new ArrayList<>();
    			edgemap.put(n, neighbors);
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
        		n1 = nodemap.get(id1);
        		n2 = nodemap.get(id2);
        		e1 = new Edge(n1,n2,bw,delay);
        		e2 = new Edge(n2,n1,bw,delay);
        		l1 = edgemap.get(n1);
        		l2 = edgemap.get(n2);
        		l1.add(e1);
        		l2.add(e2);
            }   

            // Always close files.
            bufferedReader.close();      
            fileReader.close();
            
            
        }
        catch(IOException ex) {
            System.out.println("Error reading file '" + fileName + "'");      
        }
		
           				
	}
	
	public void printGraph() {
		//For Test and LOG
		for(HashMap.Entry<Node, ArrayList<Edge>> ent : this.edgemap.entrySet()) {
			Node n = ent.getKey();
			System.out.println(n.id + "," + n.hostName + "," + n.port +":");
			ArrayList<Edge> l = ent.getValue();
			for(Edge e : l) {
				System.out.println("    " + e.from.id + " - " + e.to.id + " BW:" + e.bw + " DLY:" + e.delay + " ACT:" + e.active);
			}
			
		}		
	}

}
