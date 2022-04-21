import java.util.ArrayList;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
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
	ArrayList<String> userNameList = new ArrayList<>();
	String iAm;
	Scene prevScene;
	
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
		clientDialogueView.setPrefWidth(425);
		clientUserList.setPrefWidth(70);
		serverUserList.setPrefWidth(70);

		//Save Scenes
		sceneMap = new HashMap<String, Scene>();
		sceneMap.put("server",  createServerGui());
		sceneMap.put("client",  createClientGui(primaryStage));

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
							userNameList.clear();
							serverUserList.getItems().clear(); //Reset the list
							serverUserList.getItems().add("Users: " + gmData.set.size());
							for (String s : gmData.set) {
								userNameList.add(s);
								//System.out.println(s);
							}
							userNameList.sort(null);
							serverUserList.getItems().addAll(userNameList);
						}
					});
				});
		});
		//When pressing the client button
		clientChoice.setOnAction(e-> {
			primaryStage.setScene(sceneMap.get("client"));
			primaryStage.getIcons().clear();
			primaryStage.getIcons().add(new Image("/images/chat_client.png"));
			clientConnection = new Client(data->{
				Platform.runLater(()->{
					GuiModder gmData = (GuiModder)data;
					if (gmData.isReminder) {
						iAm = "Client #" + gmData.reminder;
						primaryStage.setTitle("You are " + iAm);
					}
					if (gmData.isMessage) {
						//System.out.println("This client received a message!");
						clientDialogueView.getItems().add(gmData.msg);
					}
					if (gmData.isUserUpdate) {
						//System.out.println("Incoming user list update!");
						userNameList.clear(); //reset the set we have saved to refill
						clientUserList.getItems().clear();
						clientUserList.getItems().add("Users: " + gmData.set.size());
						for (String s : gmData.set) {
							userNameList.add(s);
							//System.out.println(s);
						}
						//Sort the list of items just added
						userNameList.sort(null);
						clientUserList.getItems().addAll(userNameList);
					}
					if (gmData.isDMReceiver) {
						//This user is being asked if they want to DM
						//Give an option screen
						prevScene = primaryStage.getScene();
						primaryStage.setScene(createDMConfirmGui(primaryStage, gmData.userA));
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
	
	public Scene createClientGui(Stage givenStage) {
		TextField clientInputField = new TextField();
		clientInputField.setPromptText("Message to server");
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e->{
			clientConnection.send(clientInputField.getText());
			clientInputField.clear();
		});
		Button dmButton = new Button("Direct Message");
		dmButton.setOnAction(e->{
			givenStage.setScene(createDMSelectionGui(givenStage));
		});
		Button groupButton = new Button("Group Message");
		HBox msgButtonsBox = new HBox(sendButton, dmButton, groupButton);
		VBox chatBox = new VBox(10, clientDialogueView, clientInputField, msgButtonsBox);
		VBox usersBox = new VBox(clientUserList);
		chatBox.setStyle("-fx-background-color: blue");
		HBox clientBox = new HBox(10, chatBox, usersBox);
		clientBox.setStyle("-fx-background-color: blue");
		return new Scene(clientBox, 500, 300);
	}

	private Scene createDMSelectionGui(Stage givenStage) {
		Label dmTitleLabel = new Label("Select who you would like to direct message");
		dmTitleLabel.setWrapText(true);
		VBox usersBox = new VBox();
		//DONE: radial list of user names from userNamesSet
		ToggleGroup tg = new ToggleGroup();
		for (String user : userNameList) {
			// System.out.println("User: " + user);
			if (!user.contains(iAm)) { //Only can dm people who are not yourself!
				// System.out.println("iam is \""+iAm+"\"");
				// System.out.println("user is \""+user+"\"");
				RadioButton r = new RadioButton(user);
				usersBox.getChildren().add(r);
				r.setToggleGroup(tg);
			}
		}
		Button cancelButton = new Button("Cancel");
		Button confirmButton = new Button("Confirm");
		HBox confirmationBox = new HBox(10, cancelButton, confirmButton);
		confirmationBox.setAlignment(Pos.CENTER);
		VBox dmSelectBox = new VBox(10, dmTitleLabel, usersBox, confirmationBox);
		dmSelectBox.setAlignment(Pos.CENTER);

		//Cancel button action
		cancelButton.setOnAction(e->{
			givenStage.setScene(sceneMap.get("client"));
		});
		//Confirm button action
		confirmButton.setOnAction(e->{
			RadioButton chosenButton = (RadioButton)tg.getSelectedToggle();
			if (chosenButton != null) { //In case the user selected nothing
				String chosenUser = chosenButton.getText();
				clientConnection.directMessage(iAm, chosenUser);
				givenStage.setScene(createDMWaitingGui(chosenUser));
			}
		});
		return new Scene(dmSelectBox, 200, 200);
	}

	private Scene createDMConfirmGui(Stage givenStage, String requestingUser) {
		Label requesterLabel = new Label(requestingUser + " would like to Direct Message with you");
		requesterLabel.setWrapText(true);
		Button declineButton = new Button("Decline");
		Button acceptButton = new Button("Accept");
		HBox optionBox = new HBox(20, declineButton, acceptButton);
		optionBox.setAlignment(Pos.CENTER);
		VBox confirmBox = new VBox(10, requesterLabel, optionBox);
		confirmBox.setAlignment(Pos.CENTER);

		declineButton.setOnAction(e->{
			givenStage.setScene(prevScene);
		});
		acceptButton.setOnAction(e->{
			//User accepted request
			//TODO: Take this user to DM screen
			//TODO: Take user who requested to DM screen
		});

		return new Scene(confirmBox, 300, 100);
	}
	private Scene createDMWaitingGui(String requestedUser) {
		Label requestingLabel = new Label("Waiting for "+ requestedUser +" to accept your Direct Message request");
		requestingLabel.setWrapText(true);
		return new Scene(requestingLabel, 300, 100);
	}

	private Scene createGroupSelectionGui() {
		//TODO: Implement group DM
		Label groupTitleLabel = new Label();
		return null;
	}
}