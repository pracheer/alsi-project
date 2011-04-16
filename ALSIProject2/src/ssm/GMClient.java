/**
 * 
 */
package ssm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

import ssm.Operation.OpCode;
import ssm.messages.Ping;

/**
 * Group Membership Client.
 * 
 * @author prac
 *
 */
public class GMClient implements Runnable {

	private Members members;
	private Member me;
	private Random randomGenerator;

	public GMClient(Members members, Member me) {
		this.members = members;
		this.me = me;
		randomGenerator = new Random();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		try {
			while (true) {
				System.out.println("GMClient invoked");
				int callId = randomGenerator.nextInt();
				Random randomGenerator = new Random();
				DatagramSocket rpcSocket = null;
				rpcSocket = new DatagramSocket();

				// getmembers for simpleDB.
				SimpleDBInterface instance = SimpleDBInterface.getInstance();
				Members dbMembers = instance.getMembers();
				boolean found = false;
				boolean timeout = false;
				boolean updated = false;
				
				if(dbMembers.size()!=0) {
					found = dbMembers.search(me);
					int rand = randomGenerator.nextInt(dbMembers.size());
					Member testMember = dbMembers.get(rand);
					InetAddress address = InetAddress.getByName(testMember.getIp());
					Operation operation = new Operation(callId, OpCode.PING, new Ping());
					System.out.println("Sending ping message to " + testMember);
					byte[] buf = operation.toString().getBytes();
					DatagramPacket packet = new DatagramPacket(buf, buf.length, address, testMember.getPort());
					try {

						rpcSocket.send(packet);

						rpcSocket.setSoTimeout(Constants.TIMEOUT);

						buf = new byte[Constants.DATAGRAM_SIZE];
						DatagramPacket recvPkt = new DatagramPacket(buf, Constants.DATAGRAM_SIZE);
						rpcSocket.receive(recvPkt);
						timeout = false;
						
						System.out.println("Received reply to ping");

					} catch (SocketTimeoutException e) {
						timeout = true;
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if (timeout) {
						System.out.println("timeout occurred. Removing "+testMember+" from simpleDB");
						dbMembers.remove(testMember);
						updated = true;
						instance.removeMember(testMember.getIp(), testMember.getPort());
					}
					
				}
				
				if (!found) {
					System.out.println("Adding myself ("+me+") to simpleDB");
					instance.addMember(me.getIp(), me.getPort());
					dbMembers.add(me);
					updated = true;
				}
				
				members.updateList(dbMembers);

				int sleepTime = Constants.roundTime/2 + randomGenerator.nextInt(Constants.roundTime);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

}
