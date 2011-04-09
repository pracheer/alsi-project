package ssm;

import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;

/**
 * This class contains the value (count & message) associated with a given Session.
 * @author prac
 *
 */
public class Value {
	
	private int count;
	private String msg;
	private static String SEPARATOR = ";";
	public static String DEFAULT_MSG = "Hello, User!";
	
	public Value(int count) {
		super();
		this.count = count;
		this.msg = DEFAULT_MSG;
	}
	
	public Value(int count, String msg) {
		super();
		this.count = count;
		this.msg = msg;
	}
	
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public String toString() {
		return count + SEPARATOR + msg + SEPARATOR;
	}
	
	public static Value fromString(String string) {
		String[] strings = string.split(SEPARATOR);
		return new Value(Integer.parseInt(strings[0]), strings[1]);
	}
}
