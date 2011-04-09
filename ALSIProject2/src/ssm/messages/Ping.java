package ssm.messages;

public class Ping implements Message{

	public Ping() {
	}

	public static Message fromString(String string) {
		return new Ping();
	}
	
	@Override
	public String toString() {
		return "";
	}
	
}
