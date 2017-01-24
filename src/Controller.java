

import java.io.*;
import java.net.*;
import java.util.*;

public class Controller {

	HashMap<Integer, Node> nodeMap;
	HashMap<Node, ArrayList<Edge>> edgeMap;
	
	int port;
	DatagramSocket socket;
    InetAddress IPAddress;
	
	Controller(String hostName, int port){
		this.nodeMap = new HashMap<Integer, Node>();
		this.edgeMap = new HashMap<Node, ArrayList<Edge>>();
		this.port = port;
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			System.out.println("Cannot open port: " + port);
		}
	    try {
			this.IPAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			System.out.println("Cannot find host: " + hostName);
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
		
		//2: wait all REGISTER_REQUEST
		int count = controller.numSwitches();
        byte[] buffer = new byte[1024];
		DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
		

		while(count > 0) {
            try {
            	//2.1: Read in a packet for REGISTER_REQUEST, update switch Node info
            	recvPacket.setData(buffer); //reset buffer
            	Node n = controller.recvRegisterRequest(recvPacket);
            	
            	if(n == null) {
            		continue; //Not a valid switch register request
            	}
            	
            	count--;
	            
	            //2.2 send REGISTER_RESPONSE with all its neighbors to the switch
            	controller.sendRegisterResponse(n, recvPacket);
            	
			} catch (IOException e) {
				
				System.out.println("Receive Error");
			}
		}
		
		System.out.println("All Switches Registered");
	}



	private int numSwitches() {
		return nodeMap.size();
	}
	
	private Node recvRegisterRequest(DatagramPacket recvPacket) throws IOException {
		
		this.socket.receive(recvPacket);
		
		RegisterRequest req = null;
		
		try (ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData(), 0, recvPacket.getLength());
		     ObjectInput in = new ObjectInputStream(bis)) {
			
			Object obj = in.readObject();
			
			if(!obj.getClass().getSimpleName().equals("RegisterRequest")) {
				return null;
			}else {
				req = (RegisterRequest)obj;
			}
		} catch (ClassNotFoundException e) {
			System.out.println("REGISTER_REQUEST Reading Error ");
		}
		
		//Read in switch info
		int swID = req.id;
		InetAddress swIP = recvPacket.getAddress();
        int swPort = recvPacket.getPort();
        
        //update switch Node info
        Node n = this.nodeMap.getOrDefault(swID, null);
        if(n != null) {
        	n.update(swID, swIP.getHostName(), swPort, true);
        }
		return n;
	}
	
	private void sendRegisterResponse(Node n, DatagramPacket receivePacket) {
		RegisterResponse resp = new RegisterResponse();
        ArrayList<Edge> edges = this.edgeMap.get(n);
       
        for(Edge edge : edges) {
        	resp.neighbors.add(edge.to);
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(resp);
            byte[] sendBuffer = bos.toByteArray();
            receivePacket.setData(sendBuffer);
            this.socket.send(receivePacket); //ip and port are already in receivePacket
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
            System.out.println("Error reading file '" + fileName + "'");      
        }
		
           				
	}
	
	public void printGraph() {
		//For Test and LOG
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
