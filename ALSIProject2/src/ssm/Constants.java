package ssm;

public class Constants {
	public static int WQ = 3;
	public static int W = 5;
	public static final int DATAGRAM_SIZE = 1000;
	public static final int TIMEOUT = 9000; // in milliseconds
	
	public static int R = 1;
	public static int roundTime = 3000;
	public static final long SESSION_VALIDITY = 600*1000; // 1 minute
	
	public static String toHTMLString() {
		return "WQ="+WQ + " servers.<br/>W=" + W + " servers." + 
		"<br/>TIMEOUT=" + TIMEOUT/1000.0 + " seconds." +  
		"<br/>gossip time interval(roundTime)="+roundTime/1000.0+" seconds" +
		"<br/>SESSION_VALIDITY="+SESSION_VALIDITY/1000.0+" seconds.";
	}

}
