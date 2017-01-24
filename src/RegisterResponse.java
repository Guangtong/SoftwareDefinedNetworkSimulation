import java.util.ArrayList;

public class RegisterResponse implements java.io.Serializable{
	//public String type = "REGISTER_RESPONSE";
	public ArrayList<Node> neighbors;
	public RegisterResponse() {
		neighbors = new ArrayList<Node>();
	}
	
}
