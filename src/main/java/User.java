import java.io.Serializable;

public class User implements Serializable {
    String name;
    User(String title) {
        name = title;
    }
}
