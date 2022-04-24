import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class GuiModder implements Serializable {
    public boolean isMessage = false;
    public boolean isUserUpdate = false;
    public boolean isReminder = false;
    public boolean isDMRequest = false;
    public boolean isCreatingGroup = false;
    public boolean isAddingToGroup = false;
    public boolean isGroupAssignment = false;
    public boolean isGroupMessage = false;
    
    public String msg;
    public HashSet<String> set;
    public String name;
    String userA;
    String userB;
    User seeder;
    User participant;
    int groupAssignment;


    GuiModder(String message) {
        isMessage = true;
        msg = message;
    }
    GuiModder(Set<String> s) {
        isUserUpdate = true;
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
        else if ("".contains(givenName)) {
            isGroupAssignment = true;
            groupAssignment = groupNum;
        }
    }

    GuiModder(String requestingUser, String receivingUser, int groupNum) {
        isDMRequest = true;
        userA = requestingUser;
        userB = receivingUser;
        groupAssignment = groupNum;
    }

    GuiModder(User usr) {
        isCreatingGroup = true;
        seeder = usr;
    }

    GuiModder(User usr, int GroupNum) {
        isAddingToGroup = true;
        participant = usr;
    }

    GuiModder(String message, int groupNdx) {
        isGroupMessage = true;
        groupAssignment = groupNdx;
    }
}
