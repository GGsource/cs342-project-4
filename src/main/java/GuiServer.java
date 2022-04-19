import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application{
	ListView<String> clientDialogueView, serverDialogueView, userList;
	HashMap<String, Scene> sceneMap;
	Server serverConnection;
	Client clientConnection;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("The Networked Client/Server GUI Example");
		
		//Server Button
		Button serverChoice = new Button("Server");
		serverChoice.setStyle("-fx-pref-width: 300px");
		serverChoice.setStyle("-fx-pref-height: 300px");
		//Client Button
		Button clientChoice = new Button("Client");
		clientChoice.setStyle("-fx-pref-width: 300px");
		clientChoice.setStyle("-fx-pref-height: 300px");
		
		HBox buttonBox = new HBox(400, serverChoice, clientChoice);
		
		clientDialogueView = new ListView<>();
		serverDialogueView = new ListView<>();
		userList =			 new ListView<>();

		//Save Scenes
		sceneMap = new HashMap<String, Scene>();
		sceneMap.put("server",  createServerGui());
		sceneMap.put("client",  createClientGui());

		//When pressing the server button
		serverChoice.setOnAction(e->{
			primaryStage.setScene(sceneMap.get("server"));
			primaryStage.setTitle("This is the Server");
				serverConnection = new Server(data -> {
					Platform.runLater(()->{
						clientDialogueView.getItems().add(data.toString());
					});
				});
		});
		//When pressing the client button
		clientChoice.setOnAction(e-> {
			primaryStage.setScene(sceneMap.get("client"));
			primaryStage.setTitle("This is a client");
			clientConnection = new Client(data->{
				Platform.runLater(()->{
					serverDialogueView.getItems().add(data.toString());
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
		startPane.setPadding(new Insets(70));
		startPane.setCenter(buttonBox);
		Scene startScene = new Scene(startPane, 800,800);
		primaryStage.setScene(startScene);
		primaryStage.show();
	}
	
	public Scene createServerGui() {
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(70));
		pane.setStyle("-fx-background-color: coral");
		pane.setCenter(clientDialogueView);
		return new Scene(pane, 500, 400);
	}
	
	public Scene createClientGui() {
		TextField c1 = new TextField();
		Button b1 = new Button("Send");
		b1.setOnAction(e->{
			clientConnection.send(c1.getText());
			c1.clear();
		});
		VBox chatBox = new VBox(10, c1,b1,serverDialogueView);
		VBox usersBox = new VBox(userList);
		chatBox.setStyle("-fx-background-color: blue");
		HBox clientBox = new HBox(chatBox, usersBox);
		return new Scene(clientBox, 500, 300);
	}

}
