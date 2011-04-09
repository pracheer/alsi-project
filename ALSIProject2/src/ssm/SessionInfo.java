package ssm;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Contains Session Information.
 * It increments version number whenever a change is made.
 * For Creating Session Id, a combination of Servers Ip Address and Current timestamp is used.
 * By Default server keeps a particular session information for SESSION_VALIDITY ms. (kept at 1 minute).
 */

public class SessionInfo {
	
	private String sessionId;
	private int version;
	private long timestamp;
	private Value value;
	
	
	private void incrementVersion() {
		version = version + 1;
	}

	public void setValue(Value value) {
		this.value = value;
		incrementVersion();
		timestamp = System.currentTimeMillis() + Constants.SESSION_VALIDITY; 
	}
	
	public static SessionInfo create(Value value) {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			String sessionId = addr.getHostAddress() + System.nanoTime();
			int version = 1;
			return new SessionInfo(sessionId, version, value);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static SessionInfo create(Value value, int version) {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			String sessionId = addr.getHostAddress() + System.nanoTime();
			return new SessionInfo(sessionId, version, value);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public SessionInfo(String sessionId, int version, Value value) {
		this.sessionId = sessionId;
		this.version = version;
		this.value = value;
		timestamp = System.currentTimeMillis() + Constants.SESSION_VALIDITY; 
	}

	public String getSessionId() {
		return sessionId;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public Value getValue() {
		return value;
	}
}
