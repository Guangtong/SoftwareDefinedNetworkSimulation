
public class Edge {
	Node from;
	Node to;
	int bw;
	int delay;
	boolean active;
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	Edge(Node from, Node to, int bw, int delay) {
		this.from = from;
		this.to= to;
		this.bw = bw;
		this.delay = delay;
		this.active = true;
	}
}
