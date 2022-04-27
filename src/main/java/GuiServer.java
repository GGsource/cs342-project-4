import java.util.ArrayList;
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
	ListView<String> groupChatView;
	TextField groupInputField;
	String chosenUser;
	ArrayList<String> chosenUsers;
	ListView<String> participantsView;
	Label participantsLabel;
	boolean isIndividual = false;
	Lock lock;
	
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
		clientDialogueView.setPrefWidth(395);
		clientUserList.setPrefWidth(70);
		serverUserList.setPrefWidth(70);

		//Save Scenes
		sceneMap = new HashMap<String, Scene>();
		sceneMap.put("server",  createServerGui());
		sceneMap.put("client",  createClientGui(primaryStage));

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
						if (gmData.isUserListUpdate) {
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
					if (gmData.isLock) {
						lock = gmData.serverLock; //Server has sent its lock
					}
					synchronized (lock) {
						if (gmData.isReminder) {
							iAm = gmData.name;
							primaryStage.setTitle("You are " + iAm);
						}
						else if (gmData.isMessage) {
							//System.out.println("This client received a message!");
							clientDialogueView.getItems().add(gmData.msg);
						}
						else if (gmData.isUserListUpdate) {
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
						else if (gmData.isDMRequest) {
							//This user is being asked if they want to DM
							//Give an option screen
							//FIXME: is prevScene still needed
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
						else if (gmData.isGroupAssignment) {
							//System.out.println("groupIndex was just set to "+gmData.groupAssignment);
							//Now we know what group dm we belong to
							groupIndex = gmData.groupAssignment;
							// System.out.println("this client just created a groupDM, groupIndex is now: "+groupIndex);
							if (isIndividual)
								clientConnection.directMessage(iAm, chosenUser, groupIndex);
							else
								clientConnection.groupMessage(iAm, chosenUsers, groupIndex);
						}
						else if (gmData.isGroupMessage) {
							groupChatView.getItems().add(gmData.msg);
						}
						else if (gmData.isGroupListUpdate) {
							userNameList.clear(); //reset the set we have saved to refill
							participantsView.getItems().clear();
							participantsLabel.setText("Participants: " + gmData.set.size());
							for (String s : gmData.set) {
								userNameList.add(s);
							}
							//Sort the list of items just added
							userNameList.sort(null);
							participantsView.getItems().addAll(userNameList);
						}
						else if (gmData.isGroupRequest) {
							//This user is being asked if they want to join the group chat
							//Give an option screen
							//FIXME: is prevScene still needed?
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
		//TODO: give window title of "Server-wide Chat"
		//TODO: move users: x to be outside the listview as a new label next to title
		TextField clientInputField = new TextField();
		clientInputField.setPromptText("Message to server");
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e->{
			clientConnection.send(clientInputField.getText());
			clientInputField.clear();
		});
		Button dmButton = new Button("Direct Message");
		dmButton.setOnAction(e->{
			isIndividual = true;
			Stage dmStage = createGroupSelectionGui(givenStage);
			dmStage.initModality(Modality.APPLICATION_MODAL);
			dmStage.initOwner(givenStage);
			dmStage.show();
			Coord pos = getCenterPoint(givenStage, dmStage);
			dmStage.setX(pos.x);
			dmStage.setY(pos.y);
		});
		Button groupButton = new Button("Group Message");
		groupButton.setOnAction(e->{
			isIndividual = false;
			Stage groupStage = createGroupSelectionGui(givenStage);
			groupStage.initModality(Modality.APPLICATION_MODAL);
			groupStage.initOwner(givenStage);
			groupStage.show();
			Coord pos = getCenterPoint(givenStage, groupStage);
			groupStage.setX(pos.x);
			groupStage.setY(pos.y);
		});
		HBox msgButtonsBox = new HBox(20, sendButton, dmButton, groupButton);
		msgButtonsBox.setAlignment(Pos.CENTER);
		VBox chatBox = new VBox(10, clientDialogueView, clientInputField, msgButtonsBox);
		VBox usersBox = new VBox(clientUserList);
		chatBox.setStyle("-fx-background-color: blue");
		HBox clientBox = new HBox(10, chatBox, usersBox);
		clientBox.setStyle("-fx-background-color: blue");
		clientBox.setPadding(new Insets(10));
		return new Scene(clientBox, 500, 300);
	}

	private Stage createGroupSelectionGui(Stage givenStage) {
		Label groupTitleLabel = new Label("Select who you would like to message");
		groupTitleLabel.setWrapText(true);
		VBox usersBox = new VBox();
		ToggleGroup tg = new ToggleGroup();
		if (isIndividual) {
			//DONE: radial list of user names from userNamesSet
			for (String user : userNameList) {
				if (!user.contains(iAm)) { //Only can dm people who are not yourself!
					RadioButton r = new RadioButton(user);
					usersBox.getChildren().add(r);
					r.setToggleGroup(tg);
				}
			}
		}
		else { //Its a group DM, not 1 on 1
			//DONE: Create checkbox of users currently available into usersBox
			for (String user: userNameList) {
				if (!user.contains(iAm)) {
					CheckBox chk = new CheckBox(user);
					usersBox.getChildren().add(chk);
				}
			}
		}
		Button cancelButton = new Button("Cancel");
		Button confirmButton = new Button("Confirm");
		HBox confirmationBox = new HBox(10, cancelButton, confirmButton);
		confirmationBox.setAlignment(Pos.CENTER);
		VBox dmSelectBox = new VBox(10, groupTitleLabel, usersBox, confirmationBox);
		dmSelectBox.setAlignment(Pos.CENTER);

		Scene returnScene = new Scene(dmSelectBox, 200, 200);
		Stage rStage = new Stage();
		rStage.setScene(returnScene);

		//Cancel button action
		cancelButton.setOnAction(e->{
			rStage.close(); //Close the popup now
			//givenStage.setScene(sceneMap.get("client"));
		});
		//Confirm button action
		confirmButton.setOnAction(e->{
			rStage.close(); //Close the popup now
			if (isIndividual) {
				RadioButton chosenButton = (RadioButton)tg.getSelectedToggle();
				if (chosenButton != null) { //In case the user selected nothing
					chosenUser = chosenButton.getText();
					//System.out.println("this client is about to create a groupDM, groupIndex is: "+groupIndex);
				}
			}
			else {
				chosenUsers = new ArrayList<>();
				for (Node n : usersBox.getChildren()) {
					CheckBox c = (CheckBox)n;
					if (c.isSelected()) {
						//add it to a list of users we will add to our group convo
						chosenUser = c.getText();
						// System.out.println("chosenUser has: "+chosenUser);
						chosenUsers.add(chosenUser);
					} 
				}
				//chosenUsers now contains everyone who will be asked to join the group
			}
			clientConnection.createGroup(new User(iAm));
			givenStage.setScene(createGroupDMGUI(givenStage));
		});
		return rStage;
	}

	private Stage createDMConfirmGui(Stage givenStage, String requestingUser, int groupNum) {
		Label requesterLabel = new Label(requestingUser + " would like to Message with you");
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
			clientConnection.joinGroup(new User(iAm), groupIndex);
			//DONE: Take this user to DM screen
			givenStage.setScene(createGroupDMGUI(givenStage));
			//On DM group screen, the send button will send a GuiModder(String msg, int groupIndex)
		});
		return cStage;
	}

	private Scene createGroupDMGUI(Stage givenStage) {
		Label groupLabel = new Label("Group Chat");
		groupChatView = new ListView<>();
		groupInputField = new TextField();
		groupInputField.setPromptText("Message Group");
		Button sendButton = new Button("Send");
		HBox inputBox = new HBox(10, groupInputField, sendButton);
		VBox groupLeftBox = new VBox(10, groupLabel, groupChatView, inputBox);
		participantsLabel = new Label("Participants: ");
		participantsView = new ListView<>();
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
			//update group you are leaving's userlist as well as the serverwide chat's to know you're rejoining
			clientConnection.leaveGroup(new User(iAm), groupIndex);
			//DONE: set group index back to -1 since we've left
			groupIndex = -1;
		});
		//Send button
		sendButton.setOnAction(e->{
			//DONE: take text from inputfield and send it to all the clients to display
			clientConnection.groupSend(groupInputField.getText(), groupIndex);
			groupInputField.clear();
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

	//TODO: Make clients rotate between color themes for icons and backgrounds? maybe...
}