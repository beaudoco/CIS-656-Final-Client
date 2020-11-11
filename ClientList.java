import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClientList implements Serializable {

    List<String> clientList=new ArrayList<>();

    public List<String> getClients() {
        return clientList;
    }

    public void addClient(String val) {
        this.clientList.add(val);
    }

    public void removeClient(String val) {
        this.clientList.remove(val);
    }

}