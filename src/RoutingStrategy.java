


public class RoutingStrategy {
	Controller controller;
	int numNodes;
	int[][] aliveBwGraph;
	int width[];
	boolean sptSet[];
	int pred[];
	int routingTable[][];
    
    
	
	public RoutingStrategy(Controller controller) {
		this.controller = controller;
		this.aliveBwGraph = controller.aliveBwGraph;
		numNodes = controller.nodeMap.size();
		width = new int[numNodes];
	    sptSet = new boolean[numNodes];
	    pred = new int[numNodes];
	    routingTable = new int[numNodes][numNodes];//every row index is the source, and the elements in that row 
	    //marks the next hop to go is the final destination is the row source.
		
	}

	public int[][] computeRouteTable() {
		
//		controller.log.println("The alive_graph is:");
//		printAliveGraph(aliveBwGraph);
		for(int src = 0; src < numNodes; src ++) {
			widestCapacity(src);
		}
		controller.log.println("The routing table for every node is as below:");
		printSolution(routingTable);
		return routingTable;
	}
		
	public int selectFinish() {
	    
	    int max = Integer.MIN_VALUE;
	    int max_index = -1;
	    
	    for (int v = 0; v < numNodes; v++) {
	    	if (sptSet[v] == false && width[v] >= max) {
	        	max = width[v];
	    		max_index = v;
	        }
	    }   
	    return max_index;
	}
	
	public void widestCapacity(int src)//int graph[][], int src
	{
	    for (int i = 0; i < numNodes; i++) {
	    	width[i] = 0;
		    sptSet[i] = false;
		    pred[i] = -2;//src's pred is no one, detached nodes also have no pred. Have to distinguish
	    }
	       
	    width[src] = Integer.MAX_VALUE;
	    
	    
	    for (int count = 0; count < numNodes; count++)
	    {
	        int u = selectFinish();//(width, sptSet)
	        sptSet[u] = true;
	        for (int v = 0; v < numNodes; v++) {
	        	if (sptSet[v] == false && aliveBwGraph[u][v] != 0 && Math.min(width[u],aliveBwGraph[u][v]) > width[v]) {
	                width[v] = Math.min(width[u],aliveBwGraph[u][v]);
	                pred[v] = u;
	            }
	        }
	    }
	    pred[src] = -1; // Detached nodes' pred is -2, while src's pred is -1
	    for(int j = 0; j < numNodes; j ++) {
	    	routingTable[src][j] = pred[j] + 1;//plus one to be id instead of matrix index
	    }
	}

	public void printSolution(int[][] routingtable)//int width[], int pred[], int n
	{
		controller.log.println("===Up-to-date Routing Table for all Switches===");
		for(int j = 0; j < routingtable.length; j ++) {
			controller.log.println("Routing Table for Switch-" + (j + 1));
			controller.log.print("Destination Switch:     \t");
			for(int i = 0; i < routingtable.length; i ++) {
				controller.log.print((i + 1) + "\t");
			}
			controller.log.println();
			controller.log.print("Next hop:               \t");
			for(int des = 0; des < routingtable.length; des ++) {
				controller.log.print(routingtable[des][j] + "\t");
			}
			controller.log.println();
			controller.log.println();
		}
	}
	public void printAliveGraph(int[][] aliveGraph) {
		for(int i = 0; i < aliveGraph.length; i ++) {
			for (int j = 0; j < aliveGraph.length; j++) {
				controller.log.print(aliveGraph[i][j] + ", ");
		    }
			controller.log.println();
		}     
	}
	
		
		
		
	
}
