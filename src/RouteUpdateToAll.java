public class RouteUpdateToAll {
	public Controller controller;
	public int[][] routingTable;
	
	
	public RouteUpdateToAll(Controller controller, int[][] routingtable) {
		this.controller = controller;
		this.routingTable = routingtable;
	}
	
	public void sendRouteUpdateToAllNodes() {
		int V = controller.nodeMap.size();
		for(int id = 1; id <= V; id ++) {
			Node node = controller.nodeMap.get(id);
			if(node.alive) {
				int[] column = matrixToColumn(routingTable, id);
				RouteUpdate routeupdate = new RouteUpdate();
				routeupdate.sendRouteUpdateToOneNode(controller, column, node);
			}
		}
	}
	
	public int[] matrixToColumn(int[][] routingtalbe, int id) {
		int V = routingTable.length;
		int[] column = new int[V];
		int columnIndex = id - 1;
		for(int j = 0; j < V; j ++) {
			column[j] = routingTable[j][columnIndex];
		}
		return column;
	}
}