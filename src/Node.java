
@SuppressWarnings("serial")
public class Node implements java.io.Serializable {
	int id;
	int noResponseTime;
	boolean alive;
	int port;
	String hostName;
	Node(int id){
		this.id = id;
		this.noResponseTime = 0;
		this.alive = false;
		this.hostName = "undefined";
		this.port = -1;
	}
	
	public synchronized void update(int id, String hostname, int port, boolean alive){
		this.id = id;
		this.hostName = hostname;
		this.port = port;
		this.alive = alive;
		this.noResponseTime = 0;
	}
	
	public synchronized void setId(int id) {
		this.id = id;
	}

	public synchronized void setNoResponseTime(int noResponseTime) {
		this.noResponseTime = noResponseTime;
	}

	public synchronized void incNoResponseTime() {
		this.noResponseTime++;
	}
	
	public synchronized void setAlive(boolean alive) {
		this.alive = alive;
	}

	
}
