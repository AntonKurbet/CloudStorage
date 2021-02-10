package messages;

public class AuthMessage implements ExchangeMessage {
    private String login;
    private int passwordHash;

    public AuthMessage(String login, int passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getLogin() {
        return login;
    }

    public int getPasswordHash() {
        return passwordHash;
    }
}
