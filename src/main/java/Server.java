import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

public class Server {

	int count = 1;	
	//DONE: Change clients to hashmap with client, count
	HashMap<String, ClientThread> cl = new HashMap<>();
	TheServer server;
	private Consumer<Serializable> callback;
	//Create list of group DMs going on, with each group DM being a list of users
	ArrayList<HashMap<String, ClientThread>> groupList = new ArrayList<>();
	
	Server(Consumer<Serializable> call) {
	
		callback = call;
		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
			public void run() {
			
				try(ServerSocket mysocket = new ServerSocket(5555);) {
					System.out.println("Server is waiting for a client!");
					callback.accept(new GuiModder("Server has opened!"));
					callback.accept(new GuiModder(cl.keySet()));
					while(true) {
						ClientThread c = new ClientThread(mysocket.accept(), count);
						cl.put(c.name, c);
						callback.accept(new GuiModder("client has connected to server: " + c.name));
						c.start();
						
						count++;
					}
				}//end of try
				catch(Exception e) {
					System.out.println("Server socket did not launch");
					e.printStackTrace();
				}
			}//end of while
		}
	

		class ClientThread extends Thread {
			
		
			Socket connection;
			String name;
			ObjectInputStream in;
			ObjectOutputStream out;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.name = "Client #" + count;	
			}
			
			public void updateClients(String message) {
				for(ClientThread c : cl.values()) {
					try {
					 c.out.writeObject(new GuiModder(message));
					 //System.out.println("Successfully messaged clients: " + message);
					}
					catch(Exception e) {
						System.out.println("Failed to message the clients...");
						e.printStackTrace();
					}
				}
			}
			
			public void run(){
					
				try {
					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);	
				}
				catch(Exception e) {
					System.out.println("Streams not open");
					e.printStackTrace();
				}
				
				updateClients("new client has connected: "+name);
				updateClientsList();
				remindClient(this);
					
				 while(true) {
					    try {
					    	GuiModder gmIn = (GuiModder)in.readObject();
							if (gmIn.isMessage) {
								callback.accept(new GuiModder(name + " sent: " + gmIn.msg));
								updateClients(name+" said: "+gmIn.msg);
							}
							else if (gmIn.isDMRequest) {
								//Someone wants a DM conversation
								deliverDirectMessageRequest(gmIn.userA, gmIn.userB, gmIn.groupAssignment);
							}
							else if (gmIn.isCreatingGroup) {
								//Initialize a new group to chat separately
								HashMap<String, ClientThread> clientGroup = new HashMap<>();
								ClientThread c = cl.remove(gmIn.seeder.name);
								clientGroup.put(c.name, c);
								//Now add this group to our list of groups
								groupList.add(clientGroup);
								//Notify creator of what group index they have
								updateGroupIndex(c);
								updateClientsList(); //Show theyre no longer available
							}
							else if (gmIn.isJoiningGroup) {
								//A user accepted invite to group DM
								ClientThread c = cl.remove(gmIn.participant.name);
								groupList.get(gmIn.groupAssignment).put(c.name, c);
								updateClientsList(); //Show theyre no longer available
							}
							else if (gmIn.isLeavingGroup) {
								//A user has chosen to return to serverwide chat
								ClientThread c = groupList.get(gmIn.groupAssignment).remove(gmIn.participant.name);
								cl.put(c.name, c);
								updateClientsList();
							}
					    	
						}
					    catch(Exception e) {
							cl.remove(name);
					    	callback.accept(new GuiModder(name+" disconnected!"));
							updateClients(name+" has left the server!");
							updateClientsList();
					    	break;
					    }
					}
				}//end of run
			
			
		}//end of client thread

		public void updateClientsList() {
			callback.accept(new GuiModder(cl.keySet()));
			for (ClientThread c: cl.values()) {
				try {
					c.out.writeObject(new GuiModder(cl.keySet()));
				}
				catch (IOException e) {
					System.out.println("Failed to give a client an updated user list");
					e.printStackTrace();
				}
			}
		}

		private void remindClient(ClientThread c) {
			try {
				c.out.writeObject(new GuiModder(-1, c.name));
			}
			catch (IOException e) {
				System.out.println("Failed to remind the client who they are :(");
				e.printStackTrace();
			}
		}

		private void deliverDirectMessageRequest(String userRequesting , String userToRequest, int groupNum) {
			ClientThread clientB = cl.get(userToRequest);
			try {
				clientB.out.writeObject(new GuiModder(userRequesting, userToRequest, groupNum));
			}
			catch (IOException e) {
				System.out.println("Failed to notify user B that user A wanted to direct message them...");
				e.printStackTrace();
			}
		}

		private void updateGroupIndex(ClientThread creator) {
			try {
				creator.out.writeObject(new GuiModder(new Group(groupList.size())));
			}
			catch (IOException e) {
				System.out.println("Failed to notify group creator of their new group's index...");
				e.printStackTrace();
			}
		}
}