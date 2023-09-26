
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class EchoThread extends Thread {
    protected Socket socket;
    public int attempts;
    public boolean blocked;
    public long startTime;
    public long elapsedTime;
    protected int max;
    public boolean auth;
    public String name;
    private InputStream inp = null;
    private BufferedReader brinp = null;
    private DataOutputStream out = null;
    public int sequence_num;
    

    public EchoThread(Socket clientSocket, int max) {
        this.socket = clientSocket;
        this.attempts = 0;
        this.blocked = false;
        this.startTime = 0;
        this.max = max;
        this.auth = false;
    }

    public void run() {
       
        try {
            inp = socket.getInputStream();
            brinp = new BufferedReader(new InputStreamReader(inp));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        String line;
        while (true) {
            
            try {
                line = brinp.readLine();
                if (line == null) {
                  
                } else {

                    
                    if (!this.auth) {
                        if (isblocked()) {
                                out.writeBytes("Your account is blocked due to multiple authentication failures. Please try again later" + "\n\r");
                                out.flush();
                                
                            } else {
                                if  (auth(line)) {
                                    this.auth = true;
                                    sequence_num = Server.getSize();
                                    out.writeBytes("Welcome " +  Integer.toString(sequence_num) + "\n\r");
                                    out.flush();
                                    String[] hm = brinp.readLine().split("\\s",0);
                                    
                                    
                                    FileWriter myWriter = new FileWriter("edge-device-log.txt", true);
                                    this.name = hm[3];
                                
                                    String joined = tStamp();
                                
                                    myWriter.write(Integer.toString(sequence_num) + "; " + joined + "; " + this.name + "; " + hm[0] + "; " + hm[2] + "\r\n");
                                    myWriter.close();
                                } else {
                                    if (this.attempts == this.max) {
                                        this.attempts = 0;
                                        this.blocked = true;
                                        this.startTime = System.currentTimeMillis();
                                        out.writeBytes("Invalid. Your account has been blocked. Please try again later" + "\r\n");
                                        out.flush(); 
                                    }  else {
                                        this.attempts += 1;
                                        out.writeBytes("Invalid. attempt " + this.attempts + "\n\r");
                                        out.flush(); 
                                    }
            
                                }
    
                            }
                        
                    } else {
                        String[] stuff = line.split("\\s",0);
                        if (stuff[0].equals("UED")) {
                            receiveFile(stuff[1]);
                            out.writeBytes("File received" + "\n\r");
                            out.flush(); 
                        }
                        else if (stuff[0].equals("SCS")) {
                            try {
                                BufferedReader reader = new BufferedReader(new FileReader(stuff[1]));
                                List<Integer> numbers = new ArrayList<Integer>();
                                String r;
                                while((r = reader.readLine()) != null) { 
                                    numbers.add(Integer.parseInt(r));
                                }
                                int val = operations(stuff[2], numbers);
                                out.writeBytes("Result " + val + "\n\r");
                                out.flush(); 
                                reader.close();

                            } catch (FileNotFoundException e) {
                                out.writeBytes("File does not exist on server" + "\n\r");
                                out.flush(); 
                            }
                        }
                        else if (stuff[0].equals("DTE")) {
                            File file = new File(stuff[1]);
                            
                            if (!file.exists()) {
                                out.writeBytes("File does not exist on server" + "\n\r");
                                out.flush();
                            }
                            else {
                                FileWriter myWriter = new FileWriter("deletion-log.txt",true);
                                myWriter.write(this.name + "; " + tStamp() + "; " + stuff[1] + "; " + getlines(stuff[1]) + '\n');
                                file.delete();
                                myWriter.close();
                                out.writeBytes("file with ID of fileID has been successfully removed from the central server" + "\n\r");
                                out.flush();  
                            }
                        }   
                        else if (stuff[0].equals("AED")) {

                            String n = stuff[1];
                            BufferedReader reader = new BufferedReader(new FileReader("edge-device-log.txt"));
                            String currentLine;

                            String devs = Integer.toString(Server.getSize()-1);
                            out.writeBytes(devs + "\n\r");
                            out.flush();


                            while((currentLine = reader.readLine()) != null) {
                                Pattern pattern = Pattern.compile('^'+ n, Pattern.CASE_INSENSITIVE);
                                Matcher matcher = pattern.matcher(currentLine);
                                if (!matcher.find()) {
                                    out.writeBytes(currentLine + "\n\r");
                                    out.flush();  
                                }
                            }
                            
                            reader.close();
                        }
                        else if (stuff[0].equals("OUT")) {

                            String n = stuff[1];
                            File inputFile = new File("edge-device-log.txt");
                            File tempFile = new File("myTempFile.txt");
                            
                            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
                            
                
                            String currentLine;
                            
                            while((currentLine = reader.readLine()) != null) {
                                Pattern pattern = Pattern.compile('^'+ n, Pattern.CASE_INSENSITIVE);
                                Matcher matcher = pattern.matcher(currentLine);
                                if(matcher.find()) continue;
                                writer.write(currentLine + System.getProperty("line.separator"));
                            }
                            writer.close(); 
                            reader.close();
                            Files.move(tempFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        
                    }
                        
                }
                
                
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
    

    private int getlines(String fileName) {
        int lines = 0;
      try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
          while (reader.readLine() != null) lines++;
      } catch (IOException e) {
          e.printStackTrace();
      }
      return lines;
    }


    private int operations(String op, List<Integer> numbers) throws IOException {
        if (op.equals("SUM")) {
            int sum = 0;
            for (Integer i : numbers) {
                sum += i;
            }
            return sum;
        }
        else if (op.equals("AVERAGE")) {
            int sum = 0;
            for (Integer i : numbers) {
                sum += i;
            }
            return sum/numbers.size();
        }
        else if (op.equals("MAX")) {
            return Collections.max(numbers);
        } 
        else if (op.equals("MIN")) {
            return Collections.min(numbers);
        }
        
        out.writeBytes("Invalid operation type" + "\n\r");
        out.flush();
        return -1;
    }



    private void receiveFile(String fileName)
    throws Exception
    {
        int bytes = 0;
        OutputStream fileOutputStream
        = new FileOutputStream(fileName);
        
        
        byte[] buffer = new byte[4 * 1024];
        
        long size = new DataInputStream(socket.getInputStream()).readLong();
        
        while (size > 0 && (bytes = inp.read(buffer,0,(int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes;
        }
        FileWriter myWriter = new FileWriter("uploadlog.txt",true);
        myWriter.write(this.name + "; " + tStamp() + "; " + fileName + "; " + getlines(fileName)); 
        myWriter.close();
        
        fileOutputStream.close();
    }         
        
        
        
    public String tStamp() {
        String timeStamp = new SimpleDateFormat("dd mm yyyy hh:mm:ss").format(new Timestamp(System.currentTimeMillis()));
        return timeStamp;
    }
    

    
    public boolean isblocked() {
        if (this.blocked == true) { 
            if (System.currentTimeMillis() -  this.startTime > (10*1000)) {
                this.attempts = 0;
                this.startTime = 0;
                this.blocked = false;
            }
        } 
        return this.blocked;
    }


    public boolean auth(String auth) { 
        
        File file = new File("credentials.txt");
        try {
        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.equals(auth)) { 
                scanner.close();
                return true;
            }
        }
        scanner.close();

        return false;

        } catch(FileNotFoundException e) { 
            System.out.println("gggggg");
            return false;
        }

    }


}