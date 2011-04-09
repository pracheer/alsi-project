package ssm.messages;

import ssm.Value;

public class GeneralMsg implements Message{

	private String sessionId;
	private int version;
	private boolean valuePresent;
	private Value value;

	public GeneralMsg(String sessionId, int version) {
		this.sessionId = sessionId;
		this.version = version;
		this.valuePresent = false;
	}
	
	public GeneralMsg(String sessionId, int version, Value value) {
		this.sessionId = sessionId;
		this.version = version;
		this.valuePresent = true;
		this.value = value;
	}

	public static GeneralMsg fromString(String string) {
		String[] strings = string.split(SEPARATOR);
		boolean valuePresent = Boolean.parseBoolean(strings[2]);
		if(valuePresent)
			return new GeneralMsg(strings[0], Integer.parseInt(strings[1]), Value.fromString(strings[3]));
		else
			return new GeneralMsg(strings[0], Integer.parseInt(strings[1]));
	}

	@Override
	public String toString() {
		return sessionId + SEPARATOR + version + SEPARATOR + valuePresent + SEPARATOR + value;
	}

	public String getSessionId() {
		return sessionId;
	}

	public int getVersion() {
		return version;
	}

	public boolean isValuePresent() {
		return valuePresent;
	}
	
	public Value getValue() {
		return value;
	}
	
	public void setValue(Value value) {
		this.valuePresent = true;
		this.value = value;
	}
	
	public void removeValue() {
		if(valuePresent)
			valuePresent = false;
	}
}
