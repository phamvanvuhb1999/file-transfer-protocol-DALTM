package clientSide;

import java.io.BufferedReader;
import java.io.File;
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
    private ServerSocket dataSocket;
    private Socket dataConnection;
    private PrintWriter dataOutWriter;

    private int dataPort;
    private transferType transferMode = transferType.ASCII;

    private boolean quitCommandLoop = false;
    private static String directory = "data//";

    public static void main(String[] args)
    {
        Client client = new Client("localhost", 1234);
        System.out.println(client.loggin("comp4621", "network"));
        while(true){
            
        }
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
    private void closeDataConnection(){
        try{
            dataOutWriter.close();
            dataConnection.close();
            if(dataSocket != null){
                dataSocket.close();
            }
            DebugMsg("Data Connection was closed");
        }catch(Exception e){
            DebugMsg("Can not close data connection");
        }
    }
}
