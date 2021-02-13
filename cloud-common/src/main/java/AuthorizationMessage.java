public class AuthorizationMessage extends CommandMessage<Boolean>{
    private final String password;

    public AuthorizationMessage(String login, String password) {
        super(ServerCommand.AUTH,login);
        this.password = password;
    }

    public String getLogin() {
        return getParam();
    }
    public String getPassword() {
        return password;
    }
}
