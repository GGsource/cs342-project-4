import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GuiModder implements Serializable {
    public boolean isMessage = false;
    public boolean isUserListUpdate = false;
    public boolean isReminder = false;
    public boolean isDMRequest = false;
    public boolean isCreatingGroup = false;
    public boolean isJoiningGroup = false;
    public boolean isLeavingGroup = false;
    public boolean isRequestingGroupAssignment = false;
    public boolean isGroupAssignment = false;
    public boolean isGroupMessage = false;
    public boolean isGroupListUpdate = false;
    public boolean isGroupRequest = false;
    public boolean isLock = false;
    
    public String msg;
    public HashSet<String> set;
    public String name;
    String userA;
    String userB;
    User seeder;
    User participant;
    int groupAssignment;
    ArrayList<String> groupUsers;
    Lock serverLock;


    GuiModder (Lock l) {
        isLock = true;
        serverLock = l;
    }
    GuiModder(String message) {
        isMessage = true;
        msg = message;
    }
    GuiModder(Set<String> s, boolean isServerWideUpdate) {
        if (isServerWideUpdate)
            isUserListUpdate = true;
        else
            isGroupListUpdate = true;
        set = new HashSet<>();
        for (String str : s) {
            set.add(str);
        }
    }
    GuiModder(int groupNum, String givenName) {
        if (groupNum == -1){ 
            isReminder = true;
            name = givenName;
        }
    }

    GuiModder(Group g) {
        isGroupAssignment = true;
        groupAssignment = g.ndx;
    }

    GuiModder(String requestingUser, String receivingUser, int groupNum) {
        isDMRequest = true;
        userA = requestingUser;
        userB = receivingUser;
        groupAssignment = groupNum;
    }

    GuiModder(String requestingUser, ArrayList<String> receivingUsers, int groupNum) {
        isGroupRequest = true;
        userA = requestingUser;
        groupUsers = new ArrayList<>();
        for (String s : receivingUsers) {
            groupUsers.add(s);
        }
        groupAssignment = groupNum;
    }

    GuiModder(User usr) {
        isCreatingGroup = true;
        seeder = usr;
    }

    GuiModder(User usr, int GroupNum, boolean isJoining) {
        participant = usr;
        groupAssignment = GroupNum;
        if (isJoining) {
            isJoiningGroup = true;
        }
        else {
            isLeavingGroup = true;
        }
    }

    GuiModder(String message, int groupNdx) {
        isGroupMessage = true;
        msg = message;
        groupAssignment = groupNdx;
    }
}
