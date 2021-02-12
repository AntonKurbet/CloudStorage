public class ArrayCommandMessage extends CommandMessage<String> {
    private String[] paramArray;

    public String[] getParamArray() {
        return paramArray;
    }

    public void setParamArray(String[] param) {
        this.paramArray = param;
    }

    ArrayCommandMessage(ServerCommand command, String[] param) {
        super(command,null);
        paramArray = param;
    }
}
