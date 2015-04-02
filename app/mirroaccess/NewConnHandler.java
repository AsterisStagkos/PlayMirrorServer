package mirroaccess;
import java.io.File;

import models.Receiver;
import models.Sender2;

import org.java_websocket.client.WebSocketClient;

import play.mvc.WebSocket;


public class NewConnHandler implements Runnable {

	File f = null;
	String fileName = "";
	int noOfPackets = 0;
	int timeout = 0;
	WebSocket.Out<byte[]> out;
	private static WebSocketClient mWebSocketClient;
	private Sender2 newSender;
    private Receiver newReceiver;
	boolean isReceiver = false;
	String userid;
	
	

	@Override
	public void run() {
		double randomNumber = Math.random();
		int windowSize = 0;
		double timeoutTime = 10;
/*		if (randomNumber < 0.25) {
			windowSize = 32;
			timeoutTime = 0.2;
		} else if (randomNumber >= 0.25 && randomNumber < 0.50) {
			windowSize = 64;
			timeoutTime = 0.5;
		} else if (randomNumber >= 0.50 && randomNumber < 0.75) {
			windowSize = 128;
			timeoutTime = 0.8;
		} else if (randomNumber >= 0.75) {
			windowSize = 256;
			timeoutTime = 1.2;
		} */
		timeoutTime = 0.8;
		windowSize = 256;
		newSender = new Sender2();
		if (out != null) {
			if (!isReceiver) {
			newSender.send(f, timeoutTime, out, windowSize);
			} else {
			newReceiver = new Receiver(fileName, out, noOfPackets, userid);
			newReceiver.receive();
			}
		} else if (mWebSocketClient != null) {
			newSender.send(f, timeoutTime, mWebSocketClient, windowSize);
		}
		
				
	}
	public void setStats(File file, int timeOut, WebSocket.Out<byte[]> socket, boolean isReceiver) {
		this.f = file;
		this.timeout = timeOut;
		this.out = socket;
		this.isReceiver = isReceiver;
	}
	public void setStats(String fileName, WebSocket.Out<byte[]> socket, boolean isReceiver, String userid) {
		this.fileName = fileName;
		this.out = socket;
		this.isReceiver = isReceiver;
		this.userid = userid;
	}
	public void setStats(File file, int timeOut, WebSocketClient socket) {
		this.f = file;
		this.timeout = timeOut;
		this.mWebSocketClient = socket;
	}
	public void eraseStats() {
		this.f = null;
		this.fileName = "";
		this.isReceiver = false;
		this.noOfPackets = 0;
		this.out = null;
		this.mWebSocketClient = null;
	}
	public void setAck(int ack) {
		newSender.setAck(ack);
	}
	public void setFinal(boolean close) {
		System.out.println("Set to final from Pinger Actor");
		if (newSender != null) {
			newSender.setFinal(close);
		}
	}
	public void setNoOfPackets(int noOfPackets) {
		newReceiver.setNoOfPackets(noOfPackets);
		newReceiver.setStartDownload(true);
	}
	public void setFinalReceiver(boolean close) {
		newReceiver.setFinal(close);
	}
	public void newPacket(byte[] newPacket) {
		newReceiver.newPacket(newPacket);
	}
}
