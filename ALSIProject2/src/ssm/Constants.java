package ssm;

public class Constants {
	public static int WQ = 2;
	public static int W = 2;
	public static final int DATAGRAM_SIZE = 1000;
	public static final int TIMEOUT = 240000; // in milliseconds
	
	public static int R = 1;
	public static int roundTime = 1000;
	public static final long SESSION_VALIDITY = 60*1000; // 1 minute
	
	public static String toHTMLString() {
		return "WQ="+WQ + " servers.<br/>W=" + W + " servers." + 
		"<br/>TIMEOUT=" + TIMEOUT + " ms." +  
		"<br/>roundTime="+roundTime+" ms." +
		"<br/>SESSION_VALIDITY="+SESSION_VALIDITY+" ms.";
	}

}
