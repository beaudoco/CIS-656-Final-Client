import java.io.Serializable;

public class StringRpcRequest implements Serializable {

    private static final long serialVersionUID = 7503710091945320739L;

    private String method;
    private String val;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getString() {
        return val;
    }

    public void setString(String val) {
        this.val = val;
    }

}
