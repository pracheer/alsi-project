package ssm;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SSM
 * Implements doGet.
 * Map of all sessions created in init (when server loads the servlet).
 * This is synchronized at SessionObject level and hence multiple users with different session ids can work concurrently.
 * 
 */
public class SSM extends HttpServlet {
	private static final String SEPARATOR = "_";

	private static final String COOKIE_NAME = "CS5300SESSION11";

	private static final long serialVersionUID = 1L;

	public static final String title = "Session Management";

	HashMap<String, SessionInfo> sessionMap;

	private SSMStub ssmStub;

	private Members members;

	private Member me;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SSM() {
		super();
	}

	@Override
	public void init() throws ServletException {
		super.init();

		sessionMap = new HashMap<String, SessionInfo>();
		BrickServer rpcServer = new  BrickServer(sessionMap);
		me = new Member(rpcServer.getIP(), rpcServer.getPort());

		Thread server = new Thread(rpcServer);
		server.start();

		ssmStub = new SSMStub();

		members = new Members();
		GMClient gmClient = new GMClient(members, me);
		Thread gmThread = new Thread(gmClient);
		gmThread.start();
		System.out.println(me + " starting.");
		//		SimpleDBInterface.getInstance().cleanAll();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			PrintWriter out = response.getWriter();
			response.setContentType("text/html");

			int count = -1;
			Cookie[] cookies = request.getCookies();
			SessionInfo sessionInfo = null;

			// First access
			if(cookies == null || cookies.length == 0 ) {
				System.out.println("cookies not found. Creating a new session.");
				count = 1;
				sessionInfo = SessionInfo.create(new Value(count));
			}
			else {
				// repeated access.
				for (Cookie cookie : cookies) {
					if(cookie.getName().equals(COOKIE_NAME)) {
						String val = URLDecoder.decode(cookie.getValue());
						String[] split = val.split(SEPARATOR, 3);
						if(split.length < 3) {
							out.write(handleError("Cookie not constructed properly"));
							return;
						}

						String sessionId = split[0];
						int version = Integer.parseInt(split[1]);
						String locationString = split[2];
						System.out.println("Cookie found with value:"+val);

						// consists of ipaddress and port pair.
						Members locationMembers = Members.fromString(locationString);

						if(request.getParameter("logout")!=null) {
							ssmStub.remove(sessionId, version, locationMembers);
							out.write(createHTML("Bye!"));
							return;
						}

						// TODO: check in local cache if data present.
						Value value = ssmStub.get(me, sessionId, version, locationMembers);
						version++;

						if(value == null) {
							value = new Value(1);
						}
						else {
							value.setCount(value.getCount()+1);
							if(request.getParameter("replace")!= null) {
								String message = request.getParameter("message");
								value.setMsg(message);
							}
						}

						sessionInfo = new SessionInfo(sessionId, version, value);
					}
				}
			}

			if(sessionInfo == null) {
				count = 1;
				sessionInfo = SessionInfo.create(new Value(count));
			}

			SessionInfo oldSession = sessionMap.get(sessionInfo.getSessionId());
			if(oldSession!=null) {
				synchronized (oldSession) {
					sessionMap.put(sessionInfo.getSessionId(), sessionInfo);
				}
			} else {
				sessionMap.put(sessionInfo.getSessionId(), sessionInfo);
			}

			Value value = sessionInfo.getValue();

			Members wMembers;
			// write to W members.
			synchronized (members) {
				/*				if(members.size()<=2) {
					Constants.W = 1(int) Math.ceil(members.size()/2.0);
					Constants.WQ = 1(int) Math.max(Constants.W - 1, 1);
				}
				else {
					Constants.W = 2(int) Math.ceil(members.size()/2.0);
					Constants.WQ = 2(int) Math.max(Constants.W - 1, 1);
				}
				 */				
				DataStorage ds = SimpleDBInterface.getInstance().getDataStoragePattern();
				Constants.W = ds.GetNumberOfServersToSendWriteRequest();
				Constants.WQ = ds.GetNumberOfServersToWaitAfterWriteRequest();
				Constants.R = ds.GetNumberOfServersToSendReadRequest();
				if(members.size() < Constants.WQ) {
					out.write(handleError("Not enough servers running to service the request<br/>WQ="+Constants.WQ));
					return;
				}

				Members newMembers = new Members();
				for (Member m : members.getMembers()) {
					if(m.isEqualTo(me))
						continue;
					newMembers.add(m);
				}
				System.err.println("Changed Constants.W to "+ Constants.W + " and Constants.WQ to " + Constants.WQ);
				wMembers = ssmStub.put(me, sessionInfo.getSessionId(), sessionInfo.getVersion(), newMembers, 
						Constants.W-1, Constants.WQ-1, value);
			}
			wMembers.add(me);
			String cookieVal = URLEncoder.encode(sessionInfo.getSessionId()
					+ SEPARATOR + sessionInfo.getVersion()
					+ SEPARATOR + wMembers.toString());

			if(cookieVal.length() > 1024) {
				out.write(handleError("Cookie size exceeded."));
				return;
			}
			Cookie newCookie = new Cookie(COOKIE_NAME,URLEncoder.encode(cookieVal)); 
			newCookie.setMaxAge(60*60);// 1hour.
			response.addCookie(newCookie);
			String msg = "(" + value.getCount() + ")" + " " + value.getMsg() + ".";
			String smallMsg = "This request processed by " + me.getIp() + ":"
			+ me.getPort() + "<br/>" +  "Session would expire at:" + sessionInfo.getTimeOut() + " GMT."
			+ "<br/>" + "<br/>" + "<br/>" + Constants.toHTMLString() + "<br/>Membercount:"+Math.max(members.size(),1);

			smallMsg = getSmallHTMLStr(smallMsg);

			String[] otherMsgs = {smallMsg};

			out.write(assign3HTML(msg, members.toString(), otherMsgs));
			return;
		}
		finally {
			// Code to Clean up session that have timed out.
			HashSet<String> removeList = new HashSet<String>();

			Set<String> keySet = sessionMap.keySet();
			for (String key : keySet) {
				SessionInfo sessionInfo = sessionMap.get(key);
				if(sessionInfo.getTimestamp() < System.currentTimeMillis()) {
					removeList.add(key);
				}
			}

			for (String key : removeList) {
				SessionInfo sessionInfo = sessionMap.get(key);
				if(sessionInfo != null) {
					synchronized(sessionInfo) {
						sessionMap.remove(key);
					}
				}
			}
		}
	}


	private String getSmallHTMLStr(String smallMsg) {
		return "<h3>" + smallMsg + "</h3>";
	}

	private String handleError(String str) {
		StringBuffer buf = new StringBuffer();
		buf.append("An Error has occurred");
		buf.append("<br/><br/>");
		buf.append(str);
		return createHTML(buf.toString());
	}

	private String assign3HTML(String str, String hiddenMsg, String otherStrings[]) {
		StringBuffer buf = new StringBuffer();
		buf.append("<h1>");
		buf.append("<br/><br/>");
		buf.append("<form>");
		buf.append(str);
		buf.append("<br/><br/>");
		buf.append("<input name=\"replace\" type=\"submit\" value=\"Replace\">");
		buf.append("<input name=\"message\" type=\"text\">");
		buf.append("<br/><br/>");
		buf.append("<input name=\"refresh\" type=\"submit\" value=\"Refresh\">");
		buf.append("<br/><br/>");
		buf.append("<input name=\"logout\" type=\"submit\" value=\"Logout\">");
		buf.append("</form>");

		buf.append("</h1>");


		for (String string : otherStrings) {
			buf.append(string);
		}

		buf.append("<input type=\"button\"  id=\"button1\" " +
				"onclick=\"document.getElementById('info').style.display='block'\" " +
				"value=\"Show more\"/><br/>");

		buf.append("<input type=\"button\"  id=\"button2\" " +
				"onclick=\"document.getElementById('info').style.display = 'none'\" " +
				"value=\"Show less\"/><br/>");

		buf.append("<div id=\"info\" style=\"display:none\">" +
				hiddenMsg + 
		"</div>");


		return createHTML(buf.toString());
	}

	private String createHTML(String str) {
		String docType = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 " +
		"Transitional//EN\">\n";
		return docType + "<HTML>\n" +
		"<HEAD><TITLE>" + title + "</TITLE>" +
		"</HEAD>\n" +
		"<BODY>\n" +
		str + 
		"</BODY></HTML>";
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}