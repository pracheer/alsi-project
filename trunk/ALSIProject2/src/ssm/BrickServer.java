package ssm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import ssm.messages.Message;
import ssm.messages.GeneralMsg;

public class BrickServer implements Runnable {

	DatagramSocket rpcSocket;
	byte[] buffer;
	private HashMap<String, SessionInfo> sessionMap;
	public static String INVALID_VERSION = "Invalid Version found";
	private int port;
	private String ip;

	public BrickServer(HashMap<String, SessionInfo> sessionMap) {
		this.sessionMap = sessionMap;
		try {
			rpcSocket = new DatagramSocket();
			SocketAddress tmpSocket = rpcSocket.getLocalSocketAddress();
			if(tmpSocket instanceof InetSocketAddress) {
				InetSocketAddress socket = (InetSocketAddress)tmpSocket;
				port  = socket.getPort();
				ip = InetAddress.getLocalHost().getHostAddress();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public int getPort() {
		return port;
	}
	
	public String getIP() {
		return ip;
	}
	
	@Override
	public void run() {
		try {
			while(true) {
				byte[] inBuf = new byte[Constants.DATAGRAM_SIZE];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				
				rpcSocket.receive(recvPkt);
				
				byte[] outBuf = computeResponse(recvPkt.getData());
				DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length,
						recvPkt.getAddress(), recvPkt.getPort());
				
				rpcSocket.send(sendPkt);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Operation computeResponseOperation(Operation operation) {

		Message message = operation.getMessage();

		switch(operation.getOpCode()) {
		case PING:
			// return the same thing again.
			return operation;

		case GET:

			if(message instanceof GeneralMsg) {
				GeneralMsg getMsg = (GeneralMsg)message;
				if(sessionMap.containsKey(getMsg.getSessionId())) {
					SessionInfo sessionInfo = sessionMap.get(getMsg.getSessionId());
					synchronized (sessionInfo) {
						if(sessionInfo.getVersion() == getMsg.getVersion()) {

							// Timeout
							if(sessionInfo.getTimestamp() < System.currentTimeMillis()) {
								operation.setErrorMsg("Data is quite old. So, \"TIMED OUT\"");
								sessionMap.remove(getMsg.getSessionId());
								return operation;
							}

							Value value = sessionInfo.getValue();
							getMsg.setValue(value);
							operation.setMessage(getMsg);
							return operation;
						}
						else {
							operation.setErrorMsg(INVALID_VERSION);
							return operation;
						}
					}
				}
				else {
					operation.setErrorMsg("Session not found");
					return operation;
				}
			}
			break;

		case PUT:
			if(message instanceof GeneralMsg) {
				GeneralMsg putMsg = (GeneralMsg)message;
				SessionInfo newSessionInfo = SessionInfo.create(putMsg.getValue(), putMsg.getVersion());
				if(sessionMap.containsKey(putMsg.getSessionId())) {
					SessionInfo sessionInfo = sessionMap.get(putMsg.getSessionId());
					synchronized(sessionInfo) {
						sessionMap.put(putMsg.getSessionId(), newSessionInfo);
					}
				}
				else {
					sessionMap.put(putMsg.getSessionId(), newSessionInfo);
				}
				putMsg.removeValue();
				operation.setMessage(putMsg);
				return operation;
			}
			break;

		case REMOVE:
			if(message instanceof GeneralMsg) {
				GeneralMsg putMsg = (GeneralMsg)message;
				if(sessionMap.containsKey(putMsg.getSessionId())) {
					SessionInfo sessionInfo = sessionMap.get(putMsg.getSessionId());
					synchronized(sessionInfo) {
						sessionMap.remove(putMsg.getSessionId());
					}
				}
				else {
					operation.setErrorMsg("Session not found");
					return operation;
				}
			}
			break;
		}

		return null;

	}
	private byte[] computeResponse(byte[] data) {
		Operation operation = Operation.fromString(new String(data));
		Operation operationOut = computeResponseOperation(operation);
		return operationOut.toString().getBytes();
	}
}
