
import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    
    private static int count;
    public static void main(String args[]) {
        
        Map <Socket, Integer> devices = new HashMap<Socket, Integer>();
        ServerSocket serverSocket = null;
        Socket socket = null;
        Socket address = null;

        int attempts;
        int serverport = Integer.parseInt(args[0]);
        try {
            attempts = Integer.parseInt(args[1]);
            if (attempts > 5 || attempts < 0) {
                System.out.println("Invalid number of allowed failed consecutive attempts: " + attempts +". The valid value of argument number is an integer between 1 and 5");
            }

        } catch (Exception e) {
            System.out.println("Invalid number of allowed failed consecutive attempts: " + args[1] +". The valid value of argument number is an integer between 1 and 5");
            return;
        }



        try {
            serverSocket = new ServerSocket(serverport);
        } catch (IOException e) {
            e.printStackTrace();

        }
        while (true) {
            try {
                socket = serverSocket.accept();
                address = socket;
                devices.put(address,0);
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            new EchoThread(socket,attempts).start();
            count += 1;
        }
    }

 
    public static int getSize() {
        return count;
    }

}
