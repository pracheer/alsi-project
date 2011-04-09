package ssm;

import ssm.messages.GeneralMsg;
import ssm.messages.Message;
import ssm.messages.Ping;

public class Operation {

	enum OpCode {
		PING,
		GET,
		PUT, 
		REMOVE
	}
	
	OpCode opCode;
	int callId;
	boolean error;
	String errorMsg;
	Message message;
	
	private static final String OP_SEP = "_";

	public Operation(int callId, OpCode opCode, Message message) {
		this.opCode = opCode;
		this.callId = callId;
		this.message = message;
		this.error = false;
	}
	
	public Operation(int callId, OpCode opCode, boolean error, String errorMsg, Message message) {
		this.opCode = opCode;
		this.callId = callId;
		this.error = false;
		this.errorMsg = errorMsg;
		this.message = message;
	}
	
	public OpCode getOpCode() {
		return opCode;
	}
	
	public int getCallId() {
		return callId;
	}
	
	public Message getMessage() {
		return message;
	}
	
	public void setMessage(Message message) {
		this.message = message;
	}
	
	public boolean isError() {
		return error;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.error = true;
		this.errorMsg = errorMsg;
	}
	
	/**
	 * to unroll the operation from a string sent by request.
	 * @param opString
	 * @return
	 */
	public static Operation fromString(String opString) {
		
		String[] strings = opString.split(OP_SEP);
		
		int callId = Integer.parseInt(strings[0]);
		String opCodeStr = strings[1];
		boolean error = Boolean.parseBoolean(strings[2]);
		String errorMsg = strings[3];
		
		// The remaining string indicates the Message.
		int msgIndex = (strings[0]+OP_SEP+strings[1]+OP_SEP+strings[2]+OP_SEP+strings[3]+OP_SEP).length();
		String msgString = opString.substring(msgIndex);
		
		if(opCodeStr.equalsIgnoreCase(OpCode.PING.toString())) {
			Message message = new Ping();
			return new Operation(callId, OpCode.PING, error, errorMsg, message);
		}
		else if (opCodeStr.equalsIgnoreCase(OpCode.GET.toString())) {
			Message message = GeneralMsg.fromString(msgString);
			return new Operation(callId, OpCode.GET, error, errorMsg, message);
		}
		else if(opCodeStr.equalsIgnoreCase(OpCode.PUT.toString())) {
			Message message = GeneralMsg.fromString(msgString);
			return new Operation(callId, OpCode.PUT, error, errorMsg, message);
		}
		else if(opCodeStr.equalsIgnoreCase(OpCode.REMOVE.toString())) {
			Message message = GeneralMsg.fromString(msgString);
			return new Operation(callId, OpCode.REMOVE, error, errorMsg, message);
		}
		
		return null;
	}
	
	public String toString() {
		return callId + OP_SEP + opCode + OP_SEP + error + OP_SEP + errorMsg + OP_SEP + message;
	}
}
