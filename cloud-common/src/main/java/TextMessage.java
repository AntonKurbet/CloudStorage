import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TextMessage implements Serializable {
    private final String text;
    private final LocalDateTime createAt;

    public TextMessage(String text) {
        this.text = text;
        this.createAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s",createAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),text);
    }
}
