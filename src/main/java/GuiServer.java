//CS 342: Project 4 - Advanced Chat App
//ggonza55@uic.edu

//Entirely Custom UI, was fun to make

import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class GuiServer extends Application{
	ListView<String> clientDialogueView, serverDialogueView, clientUserList, serverUserList;
	//HashMap<String, Scene> sceneMap;
	Server serverConnection;
	Client clientConnection;
	ArrayList<String> userNameList = new ArrayList<>();
	String iAm;
	int groupIndex = -1;
	ListView<String> groupChatView;
	TextField groupInputField;
	String chosenUser;
	ArrayList<String> chosenUsers;
	ListView<String> participantsView;
	Label participantsLabel;
	boolean isIndividual = false;
	Lock lock;
	Label serverUsersCountLabel;
	Label clientUsersCountLabel;
	Scene serverWideChatScene;
	double xOffset;
	double yOffset;
	Label titleLabel;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// //Window title and icon
		String titleString = "Project 4: Advanced Chat App";
		primaryStage.setTitle(titleString);
		primaryStage.getIcons().add(new Image("/images/bubble.png"));
		//Custom title bar!
		BorderPane titleBar = customizeBar(primaryStage, titleString, 0);
		//Welcome Message
		Label introLabel = new Label("Welcome to the Advanced Chat App!");
		introLabel.getStyleClass().add("introLabel");
		Label descriptionLabel = new Label("Select Server or Client:");
		//Server Button
		Button serverChoice = new Button("Server");
		serverChoice.getStyleClass().add("serverButton");
		//Client Button
		Button clientChoice = new Button("Client");
		clientChoice.getStyleClass().add("clientButton");

		//When pressing the server button
		serverChoice.setOnAction(e->{
			primaryStage.setScene(createServerGui(primaryStage));
			serverConnection = new Server(data -> {
				Platform.runLater(()->{
					GuiModder gmData = (GuiModder)data;
					if (gmData.isMessage) {
						//It's a message, display in server
						serverDialogueView.getItems().add(gmData.msg);	
					}
					if (gmData.isUserListUpdate) {
						//It's an update to who has left or joined
						userNameList.clear();
						serverUserList.getItems().clear(); //Reset the list
						serverUsersCountLabel.setText("Users: " + gmData.set.size());
						for (String s : gmData.set) {
							userNameList.add(s);
						}
						userNameList.sort(null);
						serverUserList.getItems().addAll(userNameList);
					}
				});
			});
		});
		//When pressing the client button
		clientChoice.setOnAction(e-> {
			serverWideChatScene = createClientGui(primaryStage);
			primaryStage.setScene(serverWideChatScene);
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
							titleLabel.setText("You are " +iAm);
						}
						else if (gmData.isMessage) {
							//System.out.println("This client received a message!");
							clientDialogueView.getItems().add(gmData.msg);
						}
						else if (gmData.isUserListUpdate) {
							//System.out.println("Incoming user list update!");
							userNameList.clear(); //reset the set we have saved to refill
							clientUserList.getItems().clear();
							clientUsersCountLabel.setText("Users: " + gmData.set.size());
							for (String s : gmData.set) {
								userNameList.add(s);
							}
							//Sort the list of items just added
							userNameList.sort(null);
							clientUserList.getItems().addAll(userNameList);
						}
						else if (gmData.isDMRequest || gmData.isGroupRequest) {
							//This user is being asked if they want to join a group/dm, give popup to accept
							Stage confirmStage = createDMConfirmGui(primaryStage, gmData.userA, gmData.groupAssignment);
							//Make it so the client cannot use main stage until they deal w/ popup
							confirmStage.initModality(Modality.APPLICATION_MODAL);
							confirmStage.initOwner(primaryStage);
							confirmStage.show();
							//Center the popup on the client who needs to accept/decline
							Coord pos = getCenterPoint(primaryStage, confirmStage);
							confirmStage.setX(pos.x);
							confirmStage.setY(pos.y);
							//Give this popup window a title
							confirmStage.setTitle("Group DM Invite");
							//Give popup the same icon as client
							confirmStage.getIcons().add(new Image("/images/bubble_invite.png"));
							//make popup unable to be maximized
							confirmStage.setResizable(false);
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
					}
				});
			});
			clientConnection.start();
		});
		//To ensure closing the window ends the program
		primaryStage.setOnCloseRequest(close->{
			Platform.exit();
			System.exit(0);
        });

		//Set Start Scene and begin
		BorderPane buttonPane = new BorderPane();
		buttonPane.getStyleClass().add("buttonPane");
		buttonPane.setLeft(serverChoice);
		buttonPane.setRight(clientChoice);
		BorderPane.setAlignment(serverChoice, Pos.CENTER_LEFT);
		BorderPane.setAlignment(clientChoice, Pos.CENTER_RIGHT);
		VBox introBox = new VBox(titleBar, introLabel, descriptionLabel, buttonPane);
		introBox.getStyleClass().setAll("backgroundBox");
		Scene introScene = new Scene(introBox, 500, 500);
		introScene.getStylesheets().add("/styles/IntroStyle.css");
		introScene.setOnMouseClicked(e->{titleBar.requestFocus();}); //clicking on screen focuses back the window
		primaryStage.setScene(introScene);
		primaryStage.initStyle(StageStyle.TRANSPARENT);
		introScene.setFill(Color.TRANSPARENT);
		primaryStage.setResizable(false);
		titleBar.requestFocus(); //To avoid any button being selected by default :D
		primaryStage.show();
	}
	
	public Scene createServerGui(Stage givenStage) {
		String serverTitle = "This is the Server";
		givenStage.setTitle(serverTitle);
		givenStage.getIcons().setAll(new Image("/images/bubble_server.png"));

		serverDialogueView = new ListView<>();
		serverUserList =	 new ListView<>();
		serverUserList.setPrefWidth(70);
		//Make it so neither list can be focused on
		serverDialogueView.setFocusTraversable(false);
		serverUserList.setFocusTraversable(false);

		//Custom title bar!
		BorderPane titleBar = customizeBar(givenStage, serverTitle, 1);
		//Server Center Stage
		BorderPane serverPane = new BorderPane();
		serverPane.getStyleClass().add("centerPane");
		Label serverChatLabel = new Label("Server-wide Chat");
		serverUsersCountLabel = new Label("Users: 0");
		VBox centerBox = new VBox(serverChatLabel, serverDialogueView);
		centerBox.getStyleClass().add("vbox");
		serverPane.setCenter(centerBox);
		VBox rightBox = new VBox(serverUsersCountLabel, serverUserList);
		rightBox.getStyleClass().add("vbox");
		serverPane.setRight(rightBox);
		VBox serverBox = new VBox(titleBar, serverPane);
		serverBox.getStyleClass().add("serverBox");
		Scene serverScene = new Scene(serverBox, 500, 400);
		serverScene.getStylesheets().add("/styles/ServerStyle.css");
		serverScene.setFill(Color.TRANSPARENT);
		titleBar.requestFocus(); //To avoid any button being selected by default :D
		return serverScene;
	}
	
	public Scene createClientGui(Stage givenStage) {
		//TODO: Give client custom titlebar
		//TODO: make pressing enter in inputfield sends message
		givenStage.getIcons().clear();
		givenStage.getIcons().add(new Image("/images/bubble_client.png"));

		BorderPane titleBar = customizeBar(givenStage, "This is Client", 2);

		clientDialogueView = new ListView<>();
		clientDialogueView.setPrefWidth(395);
		clientDialogueView.setDisable(true);
		clientUserList =	 new ListView<>();
		clientUserList.setPrefWidth(70);
		clientUserList.setDisable(true);

		//give title of "Server-wide Chat"
		Label chatLabel = new Label("Server-wide Chat");
		clientUsersCountLabel = new Label("Users: 0");
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
			createGroupInvitePopout(givenStage);
		});
		Button groupButton = new Button("Group Message");
		groupButton.setOnAction(e->{
			isIndividual = false;
			createGroupInvitePopout(givenStage);
		});
		HBox msgButtonsBox = new HBox(20, sendButton, dmButton, groupButton);
		msgButtonsBox.setAlignment(Pos.CENTER);
		VBox chatBox = new VBox(10, chatLabel, clientDialogueView, clientInputField, msgButtonsBox);
		chatBox.getStyleClass().add("vbox");
		VBox usersBox = new VBox(clientUsersCountLabel, clientUserList);
		usersBox.getStyleClass().add("vbox");
		HBox centerBox = new HBox(10, chatBox, usersBox);
		centerBox.getStyleClass().add("centerPane");
		VBox clientBox = new VBox(titleBar, centerBox);
		clientBox.getStyleClass().add("clientBox");
		Scene clientScene = new Scene(clientBox, 550, 350);
		clientScene.getStylesheets().add("/styles/ClientStyle.css");
		clientScene.setFill(Color.TRANSPARENT);
		clientScene.setOnMouseClicked(e->{titleBar.requestFocus();});
		titleBar.requestFocus();
		return clientScene;
	}

	private Stage createGroupSelectionGui(Stage givenStage) {
		//BorderPane titleBar = customizeBar(givenStage, "Group Request", 3);
		
		Label groupTitleLabel = new Label("Select who you would like to message:");
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
		dmSelectBox.getStyleClass().add("centerPane");
		VBox requestBox = new VBox( dmSelectBox);
		requestBox.getStyleClass().add("requestBox");
		Scene requestScene = new Scene(requestBox, 200, 200);
		Stage requestStage = new Stage();
		requestStage.setScene(requestScene);
		requestStage.initStyle(StageStyle.TRANSPARENT);
		requestScene.setFill(Color.TRANSPARENT);
		requestScene.getStylesheets().add("/styles/RequestStyle.css");
		groupTitleLabel.requestFocus();

		//Cancel button action
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(e->{
			requestStage.close(); //Close the popup now
		});
		requestStage.setOnCloseRequest(e->requestStage.close());
		//Confirm button action
		confirmButton.setOnAction(e->{
			requestStage.close(); //Close the popup now
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
		return requestStage;
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
		confirmBox.getStyleClass().setAll("requestBox", "centerPane");

		Scene confirmScene = new Scene(confirmBox, 300, 100);
		Stage cStage = new Stage();
		cStage.setScene(confirmScene);
		cStage.initStyle(StageStyle.TRANSPARENT);

		confirmScene.getStylesheets().add("/styles/RequestStyle.css");
		confirmScene.setFill(Color.TRANSPARENT);
		requesterLabel.requestFocus();

		declineButton.setCancelButton(true);
		declineButton.setOnAction(e->{
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
		//TODO: color for group DM
		//TODO: maybe different color for 1 on 1 dm
		BorderPane titleBar = customizeBar(givenStage, titleLabel.getText(), 4);

		Label groupLabel = new Label("Group Chat");
		groupChatView = new ListView<>();
		groupChatView.setDisable(true);
		groupInputField = new TextField();
		groupInputField.setPromptText("Message Group");
		Button sendButton = new Button("Send");
		HBox inputBox = new HBox(10, groupInputField, sendButton);
		VBox groupLeftBox = new VBox(10, groupLabel, groupChatView, inputBox);
		participantsLabel = new Label("Participants: ");
		participantsView = new ListView<>();
		participantsView.setDisable(true);
		Button returnButton = new Button("Return");
		VBox groupRightBox = new VBox(10, participantsLabel, participantsView, returnButton);
		HBox centerBox = new HBox(10, groupLeftBox, groupRightBox);
		centerBox.getStyleClass().add("centerPane");
		VBox groupBox = new VBox(titleBar, centerBox);
		groupBox.getStyleClass().add("groupBox");
		//Center everything
		inputBox.setAlignment(Pos.CENTER);
		groupLeftBox.setAlignment(Pos.CENTER);
		groupRightBox.setAlignment(Pos.CENTER);
		//Adjust list sizes
		participantsView.setPrefWidth(70);

		//Return button
		returnButton.setOnAction(e->{
			//DONE: set scene back to clientGui
			givenStage.setScene(serverWideChatScene);
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
		Scene groupScene = new Scene(groupBox, 400, 300);
		groupScene.getStylesheets().add("/styles/GroupStyle.css");
		groupScene.setFill(Color.TRANSPARENT);
		groupScene.setOnMouseClicked(e->{titleBar.requestFocus();});
		titleBar.requestFocus();

		return groupScene;
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
	private void createGroupInvitePopout(Stage parentStage) {
		Stage dmStage = createGroupSelectionGui(parentStage);
		dmStage.initModality(Modality.APPLICATION_MODAL);
		dmStage.initOwner(parentStage);
		dmStage.show();
		Coord pos = getCenterPoint(parentStage, dmStage);
		dmStage.setX(pos.x);
		dmStage.setY(pos.y);
		dmStage.setTitle("Who to Invite?");
		//Give popup the same icon as client
		dmStage.getIcons().add(new Image("/images/bubble_invite.png"));
		//make popup unable to be maximized
		dmStage.setResizable(false);
	}

	private BorderPane customizeBar(Stage givenStage, String barTitle, int sceneType) {
		String icon = "bubble";
		switch (sceneType) {
			case 1: {//this is server scene
				icon += "_server";
				break;
			} case 2: {//this is client scene
				icon += "_client";
				break;
			} case 3: {//this is invite request/confirm scene
				icon += "_invite";
				break;
			} case 4: {//this is group scene
				icon += "_group";
				break;
			} default://this is neutral scene, no change needed
				break;
		}
		ImageView titleIcon = new ImageView(new Image("/images/"+icon+".png", 24, 24, true, true, true));
		titleLabel = new Label(barTitle);
		titleLabel.getStyleClass().setAll("titleLabel");
		Button minimizeButton = new Button("-");
		minimizeButton.getStyleClass().setAll("windowButton", "miniButton");
		minimizeButton.setOnAction(e->givenStage.setIconified(true));
		minimizeButton.setFocusTraversable(false);
		Button closeButton = new Button("✖");
		closeButton.getStyleClass().setAll("windowButton", "closeButton");
		closeButton.setOnAction(e->{Platform.exit(); System.exit(0);});
		closeButton.setFocusTraversable(false);
		HBox windowBox = new HBox(minimizeButton, closeButton);
		BorderPane titleBar = new BorderPane();
		titleBar.getStyleClass().setAll("titleBar");
		titleBar.setLeft(titleIcon);
		titleBar.setCenter(titleLabel);
		titleBar.setRight(windowBox);
		BorderPane.setAlignment(titleIcon, Pos.CENTER);
		BorderPane.setMargin(titleIcon, new Insets(0, 40, 0, 5));

		titleBar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
			givenStage.setX(event.getScreenX() - xOffset);
			givenStage.setY(event.getScreenY() - yOffset);
        });

		return titleBar;
	}
}