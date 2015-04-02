package mirroaccess;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import models.Pinger;
import play.mvc.WebSocket;

import com.gc.android.market.api.MarketSession;

public class Sender {
	public static Socket tcpConnect(ServerSocket welcomeSocket, int port) {
		try {
			String clientSentence;
			String capitalizedSentence;
			Socket connectionSocket = null;
			while (true) {
				connectionSocket = welcomeSocket.accept();
				BufferedReader inFromClient = new BufferedReader(
						new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(
						connectionSocket.getOutputStream());
				clientSentence = inFromClient.readLine();
				System.out.println("Received: " + clientSentence);
				capitalizedSentence = clientSentence.toUpperCase() + '\n';
				outToClient.writeBytes(capitalizedSentence);
				ConnectionParams c = new ConnectionParams(
						connectionSocket.getInetAddress(),
						connectionSocket.getPort());
				return connectionSocket;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void tcpSendFile(Socket socket, File f) {
		try {

			FileInputStream fRead = new FileInputStream(f);

			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream output = new DataOutputStream(
					socket.getOutputStream());
			// Write size
			output.writeLong(f.length());
			System.out.println("File length: " + f.length());
			// Write bytes
			while (fRead.available() > 0) {
				output.write(fRead.read());
			}
			System.out.println("Successfully sent file");
		} catch (Exception e) {

		}

	}
	public static void sendNotFound(Socket socket) {
		try {
			DataOutputStream output = new DataOutputStream(
					socket.getOutputStream());
			// Write size
			output.writeLong(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void tcpSendError(Socket socket) {
		try {

			DataOutputStream output = new DataOutputStream(
					socket.getOutputStream());
			// Write size
			output.writeLong(0);
			System.out.println("Successfully sent error");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public Socket waitForConnection(ServerSocket welcomeSocket, int port) {
		Socket newSocket = null;
		try{
			System.out.println("Waiting for Connection on port: " + port);
			newSocket = tcpConnect(welcomeSocket, port);
			return newSocket;
		} catch(Exception e) {
			return newSocket;
		}
	}
	public void waitForAction(Socket connectionSocket, int port, Pinger p) {
		try {
			while (true) {
//				while (connectionSocket == null) {
//					System.out.println("Waiting for Connection on port: " + port);
//					connectionSocket = tcpConnect(port);
//				}
				
				BufferedReader inFromClient = new BufferedReader(
						new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(
						connectionSocket.getOutputStream());
				String clientSentence = inFromClient.readLine();
				System.out.println("Received: " + clientSentence);
				if (clientSentence != null) {
				StringTokenizer st = new StringTokenizer(clientSentence);
				String action = "";
				String query = "";
				String assetID  = "";
				String assetName = "default";
				String token = "";
				String androidId = "";
				int whichExperiment = 0;
				if (st.hasMoreTokens()) {
					action = st.nextToken();
				}
				if (action.equals("query")) {
					if (st.hasMoreTokens()) {
						query = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						token = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						androidId = st.nextToken();
					}
				} else if (action.equals("download")) {
					if (st.hasMoreTokens()) {
						assetID = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						assetName = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						whichExperiment = Integer.parseInt(st.nextToken());
					}
					if (st.hasMoreTokens()) {
						token = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						androidId = st.nextToken();
					}
				}
				if (action.equals("query")) {
					String searchResults = searchMarket(query, token, androidId);
					System.out.println("Returned from search market: " + searchResults);
					sendData(connectionSocket, searchResults);
				} else if (action.equals("download")) {
					try {
					downloadApp(assetID, assetName, token, androidId, p);
					if (whichExperiment != 0) {
						sendToScientist(whichExperiment);
					}
					sendApp(connectionSocket, assetName);
					} catch(IndexOutOfBoundsException e) {
						System.out.println("send error");
						tcpSendError(connectionSocket);
						e.printStackTrace();				
					}
				}
				else {
					System.out.println("Incorrect Action received");
				}
			} else {
				connectionSocket = null;
			}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static String waitForWebSocketAction(String clientSentence, WebSocket.Out<byte[]> out, Pinger p) {
		
				if (clientSentence != null) {
				StringTokenizer st = new StringTokenizer(clientSentence);
				String action = "";
				String query = "";
				String assetID  = "";
				String assetName = "default";
				String token = "";
				String androidId = "";
				String userUniqueId = "";
				int whichExperiment = 0;
				if (st.hasMoreTokens()) {
					action = st.nextToken();
				}
				if (action.equals("query")) {
					if (st.hasMoreTokens()) {
						query = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						token = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						androidId = st.nextToken();
					}
				} else if (action.equals("download")) {
					if (st.hasMoreTokens()) {
						assetID = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						assetName = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						whichExperiment = Integer.parseInt(st.nextToken());
					}
					if (st.hasMoreTokens()) {
						token = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						androidId = st.nextToken();
					}
					if (st.hasMoreTokens()) {
						userUniqueId = st.nextToken();
					}
				}
				if (action.equals("query")) {
					String searchResults = searchMarket(query, token, androidId);
					System.out.println("Returned from search market: " + searchResults);
					return "query " +searchResults;
					
				} else if (action.equals("download")) {
					try {
					downloadApp(assetID, assetName, token, androidId, p);
					if (whichExperiment != 0) {
						sendToScientist(whichExperiment);
					}
					
				//	sendApp(connectionSocket, assetName);
					} catch(IndexOutOfBoundsException e) {
						p.setErrorGiven(true);
						//Pinger.errorGiven = true;
						out.write("noOfPackets 0".getBytes());
						System.out.println("send errorrr	");
				//		tcpSendError(connectionSocket);
						e.printStackTrace();	
						return "outOfBoundsException " + "exception";
					}
					return "download " + assetName + " " + userUniqueId;
				} 
				else {
					return clientSentence;
				}
			} 
				return "";
		
	}
	public static void sendToScientist(int whichScientist) {
		
	}
	public static void sendData(Socket connectionSocket, String data) {
		System.out.println("Sending data: " + data);
		try {
		DataOutputStream outToClient = new DataOutputStream(
				connectionSocket.getOutputStream());
		outToClient.writeLong(data.length());
		outToClient.writeBytes(data);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendApp(Socket connectionSocket, String appName) {
		File f = new File(appName + ".apk");
		if (f.exists()) {
			System.out.println("file found in directory: " + f.getAbsolutePath());
			tcpSendFile(connectionSocket, f);
		} else {
			System.out.println("file not found in directory");
			sendNotFound(connectionSocket);
		}
		
	}
	public static String searchMarket(String query, String token, String androidId) {
		try {
		String androidID4 = "35E1811C627F2BAB";
		String result = "";
		Downloader dl = new Downloader();
		MarketSession session = dl.authenticate(androidId, false, token);
		result = dl.searchApp(query, session);
		return result;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String downloadApp(String assetID, String assetName, String token, String androidId, Pinger p) {
		try {
		String result = "";
		Downloader dl = new Downloader();
		MarketSession session = dl.authenticate(androidId, true, token);
		try {
			dl.downloadApp(assetID, assetName, session, p);
		} catch (IndexOutOfBoundsException e) {
			p.setErrorGiven(true);
			e.printStackTrace();
		}
		return result;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static void main(String[] args) {
		// int port = Integer.parseInt(args[0]);
		File f = new File("C:\\Users\\User\\Desktop\\Semester1Deadlines.txt");
		String address = "";
		int port = 12121;
		try{
		ServerSocket welcomeSocket = new ServerSocket(port);
		while(true) {
			Socket conn = null;
			Sender sender = new Sender();
			conn = sender.waitForConnection(welcomeSocket, port);
			if (conn != null) {
//			Runnable newConnection = new NewConnHandler(conn, port);
//			new Thread(newConnection).start();
			}
			try{
			Thread.sleep(1000);
			} catch(InterruptedException e) {
				
			}
			}
		} catch (Exception e) {
			
		}
		
		
		//waitForAction(conn, port);

	}
}