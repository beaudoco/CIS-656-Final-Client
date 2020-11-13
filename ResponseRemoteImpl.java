import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class ResponseRemoteImpl implements Response {
    public static final int PORT = 4444;

    public void request(String server) {
        Socket sock;
        Socket sock2 = null;
        boolean clientHasValue = true;
        boolean serverHasValue = true;
        ClientList clientList = new ClientList();

        try {
            sock = new Socket(server, PORT);
//            sock = new Socket("localhost", PORT);
            new ServerWait(clientList).start();

            ObjectInputStream isr = new ObjectInputStream(sock.getInputStream());
            Object response = isr.readObject();

            if (response.toString().contains("/")) {
                String hostIP = response.toString();

                while(hostIP.contains("/")) {
                    String tmpHostIP = hostIP.split(":")[0];
                    tmpHostIP = tmpHostIP.split("/")[1];

                    System.out.println("tmp: " + tmpHostIP);

                    sock2 = new Socket(tmpHostIP, 8080);
                    isr = new ObjectInputStream(sock2.getInputStream());
                    response = isr.readObject();

                    if (response.toString().contains("/")) {
                        StringRpcRequest tmpStringRpcRequest = generateServerRequest("");
                        ObjectOutputStream tmpOut = new ObjectOutputStream(sock2.getOutputStream());
                        tmpOut.writeObject(tmpStringRpcRequest);
                        tmpOut.flush();
                        sock2.close();
                    } else {
                        clientList.addClient(hostIP);
                    }
                    hostIP = response.toString();
                }

                System.out.println(response);

                while (clientHasValue) {
                    System.out.println("Please give a string");
                    Scanner in = new Scanner(System.in);
                    String s = in.nextLine();

                    clientHasValue = !s.isEmpty();

                    StringRpcRequest stringRpcRequest = generateServerRequest(s);
                    ObjectOutputStream out = new ObjectOutputStream(sock2.getOutputStream());
                    out.writeObject(stringRpcRequest);
                    out.flush();

                    if (clientHasValue) {
                        isr = new ObjectInputStream(sock2.getInputStream());
                        response = isr.readObject();
                        if (response instanceof String) {
                            System.out.println("Got this from the Server: " + response.toString());

                        } else {
                            sock2.close();
                            throw new InternalError();

                        }
                    } else {
                        System.out.println("Ending Client");
                        out = new ObjectOutputStream(sock.getOutputStream());
                        out.writeObject(stringRpcRequest);
                        out.flush();
                        sock.close();
                        sock2.close();
                        System.exit(0);
//                        return;
                    }
                }
            }

            System.out.println(response);

            while (serverHasValue) {
                System.out.println("Please give a string");
                Scanner in = new Scanner(System.in);
                String s = in.nextLine();

                serverHasValue = !s.isEmpty();

                StringRpcRequest stringRpcRequest = generateServerRequest(s);
                ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                out.writeObject(stringRpcRequest);
                out.flush();

                if (serverHasValue) {
                    isr = new ObjectInputStream(sock.getInputStream());
                    response = isr.readObject();
                    if (response instanceof String) {
                        System.out.println("Got this from the Server: " + response.toString());

                        for(int i = 0; i < clientList.getClients().size(); i++) {
                            System.out.println(clientList.getClients().get(i));
                        }

                    } else {
                        sock.close();
                        throw new InternalError();

                    }
                } else {
                    System.out.println("Ending Client");
                    sock.close();
                    System.exit(0);
//                    return;
                }
            }


        } catch (Exception e) {
            System.out.println(e);

            throw new InternalError();
        }
    }

    @Override
    public String welcomeMessage() {
        return ("Hello, you are in the club \r\n");
    }

    private StringRpcRequest generateServerRequest(String val) {
        StringRpcRequest stringRpcRequest = new StringRpcRequest();
        stringRpcRequest.setString(val);
        stringRpcRequest.setMethod("request");
        return stringRpcRequest;
    }
}

class ServerWait extends Thread {
    ClientList clientList;
    static int clientCount = 0;
    public ServerWait(ClientList clientList) {
        this.clientList = clientList;
    }

    public void run() {
        int maxPendingConn = 10;
        final int port = 8080;
        ServerSocket servsock = null;
        //ClientList clientList = new ClientList();
        try {
            servsock = new ServerSocket(port, maxPendingConn);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            Socket sock = null;
            try {
                sock = servsock.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String clientName = sock.getRemoteSocketAddress().toString();

            if (clientList.getClients().size() <= 3) {
                clientList.addClient(clientName);
                clientCount++;

                new ServerThread(sock, clientCount, clientList, clientName).start();

                System.out.println(clientList.getClients().get(0) + " size: " + clientList.getClients().size());
            } else {
                new ServerThread(sock, clientCount, clientList, clientName).start();
            }
        }
    }
}

class ServerThread extends Thread {
    protected Socket sock;
    protected int clientNumber;
    private Response response = new ResponseRemoteImpl();
    ClientList clientList;
    String clientName;

    public ServerThread(Socket clientSocket, int clientNumber, ClientList clientList, String clientName) {
        this.sock = clientSocket;
        this.clientNumber = clientNumber;
        this.clientList = clientList;
        this.clientName = clientName;
    }

    public void run() {
        // Get I/O streams from the socket
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(sock.getOutputStream());
        } catch (Exception e) {
            System.out.println("error!");
        }

        try {
            if (clientList.getClients().size() <= 1) {
                out.writeObject(response.welcomeMessage());
                out.flush();
            } else {
                List<String> tmpClientList = new ArrayList<>();
                tmpClientList.addAll(clientList.getClients());
                tmpClientList.remove(clientName);
                int randomNum = ThreadLocalRandom.current().nextInt(0, tmpClientList.size());
                out.writeObject(tmpClientList.get(randomNum));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean hasValue = true;

        while(hasValue) {
            String request = null;
            try {
                ObjectInputStream isr = new ObjectInputStream(sock.getInputStream());
                Object object = isr.readObject();

                if (object instanceof  StringRpcRequest) {
                    StringRpcRequest stringRpcRequest = (StringRpcRequest) object;

                    String tmpString = stringRpcRequest.getString();

                    if ("request".equals(stringRpcRequest.getMethod())) {
                        if (tmpString.isEmpty()) {
//                            System.out.println(clientList.getClients().isEmpty());
                            clientList.removeClient(clientName);
                            hasValue = false;
                            sock.close();
                            System.out.println("Socket closed!");
                        }
                        request = tmpString;

                        if (hasValue) {
                            out = new ObjectOutputStream(sock.getOutputStream());
                            out.writeObject(request);
                            out.flush();
                        }
                    }

                } else {
                    System.out.println("error!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("error!");
            }
        }
    }
}
