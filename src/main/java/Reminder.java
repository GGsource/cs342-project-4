import java.io.Serializable;

public class Reminder implements Serializable {
    String name;
    Reminder(String title) {
        name = title;
    }
}
