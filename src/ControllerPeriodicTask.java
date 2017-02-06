import java.util.TimerTask;

public class ControllerPeriodicTask extends TimerTask{
	//For every MK seconds, the controller needs to check if any node haven't sent the topology update
	public Controller controller;
	
	public ControllerPeriodicTask(Controller controller) {
		this.controller = controller;
	}
	
	public void run() {
		int v = controller.numNodes;
		for(int id = 1; id <= v; id ++) {
			Node node = controller.nodeMap.get(id);
			if(node.alive && controller.aliveNodeArr[node.id - 1] == 0) {
				//For LOG
				controller.log.println("Switch ID: " + node.id + "became dead!");
				
				//mark all the edges from and to this node as dead, id - 1 row and id - 1 column all zeros
				synchronized(controller) {
					node.setAlive(false);
					for(int j = 0; j < v; j ++) {
						controller.aliveBwGraph[id - 1][j] = 0;
					}
					for(int i = 0; i < v; i ++) {
						controller.aliveBwGraph[i][id - 1] = 0;
					}

					//trigger route update computation and send to all switches
					RoutingStrategy routingStrategy = new RoutingStrategy(controller);
					//routing table is not a pulic variable in controller because no need to store history in controller
					int[][] routingtable = routingStrategy.computeRouteTable();
					RouteUpdateToAll routeupdatetoall = new RouteUpdateToAll(controller, routingtable);
					routeupdatetoall.sendRouteUpdateToAllNodes();
				}
				controller.log.println("New Route Table Sent.");
			}
		}
		//re-initialize
		for(int i = 0; i < v; i ++) {
			controller.aliveNodeArr[i] = 0;
		}
	}
		

}
