package models;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.StringTokenizer;

import mirroaccess.Experiment;
import mirroaccess.NewConnHandler;
import mirroaccess.Sender;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.mvc.WebSocket;
import akka.actor.UntypedActor;

public class Pinger extends UntypedActor {
	private WebSocket.In<byte[]> in;
	private WebSocket.Out<byte[]> out;
	private WebSocketClient mWebSocketClient;
	private boolean downloadComplete = false;
	private boolean errorGiven = false;

	public Pinger(WebSocket.In<byte[]> in, WebSocket.Out<byte[]> out) {
		this.in = in;
		this.out = out;
		startActor();
	}
	public void setDownloadComplete(boolean downloadComplete) {
		this.downloadComplete = downloadComplete;
	}
	public void setErrorGiven(boolean errorGiven) {
		this.errorGiven = errorGiven;
	}
	private void startActor() {
		final NewConnHandler actorThread = new NewConnHandler();
		in.onMessage(new Callback<byte[]>() {
			public void invoke(byte[] event) {

				String message = new String(event);
				// System.out.println("Message received: " + message);
				String reply = Sender.waitForWebSocketAction(message, out, Pinger.this);
				// System.out.println("reply: " + reply);
				StringTokenizer replyTokenizer = new StringTokenizer(reply);
				if (replyTokenizer.hasMoreTokens()) {
					String firstReplyToken = replyTokenizer.nextToken();
					if (firstReplyToken.equals("query")) {
						out.write(reply.getBytes());
					} else if (firstReplyToken.equals("download")) {
						try {
							String userid = "";
							String fileName = "";
							if (replyTokenizer.hasMoreTokens()) {
								fileName = replyTokenizer.nextToken() + ".apk";
							}
							if (replyTokenizer.hasMoreTokens()) {
								userid = replyTokenizer.nextToken();
							}
							String usersResult = "";
							usersResult = DatabaseHandler
									.enterSelectStatement("SELECT * FROM Users WHERE userid = '"
											+ userid + "'");
							System.out.println("file name given: " + fileName);
							while (!downloadComplete && !errorGiven) {
								Thread.sleep(50);
							}
						//	timeToDownload = (long) ((double) timeToDownload / (double) 1000000);
							downloadComplete = false;
							errorGiven = false;
							/*
							 * If the file exists, it means it was downloaded
							 * from android market api successfully, so proceed
							 * else, send an error code to inform the client
							 * that the app does not exist
							 */
							if (new File(fileName).exists()) {
								String experiment = DatabaseHandler
										.enterSelectStatement("SELECT experimentidOne FROM ExP WHERE userid = '"
												+ userid + "'");
								experiment = experiment.trim();
								System.out
										.println("experiment to participate in: "
												+ experiment);
								/*
								 * If the user is not participating in any
								 * experiments, send the file to the user
								 * otherwise, send the file to the specified
								 * scientist for instrumentation
								 */
								if (experiment.equals("NoExperimentSelected")
										|| experiment.equals("")) {
									if (!usersResult.equals("")) {
										int downloadsNo = 0;
										int experimentsNo = 0;
										int questionsNo = 0;
										StringTokenizer usersTokens = new StringTokenizer(usersResult);
										if (usersTokens.hasMoreTokens()) {
											//skip userid 
											usersTokens.nextToken();
										}
										if (usersTokens.hasMoreTokens()) {
											downloadsNo = Integer.parseInt(usersTokens.nextToken());
										}
										if (usersTokens.hasMoreTokens()) {
											experimentsNo = Integer.parseInt(usersTokens.nextToken());
										}
										if (usersTokens.hasMoreTokens()) {
											questionsNo = Integer.parseInt(usersTokens.nextToken());
										}
										downloadsNo++;
										DatabaseHandler
										.enterUpdateStatement("UPDATE Users SET downloadsNo = " + downloadsNo + " WHERE userid = '"
												+ userid + "'");
										
									}
									File f = new File(fileName);
									actorThread.setStats(f, 1000, out, false);
									new Thread(actorThread).start();
								} else {
									DatabaseHandler.enterUpdateStatement("INSERT INTO ScientistData VALUES ('" + experiment + "', '" + userid + "')" );
									sendToScientist(experiment, fileName,
											userid);
								}

							} else {
								DatabaseHandler.enterUpdateStatement("INSERT INTO Userfails VALUES ('" + userid + "', " + 1 + ")" );
								System.out.println("File does not exist");
								Thread.sleep(2500);
								out.write("noOfPackets 0".getBytes());
							}

						} catch (Exception e) {
							e.printStackTrace();
							try {
								Thread.sleep(2500);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							out.write("noOfPackets 0".getBytes());
						}
					} else if (firstReplyToken.equals("ack")) {
						// System.out.println("message received: " + message);
						try {
							int acknowledgement = 0;
							if (replyTokenizer.hasMoreTokens()) {
								String token = replyTokenizer.nextToken();
								if (token.equals("final")) {
									actorThread.setFinal(true);
									// actorThread.
								} else {
									acknowledgement = Integer.parseInt(token);
								}
							}
							// int acknowledgement = Integer.parseInt(message);
							// System.out.println("message received is an ack: "
							// + acknowledgement);
							actorThread.setAck(acknowledgement);
						} catch (Exception e) {
							System.out.println("not a number");
						}
					} else if (firstReplyToken.equals("outOfBoundsException")) {
						System.out.println("Out of bounds exception caught");
						String toSend = "noOfPackets 0";
						out.write(toSend.getBytes());
						out.write(toSend.getBytes());
						out.write(toSend.getBytes());
						out.close();
					} else if (firstReplyToken.equals("Experiment")) {
						ArrayList<Experiment> experiments = new ArrayList<Experiment>();
						try {
							String experimentsFromDb = DatabaseHandler
									.enterSelectStatement("SELECT * FROM Scientists");
							System.out.println("experimentsFromDb: "
									+ experimentsFromDb);
							String[] experimentsSplit = experimentsFromDb
									.split("\n");
							for (int k = 0; k < experimentsSplit.length; k++) {
								String currentExperiment = experimentsSplit[k];
								String address = "";
								String fname = "";
								String lname = "";
								String expname = "";
								String scriptname = "";
								String expdesc = "";
								String isIndependent = "";
								isIndependent = currentExperiment.trim()
										.substring(currentExperiment.length() - 3);
								currentExperiment =currentExperiment.trim()
										.substring(0,
												currentExperiment.length() - 3);
								StringTokenizer expTokenizer = new StringTokenizer(
										currentExperiment);

								if (expTokenizer.hasMoreTokens()) {
									address = expTokenizer.nextToken();
								}
								if (expTokenizer.hasMoreTokens()) {
									fname = expTokenizer.nextToken();
								} 
								if (expTokenizer.hasMoreTokens()) {
									lname = expTokenizer.nextToken();
								}
								if (expTokenizer.hasMoreTokens()) {
									expname = expTokenizer.nextToken();
								}	
								if (expTokenizer.hasMoreTokens()) {
									scriptname = expTokenizer.nextToken();
								}
								while (expTokenizer.hasMoreTokens()) {
									expdesc += expTokenizer.nextToken() + " ";
								}
								System.out.println("isIndependent: " + isIndependent);
								boolean isIndBool = (isIndependent.trim().equals("f")) ? false : true;
								Experiment e = new Experiment(fname + " " + lname, expname, expdesc, address, scriptname, isIndBool);
								experiments.add(e);
							}
						} catch (SQLException | URISyntaxException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

//						Experiment dataCollector = new Experiment(
//								"Peter Henderson",
//								"DataCollector",
//								"Data Collector is an application which collects battery usage data to help improve battery efficiency",
//								"Peter Henderson", "DataCollector1.0.5.apk",
//								true);
//						Experiment bbinstrumenter = new Experiment(
//								"Pavlos Petoumenos",
//								"bbinstrumenter",
//								"Each basic block of applications are given a counter, to test how much each part of the code is used.",
//								"ad", "runInstrumenter.sh", false);
//						experiments.add(dataCollector);
//						experiments.add(bbinstrumenter);

						Iterator expIterator = experiments.iterator();
						String toOutput = "";
						while (expIterator.hasNext()) {
							Experiment e = (Experiment) expIterator.next();
							toOutput += ("experiments " + e.getName() + "<"
									+ e.getAddress() + "<" + e.getCreator()
									+ "<" + e.getDescription() + "<"
									+ e.getFilePath() + "<"
									+ String.valueOf(e.isIndependent()) + ">nextapp<");
						}
						out.write(toOutput.getBytes());
						out.close();

					} else if (firstReplyToken.equals("DownloadExperiment")) {
						try {
							String filePath = "";
							String userid = "";
							if (replyTokenizer.hasMoreTokens()) {
								filePath = replyTokenizer.nextToken();
							}
							if (replyTokenizer.hasMoreTokens()) {
								userid = replyTokenizer.nextToken();
							}
							String usersResult = "";
							usersResult = DatabaseHandler
									.enterSelectStatement("SELECT * FROM Users WHERE userid = '"
											+ userid + "'");
							System.out.println("file name given: " + filePath);
							if (new File(filePath).exists()) {
								if (!usersResult.equals("")) {
									int downloadsNo = 0;
									int experimentsNo = 0;
									int questionsNo = 0;
									StringTokenizer usersTokens = new StringTokenizer(usersResult);
									if (usersTokens.hasMoreTokens()) {
										//skip userid 
										usersTokens.nextToken();
									}
									if (usersTokens.hasMoreTokens()) {
										downloadsNo = Integer.parseInt(usersTokens.nextToken());
									}
									if (usersTokens.hasMoreTokens()) {
										experimentsNo = Integer.parseInt(usersTokens.nextToken());
									}
									if (usersTokens.hasMoreTokens()) {
										questionsNo = Integer.parseInt(usersTokens.nextToken());
									}
									experimentsNo++;
									DatabaseHandler
									.enterUpdateStatement("UPDATE Users SET experimentsNo = " + experimentsNo + " WHERE userid = '"
											+ userid + "'");
									
								}
								Thread.sleep(1000);
								File f = new File(filePath);
								actorThread.setStats(f, 1000, out, false);
								new Thread(actorThread).start();
							} else {
								System.out.println("File does not exist");
								out.write("noOfPackets 0".getBytes());
								out.close();

							}

						} catch (Exception e) {
							e.printStackTrace();
							out.write("noOfPackets 0".getBytes());
						}
					} else if (firstReplyToken.equals("participate")) {
						System.out
								.println("User says he wants to participate in experiment");
						String experimentID = "";
						String androidIDStr = "";
						if (replyTokenizer.hasMoreTokens()) {
							experimentID = replyTokenizer.nextToken();
						}
						if (replyTokenizer.hasMoreTokens()) {
							androidIDStr = replyTokenizer.nextToken();
						}
						if (!androidIDStr.equals("")
								&& !experimentID.equals("")) {
							try {
								String userid = DatabaseHandler
										.enterSelectStatement(
												"SELECT userid FROM ExP WHERE userid = '"
														+ androidIDStr + "'")
										.trim();
								System.out
										.println("User id found in database: "
												+ userid);
								if (userid.equals(androidIDStr)) {
									DatabaseHandler
											.enterUpdateStatement("UPDATE Exp SET experimentidOne = '"
													+ experimentID
													+ "' WHERE userid = '"
													+ userid + "'");
								} else {
									DatabaseHandler
											.enterUpdateStatement("INSERT INTO ExP VALUES ('"
													+ androidIDStr
													+ "', '"
													+ experimentID + "')");
								}
							} catch (SQLException | URISyntaxException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						out.close();
					} else if (firstReplyToken.equals("executeSQL")) {
						System.out.println("User wants to execute sql: "
								+ message);
						try {
							if (replyTokenizer.hasMoreTokens()) {
								String firstCommand = replyTokenizer
										.nextToken();
								if (firstCommand.equals("SELECT")) {
									String dbresult = DatabaseHandler
											.enterSelectStatement(message
													.substring(10));
									out.write(dbresult.getBytes());
								} else {
									DatabaseHandler
											.enterUpdateStatement(message
													.substring(10));
								}
							}
						} catch (SQLException | URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					//	out.close();
					} else if (firstReplyToken.equals("receive")) {
						String fileName = "";
						String userid = "";
						if (replyTokenizer.hasMoreTokens()) {
							fileName = replyTokenizer.nextToken();
						}
						if (replyTokenizer.hasMoreTokens()) {
							userid = replyTokenizer.nextToken();
						}
						actorThread.setStats(fileName, out, true, userid);
						new Thread(actorThread).start();
					} else if (firstReplyToken.equals("noOfPackets")) {
						int noOfPackets = 0;
						if (replyTokenizer.hasMoreTokens()) {
							noOfPackets = Integer.parseInt(replyTokenizer
									.nextToken());
						}
						actorThread.setNoOfPackets(noOfPackets);
						System.out.println("Received no of packets: "
								+ noOfPackets);

					} else if (firstReplyToken.equals("Hey")) {
						// ignore
					} else if (firstReplyToken.equals("CheckDownloads")) {
						String userid = "";
						if (replyTokenizer.hasMoreTokens()) {
							userid = replyTokenizer.nextToken();
						}
						try {
							String usersResult = DatabaseHandler
									.enterSelectStatement("SELECT * FROM Users WHERE userid = '"
											+ userid + "'");
							String databaseResult = DatabaseHandler
									.enterSelectStatement("SELECT filename FROM waitingfiles WHERE userid = '"
											+ userid + "'");
							String notificationResult = DatabaseHandler
									.enterSelectStatement("SELECT notification FROM notifications WHERE userid = '"
											+ userid + "'");
							if (usersResult.equals("")) {
								DatabaseHandler.enterUpdateStatement("INSERT INTO Users VALUES ('" + userid + "', 0, 0, 0)");
								DatabaseHandler.enterUpdateStatement("INSERT INTO notifications VALUES ('" + userid +"', 'Welcome to SciencePlay! SciencePlay is an app that lets you download other apps, outside of Google Play. You can also participate or download various experiments. If you participate in an experiment, all future downloads will be instrumented as that experiment specifies. Finally, some applications can not download through this app. These will typically be large popular apps like Facebook. This is due to the project not being officially supported by Google. Thank you for using SciencePlay.'");
							}
							if (!databaseResult.equals("")) {
								String scriptName = "";
								StringTokenizer scriptNameTokens = new StringTokenizer(databaseResult);
								if (scriptNameTokens.hasMoreTokens()) {
									scriptNameTokens.nextToken();
								}
								if (scriptNameTokens.hasMoreTokens()) {
									scriptName = scriptNameTokens.nextToken();
								}
								String toReturn = "DownloadAvailable " + scriptName;
								out.write(toReturn.getBytes());
								
							} else if (!notificationResult.equals("")) {
								DatabaseHandler.enterUpdateStatement("DELETE FROM notifications WHERE userid = '" + userid + "'");
								out.write(("NotificationAvailable " + notificationResult).getBytes());
							} else {
								String toReturn = "DownloadNotAvailable";
								out.write(toReturn.getBytes());
							}
							out.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if (firstReplyToken.equals("GetAvailableDownload")) {
						String userid = "";
						if (replyTokenizer.hasMoreTokens()) {
							userid = replyTokenizer.nextToken();
						}
						String databaseResult;
						try {
							String usersResult = "";
							usersResult = DatabaseHandler
									.enterSelectStatement("SELECT * FROM Users WHERE userid = '"
											+ userid + "'");
							databaseResult = DatabaseHandler
									.enterSelectStatement("SELECT filename FROM waitingfiles WHERE userid = '"
											+ userid + "'");
							String[] databaseResultArray = databaseResult.split("\n");
							String firstResult = (databaseResultArray.length > 0) ? databaseResultArray[0] : "";
							if (new File(firstResult.trim()).exists()) {
								if (!usersResult.equals("")) {
									int downloadsNo = 0;
									int experimentsNo = 0;
									int questionsNo = 0;
									StringTokenizer usersTokens = new StringTokenizer(usersResult);
									if (usersTokens.hasMoreTokens()) {
										//skip userid 
										usersTokens.nextToken();
									}
									if (usersTokens.hasMoreTokens()) {
										downloadsNo = Integer.parseInt(usersTokens.nextToken());
									}
									if (usersTokens.hasMoreTokens()) {
										experimentsNo = Integer.parseInt(usersTokens.nextToken());
									}
									if (usersTokens.hasMoreTokens()) {
										questionsNo = Integer.parseInt(usersTokens.nextToken());
									}
									questionsNo++;
									DatabaseHandler
									.enterUpdateStatement("UPDATE Users SET questionsNo = " + questionsNo + " WHERE userid = '"
											+ userid + "'");
									
								}
								System.out.println("New file with name: "
										+ firstResult.trim() + " exists");
								File f = new File(firstResult.trim());
								actorThread.eraseStats();
								actorThread.setStats(f, 10, out, false);
								new Thread(actorThread).start();
							} else {
								Thread.sleep(2500);
								out.close();
							}
							DatabaseHandler
									.enterUpdateStatement("DELETE FROM waitingfiles WHERE userid = '"
											+ userid + "' AND filename = '" + firstResult.trim() + "'");
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				 	  else if (firstReplyToken.equals("ScientistInfo")) {
						String address = "";
						String fname = "";
						String lname = "";
						String expname = "";
						String scriptname = "";
						String expdesc = "";
						Boolean isIndependent = false;
						if (replyTokenizer.hasMoreTokens()) {
							address = replyTokenizer.nextToken();
						}
						if (replyTokenizer.hasMoreTokens()) {
							fname = replyTokenizer.nextToken();
						}
						if (replyTokenizer.hasMoreTokens()) {
							lname = replyTokenizer.nextToken();
						}
						if (replyTokenizer.hasMoreTokens()) {
							expname = replyTokenizer.nextToken();
						}
						if (replyTokenizer.hasMoreTokens()) {
							scriptname = replyTokenizer.nextToken();
						}
						while (replyTokenizer.hasMoreTokens()) {
							expdesc += replyTokenizer.nextToken() + " ";
						}
						expdesc = expdesc.replace("'", "");
						System.out.println("expdesc: " + expdesc);
						try {
							String results = DatabaseHandler
									.enterSelectStatement("SELECT address FROM Scientists WHERE address = '"
											+ address + "'");
							if (results.equals("") || results == null) {
								DatabaseHandler
										.enterUpdateStatement("INSERT INTO Scientists VALUES ('"
												+ address
												+ "', '"
												+ fname
												+ "', '"
												+ lname
												+ "', '"
												+ expname
												+ "', '"
												+ scriptname
												+ "', '"
												+ expdesc
												+ "', '" + isIndependent + "')");
							} else {
								DatabaseHandler
										.enterUpdateStatement("UPDATE Scientists SET firstname = '"
												+ fname
												+ "', lastname  = '"
												+ lname
												+ "', expname ='"
												+ expname
												+ "', expdesc= '"
												+ expdesc
												+ "', scriptname= '"
												+ scriptname
												+ "', isIndependent= '"
												+ isIndependent
												+ "' WHERE address = '"
												+ address + "'");

							}
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						out.close();
					} else if (firstReplyToken.equals("CheckQuestions")) {
						try {
							String results = DatabaseHandler
									.enterSelectStatement("SELECT * FROM Questionnaires");
							System.out.println("results: " + results);
							if (results.equals("")) {
								out.write("QuestionnairNotAvailable".getBytes());
							} else {
								String returnString = "QuestionnairAvailable ";
								String[] resultsArray = results.split("\n");
								for (int j = 0; j<resultsArray.length; j++) {
									StringTokenizer eachResult = new StringTokenizer(resultsArray[j]);
									while (eachResult.hasMoreTokens()) {
										returnString += eachResult.nextToken() + " ";
									}
									returnString += ">nextapp<";
								}
								System.out.println("returnString: " + returnString);
								out.write(returnString.getBytes());
							}
							
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();	
						}
						out.close();
					} else if (firstReplyToken.equals("Pong")) {
						// do nothing
					}
						else {
						//System.out.println("Other data received: " + message);
						actorThread.newPacket(event);
					}
				}

			}
		});
		in.onClose(new Callback0() {
			@Override
			public void invoke() throws Throwable {
				actorThread.setFinal(true);
				// cancellable.cancel();
			}
		});
	}

	public void sendToScientist(String experimentID, final String fileName,
			final String userid) {
		URI uri;
		final NewConnHandler scientistThread = new NewConnHandler();
		try {
			System.out.println("Websocket " + "trying to connect");
			uri = new URI("ws://" + experimentID);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}

		mWebSocketClient = new WebSocketClient(uri) {
			@Override
			public void onOpen(ServerHandshake serverHandshake) {

				String heyBytes = "Hey";
				byte[] heyinBytes = heyBytes.getBytes();
				mWebSocketClient.send(heyinBytes);
				System.out.println("WebSocket Connected!");
				String appNameNotif = "appname " + fileName;
				mWebSocketClient.send(("User " + userid).getBytes());
				mWebSocketClient.send(appNameNotif.getBytes());
				mWebSocketClient.send("receive".getBytes());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				File f = new File(fileName);
				scientistThread.setStats(f, 1000, mWebSocketClient);
				new Thread(scientistThread).start();
			}

			@Override
			public void onMessage(ByteBuffer bytes) {
				byte[] messageReceived = new byte[bytes.capacity()];
				bytes.get(messageReceived, 0, bytes.capacity());
				String message = new String(messageReceived);
			//	System.out.println("Received message: " + message);
				// Log.d("received byte message", message);
				StringTokenizer messageTok = new StringTokenizer(message);
				String firstToken = "";
				if (messageTok.hasMoreTokens()) {
					firstToken = messageTok.nextToken();
				}
				if (firstToken.equals("ack")) {
					int newAck = 0;
					if (messageTok.hasMoreTokens()) {
						newAck = Integer.parseInt(messageTok.nextToken());
						scientistThread.setAck(newAck);
					}
				}

			}

			@Override
			public void onMessage(String s) {
				final String message = s;

			}

			@Override
			public void onClose(int i, String s, boolean b) {
				System.out.println("WebSocketClient closed");
				scientistThread.setFinalReceiver(true);
			}

			@Override
			public void onError(Exception e) {

			}
		};
		mWebSocketClient.connect();
	}

	@Override
    public void onReceive(Object message) {
        if (message.equals("Tick")) {
        //	System.out.println("send ping");
            out.write("DownloadNotAvailable".getBytes());
        } else {
        	System.out.println("unhandled message " + message);
            unhandled(message);
        }
    }

}
