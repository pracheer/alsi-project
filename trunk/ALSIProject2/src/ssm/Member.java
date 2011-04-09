package ssm;


/**
 * @author prac, @bby
 *
 */

public class Member {

	private String ip;
	private int port;

	public Member(String hostName, int port) {
		super();
		this.ip = hostName;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public boolean isEqualTo(Member m)
	{
		return (m.getIp().equals(ip) &&
				(m.getPort()==port));
	}
}