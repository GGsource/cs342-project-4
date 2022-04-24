import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GuiModder implements Serializable {
    public boolean isMessage = false;
    public boolean isUserUpdate = false;
    public boolean isReminder = false;
    public boolean isDMRequest = false;
    public boolean isDMReceiver = false;
    public boolean isUserGroup = false;
    public boolean isGroupAssignment = false;
    public boolean isGroupMessage = false;
    
    public String msg;
    public HashSet<String> set;
    public int reminder;
    String userA;
    String userB;
    ArrayList<String> userList;
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
    GuiModder(int count, int groupNum) {
        if (groupNum == -1){ 
            isReminder = true;
            reminder = count;
        }
        else if (count == -1) {
            isGroupAssignment = true;
            groupAssignment = groupNum;
        }
    }

    GuiModder(Boolean isRequester, String requestingUser, String receivingUser) {
        if (isRequester)
            isDMRequest = true;
        else
            isDMReceiver = true;
        
        userA = requestingUser;
        userB = receivingUser;
    }

    GuiModder(String ... userGroup) {
        userList = new ArrayList<>();
        isUserGroup = true;
        for (String s : userGroup) {
            userList.add(s);
        }
    }

    GuiModder(String message, int groupNdx) {
        isGroupMessage = true;
        groupAssignment = groupNdx;
    }
}
