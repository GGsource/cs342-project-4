import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class GuiModder implements Serializable {
    public boolean isMessage = false;
    public boolean isUserUpdate = false;
    public boolean isReminder = false;
    public boolean isDMRequest = false;
    public boolean isDMReceiver = false;
    
    public String msg;
    public HashSet<String> set;
    public int reminder;
    String userA;
    String userB;


    GuiModder(String message) {
        isMessage = true;
        msg = message;
    }
    // GuiModder(ArrayList<Server.ClientThread> givenClients) {
    //     isUserUpdate = true;
    //     clients = new ArrayList<>();
    //     for (Server.ClientThread ct : givenClients)
    //         clients.add(ct);
    // }
    GuiModder(Set<String> s) {
        isUserUpdate = true;
        set = new HashSet<>();
        for (String str : s) {
            set.add(str);
        }
    }
    // GuiModder(String messge, ArrayList<Server.ClientThread> givenClients) {
    //     isMessage = true;
    //     isUserUpdate = true;
    //     msg = messge;
    //     clients = new ArrayList<>();
    //     for (Server.ClientThread ct : givenClients)
    //         clients.add(ct);
    // }
    GuiModder(int count) {
        isReminder = true;
        reminder = count;
    }

    GuiModder(Boolean isRequester, String requestingUser, String receivingUser) {
        if (isRequester)
            isDMRequest = true;
        else
            isDMReceiver = true;
        
        userA = requestingUser;
        userB = receivingUser;
    }
}
