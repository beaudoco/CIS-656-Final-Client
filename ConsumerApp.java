import java.io.IOException;
import java.util.Scanner;

public class ConsumerApp {
    
    public static void main(String[] args) throws IOException {
        Response response = new ResponseRemoteImpl();

        System.out.println("Please give a server IP address");
        Scanner in = new Scanner(System.in);
        String s = in.nextLine();
        response.request(s);
    }
}
