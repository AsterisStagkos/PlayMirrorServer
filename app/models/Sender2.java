package models;

/* Asteris Stagkos Bell 1119414 */
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.TreeMap;

import org.java_websocket.client.WebSocketClient;

import play.mvc.WebSocket;

public class Sender2 {
	private static int acknowledgement;
	private boolean timerIsRunning = false;
	private boolean shouldClose = false;
	private long timerStartTime = 0;
	private int nextSeqNumber = 0;
	private int sequenceBase = 0;
	private int windowsizestatic = 32;
	private int closeThreadCounter = 0;
	private int threadSleepTime = 2;
	private TreeMap<Integer, byte[]> buffer = new TreeMap<Integer, byte[]>();

	public void setFinal(boolean close) {
		shouldClose = close;
	}

	public void setAck(int ack) {
	//	System.out.println("ack set to: " + ack +  " Timer Count " + (System.nanoTime() - timerStartTime));
		acknowledgement = ack;
		if (ack > 100) {
			buffer.remove(ack - windowsizestatic);
		}
		sequenceBase = ack + 1;
		if (sequenceBase == nextSeqNumber && sequenceBase != 1) {
			 timerIsRunning = false;
		} else {
			if (!timerIsRunning) {
				timerIsRunning = true;
				timerStartTime = System.nanoTime();
			}
			
		}
	}

	public void send(File f, double timeoutTime, final WebSocket.Out<byte[]> out,
			int windowSize) {
		// Initialise variables
		shouldClose = false;
		windowsizestatic = windowSize;
		nextSeqNumber = 0;
		sequenceBase = 0;
		boolean packetWasSent = false;
		int ackReceived = 0;
		int numberOfRetransmissions = 0;
		int finCounter = 0;
		long startTime = 0;
		long endTime = 0;
		boolean isFirstPacket = true;
		boolean stopSendingFiles = false;
		try {
			// Initialise sending socket, file input stream ip address and
			// arrays to hold
			// data to be sent and acknowledgements received
			FileInputStream fileStream = new FileInputStream(f);
			// DatagramSocket udpSocket = new DatagramSocket();
			// InetAddress IPAddress = InetAddress.getByName("localhost");
			int noOfPackets = (int) ((f.length()) / 1024);
			
				System.out.println("1031 byte packets");
				byte[] sendData = new byte[1031];
				byte[] ACK = new byte[1];
				ByteBuffer byteBuffer;
				DatagramPacket ACKPacket = new DatagramPacket(ACK, ACK.length);
				// Set packet size value to 1024 for all packets
				// except for last packet, where packet size is set according to
				// the amount of data left in the file
				Thread.sleep(2000);
				String noOfPacketsSend = "noOfPackets " + noOfPackets;
				System.out.println("sent number of packets: " + noOfPackets);
				out.write(noOfPacketsSend.getBytes());
				out.write(noOfPacketsSend.getBytes());
				out.write(noOfPacketsSend.getBytes());
				while (!stopSendingFiles) {
					Thread.sleep(threadSleepTime);
					if (sequenceBase > noOfPackets || shouldClose) {
						System.out.println("Close thread" + " shouldClose: "
								+ shouldClose);
						break;
					}
					if (nextSeqNumber < sequenceBase + windowSize
							&& nextSeqNumber <= (f.length()) / 1024) {
						int packetSizeNum = 0;
						if (fileStream.available() > 1024) {
							fileStream.read(sendData, 7, 1024);
							packetSizeNum = 1024;
						} else {
							for (int w = 7; w < 1031; w++) {
								if (fileStream.available() > 0) {
									sendData[w] = (byte) fileStream.read();
									packetSizeNum++;
								} else {
									sendData[w] = 0;
									sendData[4] = 1;
									stopSendingFiles = true;
									// Add zeros to the end of the packet if we
									// reached the end of the file
									// but the last packet hasn't reached its full
									// size of 1024 bytes yet
								}
							}
						}
						// Use a byte buffer to add the sequence number of the
						// packet to its header
						byte[] sequenceNumber = new byte[4];
						ByteBuffer bytebuffer = ByteBuffer.wrap(sequenceNumber);
						bytebuffer.putInt(nextSeqNumber);
						sendData[0] = sequenceNumber[0];
						sendData[1] = sequenceNumber[1];
						sendData[2] = sequenceNumber[2];
						sendData[3] = sequenceNumber[3];
						sendData[4] = 0;
						// if Packet is the final packet, indicate that in the
						// header
						if (nextSeqNumber * 1024 > f.length() - 1024) {
							stopSendingFiles = true;
							sendData[4] = 1;
							endTime = System.nanoTime();
						}
						// Use a byte buffer to add the packet size of the packet to
						// its header
						byte[] packetSize = new byte[2];
						ByteBuffer byteBufferPackSize = ByteBuffer.wrap(packetSize);
						byteBufferPackSize.putShort((short) packetSizeNum);
						sendData[5] = packetSize[0];
						sendData[6] = packetSize[1];

						/*
						 * Set the receiver to time out if it hasn't received a
						 * packet within the specified timeout time. The sender then
						 * sends a packet and waits to receive an acknowledgement.
						 * If it doesn't, the receiver times out, the loop is
						 * repeated and the same packet is resent. If it does, but
						 * the acknowledgement is not the correct one, the packet is
						 * again resent. once the correct acknowledgement is
						 * received, the loop breaks
						 */
						if (!buffer.containsKey(nextSeqNumber)) {
							buffer.put(nextSeqNumber, sendData.clone());
						}
						// do {
						// System.out.println("Packet sent: " + i);
						// udpSocket.send(packet);
						out.write(sendData);
					//	System.out.println("Packet sent: " + nextSeqNumber);
						if (isFirstPacket) {
						//	System.out.println("TimerStartTime set at 119");
							startTime = System.nanoTime();
							timerStartTime = System.nanoTime();
							isFirstPacket = false;
						}

						if (sequenceBase == nextSeqNumber) {
							if (!timerIsRunning) {
						//		System.out.println("TimerStartTime set at 123");
								timerIsRunning = true;
								timerStartTime = System.nanoTime();
							}
						}
						nextSeqNumber++;

					}
				//	timerStartTime++;
					if ((System.nanoTime() - timerStartTime) > ((timeoutTime) * 1000000000)) {
			//			System.out.println("Timeout");
						closeThreadCounter++;
						timeOut(out);
						numberOfRetransmissions++;
					}
				
			}
			timeOut(out);
			timeOut(out);
			timeOut(out);
			buffer.clear();
			closeThreadCounter = 0;
			out.close();
			double kiloBytesPerSecond = ((noOfPackets) / ((endTime - startTime) / 1000000000));

			System.out.println("Number of Retransmissions: "
					+ numberOfRetransmissions);
			System.out.println("Kilobytes Per Second: "
					+ ((noOfPackets) / ((endTime - startTime) / 1000000000)));
			// udpSocket.close();
			DatabaseHandler.enterUpdateStatement("INSERT INTO downloaddata VALUES ('" + f.getName() +"', " + noOfPackets + ", " + kiloBytesPerSecond + ", " + numberOfRetransmissions + ", " + windowSize  + ", " + timeoutTime + ", " + threadSleepTime + ")" );
			String isExperiment = DatabaseHandler.enterSelectStatement("SELECT * FROM Scientists WHERE scriptname = '" + f.getName() + "'");
			System.out.println("filename: " +isExperiment);
			if (isExperiment.equals("")) {
				f.delete();	
			}
			fileStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception: " + e);
		}
	}
	
	private void timeOut(final WebSocket.Out<byte[]> out) {
//		if (!timerIsRunning) {
			timerIsRunning = true;
			timerStartTime = System.nanoTime();
	//	}
		for (int j = sequenceBase; j < nextSeqNumber; j++) {

			if (buffer.containsKey(j-1) && buffer.get(j-1) != null) {
				out.write(buffer.get(j-1));
				ByteBuffer resentBuffer = ByteBuffer.wrap(buffer
						.get(j-1));
				int actualPacketNo = resentBuffer.getShort(0);
		//		 System.out.println("Resent packet: " + (j-1) +
		//		 " nope actually: " + actualPacketNo);
			}
			try{
			Thread.sleep(threadSleepTime);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// System.out.println("Actually resent packet: " +
			// actualPacketNo);
		}
	}
	
	public void send(File f, double timeoutTime, final WebSocketClient out,
			int windowSize) {
		// Initialise variables
		shouldClose = false;
		windowsizestatic = windowSize;
		nextSeqNumber = 0;
		sequenceBase = 0;
		boolean packetWasSent = false;
		int ackReceived = 0;
		int numberOfRetransmissions = 0;
		int finCounter = 0;
		long startTime = 0;
		long endTime = 0;
		boolean isFirstPacket = true;
		boolean stopSendingFiles = false;
		try {
			// Initialise sending socket, file input stream ip address and
			// arrays to hold
			// data to be sent and acknowledgements received
			FileInputStream fileStream = new FileInputStream(f);
			// DatagramSocket udpSocket = new DatagramSocket();
			// InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[1031];
			byte[] ACK = new byte[1];
			ByteBuffer byteBuffer;
			DatagramPacket ACKPacket = new DatagramPacket(ACK, ACK.length);
			// Set packet size value to 1024 for all packets
			// except for last packet, where packet size is set according to
			// the amount of data left in the file
			int noOfPackets = (int) ((f.length()) / 1024);
			String noOfPacketsSend = "noOfPackets " + noOfPackets;
			System.out.println("sent number of packets: " + noOfPackets);
			out.send(noOfPacketsSend.getBytes());
			out.send(noOfPacketsSend.getBytes());
			out.send(noOfPacketsSend.getBytes());
			while (!stopSendingFiles) {
				Thread.sleep(threadSleepTime+5);
				if (sequenceBase > noOfPackets || shouldClose) {
					System.out.println("Close thread" + " shouldClose: "
							+ shouldClose);
					break;
				}
				if (nextSeqNumber < sequenceBase + windowSize
						&& nextSeqNumber <= (f.length()) / 1024) {
					int packetSizeNum = 0;
					if (fileStream.available() > 1024) {
						fileStream.read(sendData, 7, 1024);
						packetSizeNum = 1024;
					} else {
						for (int w = 7; w < 1031; w++) {
							if (fileStream.available() > 0) {
								sendData[w] = (byte) fileStream.read();
								packetSizeNum++;
							} else {
								sendData[w] = 0;
								sendData[4] = 1;
								stopSendingFiles = true;
								// Add zeros to the end of the packet if we
								// reached the end of the file
								// but the last packet hasn't reached its full
								// size of 1024 bytes yet
							}
						}
					}
					// Use a byte buffer to add the sequence number of the
					// packet to its header
					byte[] sequenceNumber = new byte[4];
					ByteBuffer bytebuffer = ByteBuffer.wrap(sequenceNumber);
					bytebuffer.putInt(nextSeqNumber);
					sendData[0] = sequenceNumber[0];
					sendData[1] = sequenceNumber[1];
					sendData[2] = sequenceNumber[2];
					sendData[3] = sequenceNumber[3];
					sendData[4] = 0;
					// if Packet is the final packet, indicate that in the
					// header
					if (nextSeqNumber * 1024 > f.length() - 1024) {
						stopSendingFiles = true;
						sendData[4] = 1;
						endTime = System.nanoTime();
					}
					// Use a byte buffer to add the packet size of the packet to
					// its header
					byte[] packetSize = new byte[2];
					ByteBuffer byteBufferPackSize = ByteBuffer.wrap(packetSize);
					byteBufferPackSize.putShort((short) packetSizeNum);
					sendData[5] = packetSize[0];
					sendData[6] = packetSize[1];

					/*
					 * Set the receiver to time out if it hasn't received a
					 * packet within the specified timeout time. The sender then
					 * sends a packet and waits to receive an acknowledgement.
					 * If it doesn't, the receiver times out, the loop is
					 * repeated and the same packet is resent. If it does, but
					 * the acknowledgement is not the correct one, the packet is
					 * again resent. once the correct acknowledgement is
					 * received, the loop breaks
					 */
					if (!buffer.containsKey(nextSeqNumber)) {
						buffer.put(nextSeqNumber, sendData.clone());
					}
					// do {
					// System.out.println("Packet sent: " + i);
					// udpSocket.send(packet);
					out.send(sendData);
				//	System.out.println("Packet sent: " + nextSeqNumber);
					if (isFirstPacket) {
				//		System.out.println("TimerStartTime set at 119");
						startTime = System.nanoTime();
						timerStartTime = System.nanoTime();
						isFirstPacket = false;
					}

					if (sequenceBase == nextSeqNumber) {
						if (!timerIsRunning) {
				//			System.out.println("TimerStartTime set at 123");
							timerIsRunning = true;
							timerStartTime = System.nanoTime();
						}
					}
					nextSeqNumber++;

				}
			//	timerStartTime++;
				if ((System.nanoTime() - timerStartTime) > (timeoutTime * 1000000000)) {
			//		System.out.println("Timeout");
					closeThreadCounter++;
					timeOut(out);
					numberOfRetransmissions++;
				}
			}
			timeOut(out);
			timeOut(out);
			timeOut(out);
			out.close();
			buffer.clear();
			closeThreadCounter = 0;
			System.out.println("Number of Retransmissions: "
					+ numberOfRetransmissions);
			double kiloBytesPerSecond = (noOfPackets / ((endTime - startTime) / 1000000000));
			System.out.println("Kilobytes Per Second: "
					+ (noOfPackets / ((endTime - startTime) / 1000000000)));
			// udpSocket.close();
			DatabaseHandler.enterUpdateStatement("INSERT INTO downloaddata VALUES ('" + f.getName() +"', " + noOfPackets + ", " + kiloBytesPerSecond + ", " + numberOfRetransmissions + ", " + windowSize  + ", " + timeoutTime + ", " + threadSleepTime + ")" );

			fileStream.close();
			String isExperiment = DatabaseHandler.enterSelectStatement("SELECT * FROM Scientists WHERE scriptname = '" + f.getName() + "'");
			System.out.println("filename: " +isExperiment);
			if (isExperiment.equals("")) {
				f.delete();	
			}	
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception: " + e);
		}
	}
	
	private void timeOut(final WebSocketClient out) {
//		if (!timerIsRunning) {
			timerIsRunning = true;
			timerStartTime = System.nanoTime();
	//	}
		for (int j = sequenceBase; j < nextSeqNumber; j++) {
			
			if (buffer.containsKey(j-1) && buffer.get(j-1) != null) {
				out.send(buffer.get(j-1));
				ByteBuffer resentBuffer = ByteBuffer.wrap(buffer
						.get(j-1));
				int actualPacketNo = resentBuffer.getShort(0);
			//	 System.out.println("Resent packet: " + (j-1) +
			//	 " nope actually: " + actualPacketNo);
			}
			try{
			Thread.sleep(threadSleepTime+5);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// System.out.println("Actually resent packet: " +
			// actualPacketNo);
		}
	}

}