import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ResponseRemoteImpl implements Response {
    public static final int PORT = 4444;

    public void request(String server) {
        Socket sock;
        boolean hasValue = true;

        try {
            sock = new Socket(server, PORT);
//            sock = new Socket("localhost", PORT);

            ObjectInputStream isr = new ObjectInputStream(sock.getInputStream());
            Object response = isr.readObject();

            if (response.toString().contains("\\")) {
                System.out.println("big ben");
            }

            System.out.println(response);


            while (hasValue) {
                System.out.println("Please give a string");
                Scanner in = new Scanner(System.in);
                String s = in.nextLine();

                hasValue = !s.isEmpty();

                StringRpcRequest stringRpcRequest = generateServerRequest(s);
                ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                out.writeObject(stringRpcRequest);
                out.flush();

                if (hasValue) {
                    isr = new ObjectInputStream(sock.getInputStream());
                    response = isr.readObject();
                    if (response instanceof String) {
                        System.out.println("Got this from the Server: " + response.toString());

                    } else {
                        sock.close();
                        throw new InternalError();

                    }
                } else {
                    System.out.println("Ending Client");
                    sock.close();
                }
            }
        } catch (Exception e) {

            throw new InternalError();
        }
    }

    private StringRpcRequest generateServerRequest(String val) {
        StringRpcRequest stringRpcRequest = new StringRpcRequest();
        stringRpcRequest.setString(val);
        stringRpcRequest.setMethod("request");
        return stringRpcRequest;
    }
}
