import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;

import javafx.application.Platform;

public class Client extends Thread{
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private Consumer<Serializable> callback;
	
	Client(Consumer<Serializable> call){
		callback = call;
	}
	
	public void run() {
		try {
		socketClient= new Socket("127.0.0.1",5555);
	    out = new ObjectOutputStream(socketClient.getOutputStream());
	    in = new ObjectInputStream(socketClient.getInputStream());
	    socketClient.setTcpNoDelay(true);
		}
		catch(Exception e) {
			System.out.println("Client socket failed to connect... Server may not be up yet...");
		}
		
		while(true) { 
			try {
				GuiModder incoming = (GuiModder) in.readObject();
				// if (incoming.isMessage) {System.out.println("Incoming is a message: " + incoming.msg);}
				// if (incoming.isUserUpdate) {System.out.println("Incoming is a userupdate...");}
				callback.accept(incoming);
			}
			catch (Exception e) {
				System.out.println("I have disconnected from the server.");
				//e.printStackTrace();
				Platform.exit();
                System.exit(0);
			}
		}
	
    }
	
	public void send(String data) {
		try {
			out.writeObject(new GuiModder(data));
		}
		catch (IOException e) {
			System.out.println("Failed to send message to the server...");
			e.printStackTrace();
		}
	}

	public void directMessage(String userA, String userB, int groupNum) {
		//User A has requested a DM chat with userB
		try {
			out.writeObject(new GuiModder(userA, userB, groupNum));
		}
		catch (IOException e) {
			System.out.println("Failed to request  direct message...");
			e.printStackTrace();
		}
	}
	public void createGroup (User usr) {
		try {
			out.writeObject(new GuiModder(usr));
		}
		catch (IOException e) {
			System.out.println("Failed to to tell server to create new DM group...");
			e.printStackTrace();
		}
	}
	public void addToGroup(User usr, int groupNum) {
		try {
			out.writeObject(new GuiModder(usr, groupNum));
		}
		catch (IOException e) {
			System.out.println("Failed to to tell server to add participant to DM group...");
			e.printStackTrace();
		}
	}
}
