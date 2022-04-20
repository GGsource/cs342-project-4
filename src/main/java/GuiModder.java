import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GuiModder implements Serializable {
    public boolean isMessage = false;
    public boolean isUserUpdate = false;
    
    String msg;
    ArrayList<Server.ClientThread> clients;
    HashSet<String> set;

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
}
