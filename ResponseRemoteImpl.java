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
        boolean hasValue = true;
        ClientList clientList = new ClientList();
        ClientList hostList = new ClientList();
        List<Socket> sockList = new ArrayList<>();
        String clientHost = "";
        String hostName = "";

        try {
            // CONNECT TO SERVER
            sock = new Socket(server, PORT);

            // GET RESPONSE FROM SERVER
            ObjectInputStream isr = new ObjectInputStream(sock.getInputStream());
            Object response = isr.readObject();

            hostName = sock.getRemoteSocketAddress().toString();
            hostList.addClient(hostName);
            new ClientListen(sock, hostList, hostName).start();

            // THIS IS IF THERE IS ALREADY A CLIENT HOST
            if (response.toString().contains("/")) {
                // THIS IS THE CLIENT THE SERVER SUGGESTED TO CONNECT TO
                clientHost = response.toString();

                // SPLIT IP ADDRESS TO CONNECT TO CLIENT HOST
                String tmpHostIP = clientHost.split(":")[0];
                tmpHostIP = tmpHostIP.split("/")[1];

                //CONNECT TO CLIENT HOST
                sock2 = new Socket(tmpHostIP, 8080);
                isr = new ObjectInputStream(sock2.getInputStream());

                //CHECK RESPONSE
                response = isr.readObject();

                // HERE WE ARE CHECKING WHAT THE CLIENT HAS SAID BACK TO US
                System.out.println(response.toString());

                // THE CLIENT WE WERE TOLD TO GO TO HAS TOO MANY NEIGHBORS
                // CONNECT TO THE NEIGHBOR THEY GAVE US
                while (response.toString().contains("/")) {
                    // CLOSE CONNECTION TO PREVIOUSLY SUGGESTED CLIENT
                    // TELL ORIGINAL HOST CLIENT FORGET ME
                    StringRpcRequest tmpStringRpcRequest = generateServerRequest("");
                    ObjectOutputStream tmpOut = new ObjectOutputStream(sock2.getOutputStream());
                    tmpOut.writeObject(tmpStringRpcRequest);
                    tmpOut.flush();
                    sock2.close();

                    // THIS IS THE NEWLY SUGGESTED CLIENT TO CONNECT TO
                    clientHost = response.toString();
                    tmpHostIP = clientHost.split(":")[0];
                    tmpHostIP = tmpHostIP.split("/")[1];

                    //CONNECT TO NEW CLIENT HOST
                    sock2 = new Socket(tmpHostIP, 8080);
                    isr = new ObjectInputStream(sock2.getInputStream());

                    //CHECK RESPONSE
                    response = isr.readObject();

                    // HERE WE ARE CHECKING WHAT THE CLIENT HAS SAID BACK TO US
                    //System.out.println(response.toString());
                }

                new ClientListen(sock2, clientList, clientHost).start();
                // WE HAVE SUCCESSFULLY CONNECTED TO A CLIENT HOST, ADD THEM AS A NEIGHBOR
                clientList.addClient(clientHost);

                // SHOW THE LIST
                for (int i = 0; i < clientList.getClients().size(); i++) {
                    System.out.println(clientList.getClients().get(i));
                }
            }

            // OPEN CLIENT AS SERVER
            new ServerWait(clientList, sockList).start();

            System.out.println(response);

            // THIS IS COMMUNICATION BETWEEN THE MAIN SERVER AND THE CLIENT
            while (hasValue) {
                System.out.println("Please give a string");
                Scanner in = new Scanner(System.in);
                String s = in.nextLine();

                hasValue = !s.equals("quit");

                StringRpcRequest stringRpcRequest = generateServerRequest(s);
                ObjectOutputStream out; // = new ObjectOutputStream(sock.getOutputStream());
//                out.writeObject(stringRpcRequest);
//                out.flush();

                if (hasValue) {
//                    isr = new ObjectInputStream(sock.getInputStream());
//                    response = isr.readObject();
                    if (response instanceof String) {
                        //System.out.println("Result for: " + response.toString());

                        for(int i = 0; i < clientList.getClients().size(); i++) {
                            System.out.println(clientList.getClients().get(i));
                        }

                    } else {
                        System.out.println("Ending Client");
                        stringRpcRequest = generateServerRequest(s);
                        if (hostList.getClients().size() > 0) {
                            out = new ObjectOutputStream(sock.getOutputStream());
                            out.writeObject(stringRpcRequest);
                            out.flush();
                            sock.close();
                        }

                        if (!clientHost.isEmpty()) {
                            out = new ObjectOutputStream(sock2.getOutputStream());
                            stringRpcRequest = generateServerRequest(s);
                            System.out.println(stringRpcRequest.toString());
                            out.writeObject(stringRpcRequest);
                            out.flush();
                            sock2.close();
                        }
                        System.exit(0);
//                        throw new InternalError();

                    }
                } else {
                    System.out.println("Ending Client");

                    stringRpcRequest = generateServerRequest(s);
                    if (hostList.getClients().size() > 0) {
                        out = new ObjectOutputStream(sock.getOutputStream());
                        out.writeObject(stringRpcRequest);
                        out.flush();
                        sock.close();
                    }

                    if (!clientHost.isEmpty() && clientList.getClients().contains(clientHost)) {
                        out = new ObjectOutputStream(sock2.getOutputStream());
                        stringRpcRequest = generateServerRequest(s);
                        out.writeObject(stringRpcRequest);
                        out.flush();
                        sock2.close();
                    }

                    for (int i = 0; i < sockList.size(); i++) {
                        if (clientList.getClients().contains(sockList.get(i).getRemoteSocketAddress().toString())) {
                            out = new ObjectOutputStream(sockList.get(i).getOutputStream());
                            stringRpcRequest = generateServerRequest(s);
                            out.writeObject(stringRpcRequest);
                            out.flush();
                            sockList.get(i).close();
                            sockList.remove(sockList.get(i));
                        }
                    }
                    System.exit(0);
                }
            }


        } catch (Exception e) {
            //System.out.println(e);
            throw new InternalError();
        }
    }

    // RESPONSE TO A CONNECTED CLIENT
    @Override
    public String welcomeMessage() {
        return ("Hello, you are in the club \r\n");
    }

    // CONTROLS COMMUNICATION BETWEEN MACHINES
    private StringRpcRequest generateServerRequest(String val) {
        StringRpcRequest stringRpcRequest = new StringRpcRequest();
        stringRpcRequest.setString(val);
        stringRpcRequest.setMethod("request");
        return stringRpcRequest;
    }
}

// SETTING UP CLIENT AS SERVER
class ServerWait extends Thread {
    ClientList clientList;
    List<Socket> sockList;
    static int clientCount = 0;
    public ServerWait(ClientList clientList, List<Socket> sockList) {
        this.clientList = clientList;
        this.sockList = sockList;
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
                sockList.add(sock);

                System.out.println(clientList.getClients().get(0) + " size: " + clientList.getClients().size());
            } else {
                new ServerThread(sock, clientCount, clientList, clientName).start();
            }
        }
    }
}

// SETTING UP CLIENT AS SERVER
class ClientListen extends Thread {
    Socket sock;
    ClientList clientList;
    String clientName;
    public ClientListen(Socket sock, ClientList clientList, String clientName) {
        this.sock = sock;
        this.clientList = clientList;
        this.clientName = clientName;
    }

    public void run() {
        boolean hasValue = true;
        while (hasValue) {
            try {
                ObjectInputStream isr = new ObjectInputStream(sock.getInputStream());
                Object object = isr.readObject();

                if (object instanceof  StringRpcRequest) {
                    StringRpcRequest stringRpcRequest = (StringRpcRequest) object;

                    String tmpString = stringRpcRequest.getString();

                    if ("request".equals(stringRpcRequest.getMethod())) {
                        if (tmpString.equals("quit")) {
                            hasValue = false;
                            sock.close();
                            clientList.removeClient(clientName);
                            System.out.println("Socket closed: " + clientName + " disconnected!");
                        }
                    }

                } else {
                    System.out.println("error!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

// USED FOR COMMUNICATION BETWEEN CLIENTS
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
            if (clientList.getClients().size() <= 3) {
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
                        if (tmpString.equals("quit")) {
                            clientList.removeClient(clientName);
                            hasValue = false;
                            sock.close();
                            System.out.println("Socket closed: " + clientName + " disconnected!");
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
            }
        }
    }
}
