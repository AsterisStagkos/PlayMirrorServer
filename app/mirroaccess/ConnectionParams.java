package mirroaccess;


import java.net.InetAddress;

public class ConnectionParams {
	InetAddress phoneAddress;
	int phonePort;
	
	public ConnectionParams(InetAddress phoneAddress, int phonePort) {
		this.phoneAddress = phoneAddress;
		this.phonePort = phonePort;
	}
	
	public InetAddress getPhoneAddress() {
		return this.phoneAddress;
	}
	public int getPhonePort() {
		return this.phonePort;
	}
}
