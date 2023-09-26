
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.*;
import java.net.*;

public class ReadUdp implements Runnable{
    
    DatagramSocket sockett;
    int c_udp_port;
    String device;

    public ReadUdp(int c_udp_port, String device) throws SocketException {
        this.c_udp_port = c_udp_port;
        this.device = device;
        sockett = new DatagramSocket(this.c_udp_port);
    }
    
    public void run()
    {
        while(true)
        {  
            try {
                receiver();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    public void receiver() throws IOException {
        
        byte[] receiveFileName = new byte[1024];
        DatagramPacket receiveFileNamePacket = new DatagramPacket(receiveFileName, receiveFileName.length);
        sockett.receive(receiveFileNamePacket);
        
        byte [] data = receiveFileNamePacket.getData();
        String p = new String(data, 0, receiveFileNamePacket.getLength()); 
        String[] ah = p.split("\\s");
        String filename = ah[0];
        String dname = ah[1];
        
        
        FileOutputStream fos = new FileOutputStream(filename);
        
        boolean end;
        int cur = 0;
        int fin = 0;
        int len = 0;
        
        while (true) {
            byte[] receiveData = new byte[1024]; 
            byte[] fileByteArray = new byte[1020];
            
            
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            sockett.receive(receivePacket);
            receiveData = receivePacket.getData();
            
            cur = ((receiveData[0] & 0xff) << 8) + (receiveData[1] & 0xff);
            end = (receiveData[2] & 0xff) == 1;
            len = (receiveData[3] & 0xff);
            
            InetAddress address = receivePacket.getAddress();
            int port = receivePacket.getPort();
            
            
            if (cur == (fin + 1)) {
                
                fin = cur;
                System.arraycopy(receiveData, 4, fileByteArray, 0, 1020);
                
                fos.write(fileByteArray,0, len);
                
                sendAck(fin, sockett, address, port);
            } else {
                sendAck(fin, sockett, address, port);
            }
            
            if (end) {
                System.out.println("a file " + filename + " has been received from " + device); 
                break;
            }
        
        }
        
    }
    
    
    private void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (foundLast >> 8);
        ackPacket[1] = (byte) (foundLast);
        
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        
    }
    
    
}
