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
            // CONNECT TO SERVER
            sock = new Socket(server, PORT);

            // OPEN CLIENT AS SERVER
            new ServerWait(new ClientList()).start();

            // GET RESPONSE FROM SERVER
            ObjectInputStream isr = new ObjectInputStream(sock.getInputStream());
            Object response = isr.readObject();

            // THIS IS IF THERE IS ALREADY A CLIENT HOST
            if (response.toString().contains("/")) {
                // THIS IS THE CLIENT THE SERVER SUGGESTED TO CONNECT TO
                String clientHost = response.toString();

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

                // WE HAVE SUCCESSFULLY CONNECTED TO A CLIENT HOST, ADD THEM AS A NEIGHBOR
                clientList.addClient(clientHost);

                // SHOW THE LIST
                for (int i = 0; i < clientList.getClients().size(); i++) {
                    System.out.println(clientList.getClients().get(i));
                }
            }
//            if (response.toString().contains("/")) {
//                String hostIP = response.toString();
//
//                while(hostIP.contains("/")) {
//                    String tmpHostIP = hostIP.split(":")[0];
//                    tmpHostIP = tmpHostIP.split("/")[1];
//
//                    System.out.println("tmp: " + tmpHostIP);
//
//                    sock2 = new Socket(tmpHostIP, 8080);
//                    isr = new ObjectInputStream(sock2.getInputStream());
//                    response = isr.readObject();
//                    System.out.println("tmp response: " + tmpHostIP);
//                    if (response.toString().contains("/")) {
//                        System.out.println("tmp response bad: " + response.toString());
//                        StringRpcRequest tmpStringRpcRequest = generateServerRequest("");
//                        ObjectOutputStream tmpOut = new ObjectOutputStream(sock2.getOutputStream());
//                        tmpOut.writeObject(tmpStringRpcRequest);
//                        tmpOut.flush();
//                        sock2.close();
//                    } else {
//                        System.out.println("tmp response good: " + hostIP);
//                        clientList.addClient(hostIP);
//                    }
//                    hostIP = response.toString();
//                }
//                //clientList.addClient(hostIP);
//                System.out.println(response);
//
//                while (clientHasValue) {
//                    System.out.println("Please give a string");
//                    Scanner in = new Scanner(System.in);
//                    String s = in.nextLine();
//
//                    clientHasValue = !s.isEmpty();
//
//                    StringRpcRequest stringRpcRequest = generateServerRequest(s);
//                    ObjectOutputStream out = new ObjectOutputStream(sock2.getOutputStream());
//                    out.writeObject(stringRpcRequest);
//                    out.flush();
//
//                    if (clientHasValue) {
//                        isr = new ObjectInputStream(sock2.getInputStream());
//                        response = isr.readObject();
//                        if (response instanceof String) {
//                            System.out.println("Got this from the Server: " + response.toString());
//
//                        } else {
//                            sock2.close();
//                            throw new InternalError();
//
//                        }
//                    } else {
//                        System.out.println("Ending Client");
//                        out = new ObjectOutputStream(sock.getOutputStream());
//                        out.writeObject(stringRpcRequest);
//                        out.flush();
//                        sock.close();
//                        sock2.close();
//                        System.exit(0);
////                        return;
//                    }
//                }
//            }

            // WE ARE CHECKING THE RESULTS
            System.out.println(response);

            // THIS IS COMMUNICATION BETWEEN THE MAIN SERVER AND THE CLIENT
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
