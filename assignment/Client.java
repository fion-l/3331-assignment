
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;




public class Client
{
    private DataOutputStream outToServer = null;
    public String name;
    public String seq;
    DatagramSocket sockett = null;
    public List<String> devices;
    public int udport;

    public static void main(String[] args) throws Exception
   {
    
    Client client = new Client();


    if (args.length != 3) {
        System.out.println("Invalid");
        return;
     }
    InetAddress host = InetAddress.getByName(args[0]);
    int port = Integer.parseInt(args[1]);
    int c_udp_port = Integer.parseInt(args[2]);
    Socket socket = new Socket(host, port);
    
    client.go(socket,args[0],port,c_udp_port);
    
    
    
    
}


public void go(Socket socket, String host, int port, int c_udp_port) throws Exception {
    
    
    udport = c_udp_port;
   
    auth(socket);
    
    outToServer = new DataOutputStream(socket.getOutputStream());
    outToServer.writeBytes(host + ' ' + port + ' ' + c_udp_port + ' ' + name + ' ' + '\n');
    
    commands(socket);
    
    System.out.println("bye :)");
    socket.close();
    
}


   public void auth(Socket socket) throws Exception {
    String login;
    System.out.println("Input login");
    BufferedReader l =
        new BufferedReader(new InputStreamReader(System.in));
    login = l.readLine();

    
    String password;
    System.out.println("Input password");
    BufferedReader p =
        new BufferedReader(new InputStreamReader(System.in));
    password = p.readLine();
    
    outToServer = new DataOutputStream(socket.getOutputStream());
	outToServer.writeBytes(login + ' ' + password + '\n');

   
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    String[] sentenceFromServer;
    sentenceFromServer = inFromServer.readLine().split("\\s");
    System.out.println(sentenceFromServer[0]);
  

    if (!sentenceFromServer[0].equals("Welcome")) {
        auth(socket);
    }

    seq = sentenceFromServer[1];
    name = login;
     
   
    ReadUdp udp = new ReadUdp(udport, name);
    Thread reader_thread = new Thread(udp);
    reader_thread.start();
   }

    public void commands(Socket socket) throws Exception {
    

    System.out.println("Enter one of the following commands (EDG, UED, SCS, DTE, AED, UVF, OUT):");
    
    BufferedReader com =
        new BufferedReader(new InputStreamReader(System.in));
    String command = com.readLine();

    String[] stuff = command.split("\\s",0);
        if (stuff[0].equals("EDG")) {
            if (stuff.length == 3) {
                if (isInt(stuff[1]) && isInt(stuff[2])) {
                    if (EDG(Integer.parseInt(stuff[1]), Integer.parseInt(stuff[2]))) {
                        System.out.println("data generation done" + "\n\r");
                    }      
                } else {
                    System.out.println("the fileID or dataAmount are not integers, you need to specify the parameter as integers" + "\n\r");
                }
            } else {
                System.out.println("EDG command requires fileID and dataAmount as arguments" + "\n\r");
            }      
        } 
        else if (stuff[0].equals("UED")) {
            if (stuff.length == 2) {
                String fname = getfile(Integer.parseInt(stuff[1]));
                File file = new File(fname);
                if (!file.exists()) {
                    System.out.println("the file to be uploaded does not exist" + "\n\r");
                }
                else {
                    outToServer = new DataOutputStream(socket.getOutputStream());
                    outToServer.writeBytes(command + '\n');
                    sendFile(fname);
                    BufferedReader inFromServer1 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    System.out.println(inFromServer1.readLine());
                }

            } else {
                System.out.println("fileID is needed to upload the data" + "\n\r");
            }
        }
        else if (stuff[0].equals("SCS")) {
            if (stuff.length == 3 && isInt(stuff[1])) {
                int fid;
                try  {
                    fid = Integer.parseInt(stuff[1]);
                } catch (NumberFormatException e) {
                    System.out.println("fileID should be an integer" + "\n\r");
                }
                
                //getfile(fid);
                outToServer = new DataOutputStream(socket.getOutputStream());
                outToServer.writeBytes(command + '\n');
                
                BufferedReader inFromServer1 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println(inFromServer1.readLine());
                
            } else {
                System.out.println("fileID is missing or fileID should be an integer" + "\n\r");
            }
            
        }
        else if (stuff[0].equals("DTE")) {
            if (stuff.length == 2) {
                outToServer = new DataOutputStream(socket.getOutputStream());
                outToServer.writeBytes(command + '\n');

                BufferedReader inFromServer1 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println(inFromServer1.readLine());
            } else {
                System.out.println("DTE command requires fileID as argument" + "\n\r");
            }
        }
        else if (stuff[0].equals("AED")) {
            
            outToServer = new DataOutputStream(socket.getOutputStream());
            outToServer.writeBytes(command + ' ' + seq + '\n');

            devices = new ArrayList<String>();
            
            BufferedReader inFromServer1 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            int line = Integer.parseInt(inFromServer1.readLine());
            
            for (int i = 0; i < line; i++) {
                String e = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
                System.out.println(e);
                String[] ee = e.split("; ");
                devices.add(ee[2] + ' ' + ee[3] + ' ' + ee[4]);
            }

            if (line == 0) {
                System.out.println("no other active edge devices");
            }
            
        }
        else if (stuff[0].equals("OUT")) {
            
            outToServer = new DataOutputStream(socket.getOutputStream());
            outToServer.writeBytes(command + ' ' + seq + '\n');
            return;
        }
        else if (stuff[0].equals("UVF")) {
            
            String dname = stuff[1];
            String fname = stuff[2];
            int udportr = 0;
            InetAddress host = null;

            for (String x : devices) {
                String[] z = x.split("\\s");
                if (z[0].equals(dname)) {
                    host = InetAddress.getByName(z[1]);
                    udportr = Integer.parseInt(z[2]);
                }
            }
            
            sender(fname,host,udportr);   
        }
        
        commands(socket);
    }
    
    public boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
 
    public void sender(String path, InetAddress host, int port) throws Exception {
        
        
        DatagramSocket serverSocket = new DatagramSocket();
        
        
        int cur = 0;
        int seq = 0;
        boolean flag;
        
        byte[] fileNameBytes = (path + ' ' + name).getBytes(); 
        DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, host, port); 
        serverSocket.send(fileStatPacket);
        
        
        File file = new File(path);
        

        byte[] sendData=new byte[(int) file.length()];

        FileInputStream fis = new FileInputStream(path);
        fis.read(sendData);

        for (int i = 0; i < sendData.length; i = i + 1020) {
            cur += 1;
            byte[] message = new byte[1024];

            message[0] = (byte) (cur >> 8);
            message[1] = (byte) (cur);
            message[3] = (byte) sendData.length;
            
            if ((i + 1020) >= sendData.length) { 
                flag = true;
                message[2] = (byte) (1); 
            } else {
                flag = false;
                message[2] = (byte) (0);
            }

            if (!flag) {
                System.arraycopy(sendData, i, message, 4, 1020);
            } else {
                System.arraycopy(sendData, i, message, 4, sendData.length - i);
                
            }
            
            DatagramPacket sendPacket = new DatagramPacket(message, message.length, host, port); 
            serverSocket.send(sendPacket);

            boolean ackRec; 

            while (true) {
                byte[] ack = new byte[2]; 
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    serverSocket.setSoTimeout(50);
                    serverSocket.receive(ackpack);
                    seq = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
                    ackRec = true; 
                } catch (SocketTimeoutException e) {
                    ackRec = false; 
                }

           
                if ((seq == cur) && (ackRec)) {
                    break;
                } 
                else {
                    serverSocket.send(sendPacket);
                }
            }
        }
        
        fis.close();

    }

 
    public boolean EDG(Integer fileID, Integer data) throws FileNotFoundException {
        
        PrintWriter writer = new PrintWriter(name + "-" + fileID + ".txt"); 
        
        for (int i = 0; i < data; i++) {
            writer.println(ThreadLocalRandom.current().nextInt(0, 10));
        }
        writer.close();
        return true;
    }
    
    
    public String getfile(int id) {
        return (name + "-" + id + ".txt");
    }
        
    
    
    private void sendFile(String path)
        throws Exception
    {
        int bytes = 0;
        
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("the file to be uploaded does not exist" + "\n\r");
            return;
        }

        InputStream fileInputStream
            = new FileInputStream(file);
 
        
        outToServer.writeLong(file.length());
        
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInputStream.read(buffer)) > 0) {
            outToServer.write(buffer, 0, bytes);
            outToServer.flush();
        }
        
        fileInputStream.close();
    }



    



}
