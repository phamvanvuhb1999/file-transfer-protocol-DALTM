package clientSide;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;


public class Client {
    /**
     * Indicating the last set transfer Type
     */
    private enum transferType {
        ASCII, BINARY
    }
    //control connection
    private Socket controlSocket;
    private PrintWriter controlOutWriter;
    private BufferedReader controlIn;
    private int controlPort;
    private String ServerAddress;

    //data Connection 
    private Socket dataConnection;
    private PrintWriter dataOutWriter;
    //private int dataPort;
    
    private transferType transferMode = transferType.ASCII;

    private boolean quitCommandLoop = false;
    private static String directory = "";

    private static String[] shouldASCII = {"txt", "html", "htm", "xml", "cgi", "pl", "php","cf","svg","asp","rtf","ps"};


    public static void main(String[] args)
    {
        Client client = new Client("localhost", 1234);
        System.out.println(client.loggin("comp4621", "network"));

        //store a file
        client.Store("filename.txt");

        //download file
        client.download("file.txt");

        client.download("yours.png");
        while(true){
            
        }
        //System.out.println(new File("" + "filename.txt").exists());
    }

    public Client(String serverAddress, int controlPort){
        this.ServerAddress = serverAddress;
        this.controlPort = controlPort;
    }

    //loggin 
    public boolean loggin(String user, String password){
        try{
            this.controlSocket = new Socket(this.ServerAddress, this.controlPort);
            this.controlOutWriter = new PrintWriter(controlSocket.getOutputStream(),true);
            this.controlIn = new BufferedReader(new java.io.InputStreamReader(controlSocket.getInputStream()));

            DebugMsg(controlIn.readLine());

        }catch(Exception e){
            DebugMsg("Could not create client");
            e.printStackTrace();
        }
        if(this.controlSocket == null || this.controlOutWriter == null || this.controlIn == null){
            DebugMsg("Control connection is not already");
            return false;
        }else{
            try{
                if(user != null){
                    boolean flag = false;
                    long prev = new Date().getTime();
                    //send username to server
                    controlOutWriter.println("USER "+user);
                    while(flag == false && (new Date().getTime()-prev<5000)){
                        String response = controlIn.readLine();
                        //debug
                        DebugMsg(response);
                        String[] splitResponse = response.split(" ");
                        if(splitResponse[0].equals("531")){
                            return false;
                        }else {
                            if(splitResponse[0].equals("530")){
                                return true;
                            }else {
                                flag = true;
                            }     
                        }
                    }
                    if(password != null){

                        controlOutWriter.println("PASS " + password);
                        while(new Date().getTime()-prev<11000){
                            String response = controlIn.readLine();
                            //debug
                            DebugMsg(response);
                            String[] splitResponse = response.split(" ");
                            if(splitResponse[0].equals("531")){
                                return false;
                            }
                            else{
                                return true;
                            }

                        }
                        DebugMsg("Stoped wait for server response");
                        return false;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                DebugMsg("Can not loggin.");
            }
            return false;
        }
    }

    private void DebugMsg(String msg){
        if(msg != ""){
            System.out.println(msg);
        }
    }

    private boolean SetDataType(){
            try{
                if(this.transferMode == transferType.ASCII){
                    controlOutWriter.println("TYPE A");
                }else {
                    controlOutWriter.println("TYPE I");
                }
                DebugMsg("Set type: " + this.transferMode);

                String resType = controlIn.readLine().split(" ")[0];
                //DebugMsg(resType);
                if(resType.equals("200")){
                    return true;
                }
                return false;
            }catch(Exception e){
                e.printStackTrace();
                DebugMsg("Could request datatype");
                return false;
            }      
    }

    private void ChangeMyDataType(String filename){
        if(filename == null){
            return;
        }else {
            int index = filename.lastIndexOf(".");
            String typeFile = filename.substring(index+1, filename.length()).toLowerCase();
            this.transferMode = transferType.BINARY;
            //reach file transfer type by file input type
            for(String type : Client.shouldASCII){
                if(typeFile.equals(type)){
                    this.transferMode = transferType.ASCII;
                    return;
                }
            } 
        }
    }

    private void openPassive(){
        try{
            controlOutWriter.println("PASV");
            boolean flag = false;
            while(!flag){
                String response = controlIn.readLine().trim();
                String[] resPasv = response.split(" ");
                if(resPasv[0].equals("227")){
                    String[] prPort = response.split(",");
                    int dtPort = Integer.parseInt(prPort[prPort.length - 1])  + 256*Integer.parseInt(prPort[prPort.length - 2]);
                    DebugMsg(dtPort+"");
                    openDataConnectionPasv(dtPort);
                    flag = true;
                }
                
            }

        }catch(Exception e){
            e.printStackTrace();
            DebugMsg("Could not open PASSIVE mode.");
        }
        

    }
   
    private boolean CheckControlConnect(){
        if(this.controlSocket == null || this.controlOutWriter == null || this.controlIn == null){
            DebugMsg("Control connection is not ready");
            return false;
        }else {
            return true;
        }
    }
    private boolean CheckDataConnect(){
        if(this.dataConnection == null || this.dataOutWriter == null){
            return false;
        }else {
            return true;
        }
    }
    
    private void openDataConnectionPasv(int port){
        try{
            dataConnection = new Socket("localhost", port);
            dataOutWriter = new PrintWriter(new OutputStreamWriter(dataConnection.getOutputStream()));
            DebugMsg("Open dataconnection passive for client.");
        }catch(Exception e){
            DebugMsg("Could not open data connection pasv mode");
            e.printStackTrace();
        }
    }
    private void closeDataConnection(){
        try{
            dataOutWriter.close();
            dataConnection.close();
            // if(dataSocket != null){
            //     dataSocket.close();
            // }
            DebugMsg("Data Connection was closed");
        }catch(Exception e){
            DebugMsg("Can not close data connection");
        }
    }

    private void Store(String filename){
        //set my transfer type
        ChangeMyDataType(filename);
        //set server handler transfer type
        SetDataType();
        //open data connection passive server
        openPassive();
        try{
            File file = new File(directory+filename);
            if(!file.exists() || !CheckDataConnect()){
                return;
            }
            controlOutWriter.println("STOR " + filename);
            String resStor = controlIn.readLine();
            DebugMsg(resStor);
            if(resStor.split(" ")[0].equals("150") && CheckDataConnect()){
                if(this.transferMode == transferType.BINARY){
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                    BufferedOutputStream bos = new BufferedOutputStream(dataConnection.getOutputStream());

                    DebugMsg("Starting sending file " + filename);
                    SenderI sender = new SenderI(bis, bos);
                    sender.start();
                    sender.join();

                    DebugMsg("Finished send file");
                }else {
                    BufferedReader rin = new BufferedReader(new FileReader(file));
                    //PrintWriter rout = new PrintWriter(dataConnection.getOutputStream(), true);
                    SenderA sender = new SenderA(rin, this.dataOutWriter);
                    sender.start();
                    sender.join();

                    DebugMsg("Finished send file .");
                }
            } else {
                DebugMsg("Could not sending file to server.");
            }
            closeDataConnection();

        }catch(Exception e){
            e.printStackTrace();
            DebugMsg("Could not send request STOR.");
        }
    }
    private void download(String filename){
        File file = new File(this.directory + filename);
        if(file.exists()){
            return;
        }
        
        //send PASV , receive response and open dataconnection on port
        if(!CheckControlConnect()){
            return;
        }
        openPassive();
        //set transfer type for client
        ChangeMyDataType(filename);
        if(!SetDataType()){
            DebugMsg("Could not set datatype transfer for the server.");
        }
        if(CheckDataConnect()){
            try{
                controlOutWriter.println("RETR " + filename);
                String response = controlIn.readLine();
                DebugMsg(response);
                if(response.split(" ")[0].equals("150")){
                    if(transferMode == transferType.ASCII){
                        BufferedReader rin = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
                        PrintWriter rout = new PrintWriter(new FileOutputStream(file), true);
                        ReceiverA receive = new ReceiverA(rin, rout);
                        receive.start();
                        receive.join();

                        DebugMsg("Finished receive file from server. by " + this.transferMode.toString());
                    }else {
                        BufferedInputStream fin = new BufferedInputStream(dataConnection.getInputStream());
                        BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(file));

                        ReceiverI receive = new ReceiverI(fin, fout);
                        receive.start();
                        receive.join();
                        DebugMsg("Finished receive file from server. by " + this.transferMode.toString());

                        fout.close();
                        fin.close();
                    }
                    closeDataConnection();
                }else {
                    DebugMsg("File " + filename + " is not exists on server.");
                    return;
                }

            }catch(Exception e){
                e.printStackTrace();
                DebugMsg("NO contact to server.");
            }
            
        }

        
    }

}


class SenderI extends Thread{
    private BufferedInputStream is;
    private BufferedOutputStream os;
    public SenderI(BufferedInputStream is, BufferedOutputStream os){
        this.is = is;
        this.os = os;
    }
    @Override
    public void run() {
        byte[] buf = new byte[1024];
        int i;
        try{
            while((i = is.read(buf, 0, 1024)) != -1){
                os.write(buf, 0, i);
            }
            os.flush();
            //close data stream
            is.close();
            os.close();
        }catch(Exception  e){
            e.printStackTrace();
        }
    }
}

class SenderA extends Thread{
    private BufferedReader bfr;
    private PrintWriter prw;
    public SenderA(BufferedReader bfr, PrintWriter prw){
        this.bfr = bfr;
        this.prw = prw;
    }
    @Override
    public void run() {
        String s;
        try{
            while((s = bfr.readLine()) != null){
                prw.println(s);
                System.out.println(s);
            }
            prw.flush();

            bfr.close();
            //prw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
class ReceiverI extends Thread{
    private BufferedInputStream is;
    private BufferedOutputStream os;
    public ReceiverI(BufferedInputStream is, BufferedOutputStream os){
        this.is = is;
        this.os = os;
    }
    @Override
    public void run() {
        byte[] buf = new byte[1024];
        int len = 0;
        try{
            while((len = is.read(buf, 0, 1024)) != -1){
                os.write(buf, 0, len);
            }
            os.flush();
            is.close();
            os.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

class ReceiverA extends Thread {
    BufferedReader in;
    PrintWriter out;
    public ReceiverA(BufferedReader in, PrintWriter out){
        this.in = in;
        this.out = out;
    }
    @Override
    public void run() {
        String s;
        try{
            while((s = in.readLine()) != null){
                out.println(s);
            }
            in.close();
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}