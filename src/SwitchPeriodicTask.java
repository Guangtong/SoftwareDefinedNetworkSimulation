import java.util.TimerTask;



public class SwitchPeriodicTask extends TimerTask {

	private Switch sw;
	private int timeout; //the maximun time not to receive a KEEP_ALIVE before set to non-alive

	public SwitchPeriodicTask(Switch sw, int m) {
		this.sw = sw;
		this.timeout = m;
	}
	

	@Override
	public void run() {
		//check the received KEEP_ALIVE from neighbors: 
		// - reset time count for alive neighbors, 
		// - increase time count for nonresponding neighbors, 
		// - change neighbor alive status when timeout
		boolean needUpdate = false;
		for(Node n : sw.neighborMap.values()) {
			if(!n.alive)
				continue; //Dead sw becoming alive is dealt with in SwitchMsdRecvThread
			
			if(sw.aliveNeighborSet.contains(n)) {
				n.setNoResponseTime(0);
				sw.aliveNeighborSet.remove(n);
			}else{
				n.incNoResponseTime();
				if(n.noResponseTime >= timeout) {
					n.setAlive(false);
					needUpdate = true;
				}
			}

			//send KEEP_ALIVE
			MsgKeepAlive.send(sw, n);
		}
		
		//send TopologyUpdate
		MsgTopologyUpdate.send(sw, needUpdate);

		
	}
	
}
