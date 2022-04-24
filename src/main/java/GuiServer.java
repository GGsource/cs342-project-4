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
import javafx.stage.Modality;
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
	int groupIndex = -1;
	
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
						iAm = gmData.name;
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
					if (gmData.isDMRequest) {
						//This user is being asked if they want to DM
						//Give an option screen
						prevScene = primaryStage.getScene();
						Stage confirmStage = createDMConfirmGui(primaryStage, gmData.userA, gmData.groupAssignment);
						confirmStage.initModality(Modality.APPLICATION_MODAL);
						confirmStage.initOwner(primaryStage);
						confirmStage.show();
						Coord pos = getCenterPoint(primaryStage, confirmStage);
						confirmStage.setX(pos.x);
						confirmStage.setY(pos.y);
						//TODO: Give this popup window a title
						//TODO: Give popup an icon
						//TODO: make popup unable to be maximized
						//TODO: make so closing the popup rejects invite
						
					}
					if (gmData.isGroupAssignment) {
						//Now we know what group dm we belong to
						groupIndex = gmData.groupAssignment;
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
		//TODO: Make this scene be a popup instead
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
				clientConnection.createGroup(new User(iAm));
				givenStage.setScene(createGroupDMGUI(givenStage));
				clientConnection.directMessage(iAm, chosenUser, groupIndex);
			}
		});
		return new Scene(dmSelectBox, 200, 200);
	}

	private Stage createDMConfirmGui(Stage givenStage, String requestingUser, int groupNum) {
		Label requesterLabel = new Label(requestingUser + " would like to Direct Message with you");
		requesterLabel.setWrapText(true);
		Button declineButton = new Button("Decline");
		Button acceptButton = new Button("Accept");
		HBox optionBox = new HBox(20, declineButton, acceptButton);
		optionBox.setAlignment(Pos.CENTER);
		VBox confirmBox = new VBox(10, requesterLabel, optionBox);
		confirmBox.setAlignment(Pos.CENTER);

		Scene confirmScene = new Scene(confirmBox, 300, 100);
		Stage cStage = new Stage();
		cStage.setScene(confirmScene);

		declineButton.setOnAction(e->{
			//givenStage.setScene(prevScene);
			cStage.close();
		});
		acceptButton.setOnAction(e->{
			//Save the group index to know where to send your messages
			groupIndex = groupNum;
			//User accepted request, close this popup
			cStage.close();
			//DONE: Add this user to the userGroup
			//clientConnection.sendGroup(requestingUser, iAm);
			clientConnection.joinGroup(new User(iAm), groupIndex);
			
			//DONE: Take this user to DM screen
			givenStage.setScene(createGroupDMGUI(givenStage));
			//On DM group screen, the send button will send a GuiModder(String msg, int groupIndex)
		});

		return cStage;
	}

	private Scene createGroupDMGUI(Stage givenStage) {
		Label groupLabel = new Label("Group Messaging");
		ListView<String> groupChatView = new ListView<>();
		TextField groupInputField = new TextField();
		groupInputField.setPromptText("Message Group");
		Button sendButton = new Button("Send");
		HBox inputBox = new HBox(10, groupInputField, sendButton);
		VBox groupLeftBox = new VBox(10, groupLabel, groupChatView, inputBox);
		Label participantsLabel = new Label("Participants: ");
		ListView<String> participantsView = new ListView<>();
		Button returnButton = new Button("Return");
		VBox groupRightBox = new VBox(10, participantsLabel, participantsView, returnButton);
		HBox groupBox = new HBox(10, groupLeftBox, groupRightBox);
		//Center everything
		inputBox.setAlignment(Pos.CENTER);
		groupLeftBox.setAlignment(Pos.CENTER);
		groupRightBox.setAlignment(Pos.CENTER);
		groupBox.setAlignment(Pos.CENTER);
		groupBox.setPadding(new Insets(10));
		//Adjust list sizes
		participantsView.setPrefWidth(70);

		//Return button
		returnButton.setOnAction(e->{
			//DONE: set scene back to clientGui
			givenStage.setScene(sceneMap.get("client"));
			//TODO: update group you are leaving's userlist
			clientConnection.leaveGroup(new User(iAm), groupIndex);
			//DONE: set group index to -1
			groupIndex = -1;
			//TODO: make sure rejoined client still has same name
		});
		//Send button
		sendButton.setOnAction(e->{
			//TODO: take text from inputfield and add to the listview
			//TODO: send message to the server to tell other participants to receive msg
		});
		//Send back the scene
		return new Scene(groupBox, 400, 300);
	}

	//Helper Function
	private Coord getCenterPoint(Stage parentStage, Stage childStage) {
		double startX = parentStage.getX();
		double lenX = parentStage.getWidth();
		double childWidth = childStage.getWidth();
		double x = (startX + Math.round(lenX/2.0)) - Math.round(childWidth/2.0);

		double startY = parentStage.getY();
		double lenY = parentStage.getHeight();
		double childHeight = childStage.getHeight();
		double y = (startY + Math.round(lenY/2.0)) - Math.round(childHeight/2.0);

		return new Coord(x, y);
	}
}