import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application{
	ListView<String> clientDialogueView, serverDialogueView, clientUserList, serverUserList;
	HashMap<String, Scene> sceneMap;
	Server serverConnection;
	Client clientConnection;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Project 4 Advanced Chat");
		primaryStage.getIcons().add(new Image("/images/chat.png"));
		
		//Server Button
		Button serverChoice = new Button("Server");
		serverChoice.setStyle("-fx-pref-width: 300px");
		serverChoice.setStyle("-fx-pref-height: 100px");
		//Client Button
		Button clientChoice = new Button("Client");
		clientChoice.setStyle("-fx-pref-width: 300px");
		clientChoice.setStyle("-fx-pref-height: 100px");
		
		HBox buttonBox = new HBox(100, serverChoice, clientChoice);
		buttonBox.setAlignment(Pos.CENTER);
		
		clientDialogueView = new ListView<>();
		serverDialogueView = new ListView<>();
		clientUserList =	 new ListView<>();
		serverUserList =	 new ListView<>();
		clientDialogueView.setPrefWidth(430);
		clientUserList.setPrefWidth(70);
		serverUserList.setPrefWidth(70);

		//Save Scenes
		sceneMap = new HashMap<String, Scene>();
		sceneMap.put("server",  createServerGui());
		sceneMap.put("client",  createClientGui());

		//TODO: make clicking a user on the userlist starts communication with them

		//When pressing the server button
		serverChoice.setOnAction(e->{
			primaryStage.setScene(sceneMap.get("server"));
			primaryStage.setTitle("This is the Server");
			primaryStage.getIcons().clear();
			primaryStage.getIcons().add(new Image("/images/chat_server.png"));
				serverConnection = new Server(data -> {
					Platform.runLater(()->{
						GuiModder gmData = (GuiModder)data;
						if (gmData.isMessage) {
							//It's a message, display in server
							serverDialogueView.getItems().add(gmData.msg);	
						}
						if (gmData.isUserUpdate) {
							//It's an update to who has left or joined
							//System.out.println("isUserUpdate was true! adding:" + gmData.clients);
							serverUserList.getItems().clear(); //Reset the list
							serverUserList.getItems().add("Users: " + gmData.set.size());
							for (String s : gmData.set) {
								serverUserList.getItems().add(s);
								//System.out.println(s);
							}
						}
					});
				});
		});
		//When pressing the client button
		clientChoice.setOnAction(e-> {
			primaryStage.setScene(sceneMap.get("client"));
			primaryStage.setTitle("This is a client");
			primaryStage.getIcons().clear();
			primaryStage.getIcons().add(new Image("/images/chat_client.png"));
			clientConnection = new Client(data->{
				Platform.runLater(()->{
					GuiModder gmData = (GuiModder)data;
					if (gmData.isMessage) {
						//System.out.println("This client received a message!");
						clientDialogueView.getItems().add(gmData.msg);
					}
					if (gmData.isUserUpdate) {
						//System.out.println("Incoming user list update!");
						clientUserList.getItems().clear();
						clientUserList.getItems().add("Users: " + gmData.set.size());
						for (String s : gmData.set) {
							clientUserList.getItems().add(s);
							//System.out.println(s);
						}
					}
				});
			});
			clientConnection.start();
		});
		//To ensure closing the window ends the program
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

		//Set Start Scene and begin
		BorderPane startPane = new BorderPane();
		startPane.setPadding(new Insets(65));
		startPane.setCenter(buttonBox);
		Scene startScene = new Scene(startPane);
		primaryStage.setScene(startScene);
		primaryStage.show();
	}
	
	public Scene createServerGui() {
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(70));
		pane.setStyle("-fx-background-color: gold");
		pane.setCenter(serverDialogueView);
		pane.setRight(serverUserList);
		return new Scene(pane, 500, 400);
	}
	
	public Scene createClientGui() {
		TextField c1 = new TextField();
		Button b1 = new Button("Send");
		b1.setOnAction(e->{
			clientConnection.send(c1.getText());
			c1.clear();
		});
		VBox chatBox = new VBox(10, c1,b1,clientDialogueView);
		VBox usersBox = new VBox(clientUserList);
		chatBox.setStyle("-fx-background-color: blue");
		HBox clientBox = new HBox(chatBox, usersBox);
		return new Scene(clientBox, 500, 300);
	}

}
