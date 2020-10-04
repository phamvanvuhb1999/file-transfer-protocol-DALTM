package clientSide;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
    //private ServerSocket dataSocket;
    private Socket dataConnection;
    private PrintWriter dataOutWriter;

    private int dataPort;
    private transferType transferMode = transferType.ASCII;

    private boolean quitCommandLoop = false;
    private static String directory = "";

    private static String[] shouldASCII = {"txt", "html", "htm", "xml", "cgi", "pl", "php","cf","svg","asp","rtf","ps"};


    public static void main(String[] args)
    {
        Client client = new Client("localhost", 1234);
        System.out.println(client.loggin("comp4621", "network"));
        client.Post1("filename.txt");
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
    private void POST(String filename){
        if(this.controlSocket == null || this.controlOutWriter == null || this.controlIn == null){
            DebugMsg("Control connection is not ready");
        }else {
            controlOutWriter.println("PASV");
            //long prev = new Date().getTime();
            boolean flag = false;
            while(flag==false){
                try{
                    String response = controlIn.readLine();
                    String[] res = response.split(",");
                    int port = Integer.parseInt(res[res.length-1])+Integer.parseInt(res[res.length-2])*256;
                    this.dataPort = port;
                    DebugMsg(response);
                    if(response.split(" ")[0].equals("227")){//check for server ready
                        //control type to write and read data BINARY or ASCII
                        int index = filename.lastIndexOf(".");
                        String typeFile = filename.substring(index+1, filename.length()).toLowerCase();
                        this.transferMode = transferType.BINARY;
                        //reach file transfer type by file input type
                        for(String type : Client.shouldASCII){
                            if(typeFile.equals(type)){
                                this.transferMode = transferType.ASCII;
                                break;
                            }
                        }

                        //send server filetype to set file receive
                        if(transferMode == transferType.ASCII){
                            controlOutWriter.println("TYPE A");
                        }else{
                            controlOutWriter.println("TYPE I");
                        }
                        boolean flag1 = false;
                        while(flag1==false){
                            String response1 = controlIn.readLine();
                            if(!response1.equals("")){
                                DebugMsg(response1);
                                if(response1.split(" ")[0].equals("200")){
                                    //after set file type transfer 200 OK, we send filename to server
                                    controlOutWriter.println("STOR " + filename);
                                    flag1 = true;
                                }else{
                                    DebugMsg("Could not set file transfer type to server");
                                    return;
                                }
                            } 
                        }
                        //send server filename need to store and stringcontrol STOR
                        long prev2 = new Date().getTime();
                        while(new Date().getTime() - prev2 < 5000 && flag==false){//wait to client receive stringcontrol
                            response = controlIn.readLine();
                            if(!response.split(" ")[0].equals("150")){
                                return;
                            }
                            else {
                                //open data connection
                                openDataConnectionPasv(this.dataPort);
                                BufferedInputStream is;
                                BufferedOutputStream os;
                                File input = new File(this.directory + filename);
                                
                                is = new BufferedInputStream(new FileInputStream(input));
                                os = new BufferedOutputStream(dataConnection.getOutputStream());

                                //call and start send thread
                                new Sender(is,os).start();
                                //wait for finished flag from server
                                long prev3 = new Date().getTime();
                                while(new Date().getTime() - prev3 < 5000 && flag==false){
                                    response = controlIn.readLine().split(" ")[0];
                                    if(response.equals("226")){
                                        flag = true;
                                        DebugMsg("Send file to server successfuly.");
                                        closeDataConnection();
                                    }else {
                                        return;
                                    }
                                }
                            }
                            
                        }
                    }
                }catch(Exception e){
                    DebugMsg("Could not send data to server");
                    e.printStackTrace();
                }
                
            }
        }
    }
    private boolean Post1(String filename){
        if(this.controlSocket == null || this.controlOutWriter == null || this.controlIn == null){
            DebugMsg("Control connection is not ready");
            return false;
        }else {
            try{
                controlOutWriter.println("PASV");
            }catch(Exception e){
                e.printStackTrace();
                DebugMsg("Could send PASV request to serverfile");
            }
        }
        String response="";
        try{
            response = controlIn.readLine();
            DebugMsg(response);
        }catch(Exception e){
            e.printStackTrace();
            DebugMsg("Could not receive response PASV");
        }
        String[] resList = response.split(",");
        int port = Integer.parseInt(resList[resList.length-1]) + Integer.parseInt(resList[resList.length-2])*256;
        DebugMsg(port+"");
        this.dataPort = port;
        boolean flag = false;
        if(response.split(" ")[0].equals("227")){
            int index = filename.lastIndexOf(".");
            String typeFile = filename.substring(index+1, filename.length()).toLowerCase();
            this.transferMode = transferType.BINARY;
            //reach file transfer type by file input type
            for(String type : Client.shouldASCII){
                if(typeFile.equals(type)){
                    this.transferMode = transferType.ASCII;
                    break;
                }
            }
            if(this.transferMode == transferType.ASCII){
                controlOutWriter.println("TYPE A");
            }else {
                controlOutWriter.println("TYPE I");
            }
            return true;
        }else{
            return false;
        }
        
    }
    private void openDataConnectionPasv(int port){
        try{
            dataConnection = new Socket("localhost", port);
            dataOutWriter = new PrintWriter(new OutputStreamWriter(dataConnection.getOutputStream()));
        }catch(Exception e){
            DebugMsg("Could not opne data connection pasv mode");
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

}

class Sender extends Thread{
    private BufferedInputStream is;
    private BufferedOutputStream os;
    public Sender(BufferedInputStream is, BufferedOutputStream os){
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
// class Receiver extends Thread{
//     private BufferedInputStream is;
//     private BufferedOutputStream os;
//     public Receiver()
// }