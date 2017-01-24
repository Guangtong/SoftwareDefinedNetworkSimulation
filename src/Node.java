
public class Node {
	int id;
	int noResponseTime;
	boolean alive;
	int port;
	String hostName;
	Node(int id){
		this.id = id;
		this.noResponseTime = 0;
		this.alive = false;
		this.hostName = "localhost";
		this.port = -1;
	}
	
	public void update(int id, String hostname, int port, boolean alive){
		this.id = id;
		this.hostName = hostname;
		this.port = port;
		this.alive = alive;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getNoResponseTime() {
		return noResponseTime;
	}
	public void setNoResponseTime(int noResponseTime) {
		this.noResponseTime = noResponseTime;
	}
	public boolean isAlive() {
		return alive;
	}
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
}
