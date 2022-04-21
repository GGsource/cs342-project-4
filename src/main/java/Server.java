import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.function.Consumer;

public class Server {

	int count = 1;	
	//DONE: Change clients to hashmap with client, count
	HashMap<String, ClientThread> cl = new HashMap<>();
	TheServer server;
	private Consumer<Serializable> callback;
	
	
	Server(Consumer<Serializable> call){
	
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
						cl.put("Client #"+count, c);
						callback.accept(new GuiModder("client has connected to server: " + "client #" + count));
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
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;	
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
				
				updateClients("new client has connected: client #"+count);
				updateClientsList();
				remindClient(this);
					
				 while(true) {
					    try {
					    	GuiModder gmIn = (GuiModder)in.readObject();
							if (gmIn.isMessage) {
								callback.accept(new GuiModder("client #" + count + " sent: " + gmIn.msg));
								updateClients("client #"+count+" said: "+gmIn.msg);
							}
							else if (gmIn.isDMRequest) {
								//Someone wants a DM conversation
								deliverDirectMessageRequest(gmIn.userA, gmIn.userB);
							}
					    	
					    	}
					    catch(Exception e) {
							cl.remove("Client #"+this.count);
					    	callback.accept(new GuiModder("Client #" + count + " disconnected!"));
							updateClients("Client #"+count+" has left the server!");
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
				c.out.writeObject(new GuiModder(c.count));
			}
			catch (IOException e) {
				System.out.println("Failed to remind the client who they are :(");
				e.printStackTrace();
			}
		}

		private void deliverDirectMessageRequest(String userRequesting , String userToRequest) {
			//ClientThread clientA = cl.get(gmIn.userA);
			ClientThread clientB = cl.get(userToRequest);
			try {
				clientB.out.writeObject(new GuiModder(false, userRequesting, userToRequest));
			}
			catch (IOException e) {
				System.out.println("Failed to notify user B that user A wanted to direct message them...");
				e.printStackTrace();
			}
		}
}