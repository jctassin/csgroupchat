/**
 * Jared Tassin
 * Programming Assignment #1 for CSCI 5311, 3/7/2023
 * Client for Group Chat
 */
import java.net.*;
import java.io.*;

public class MyClient {

    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;
    String username = "";

    public MyClient(String address, int port) {
        try {
            socket = new Socket(address, port);
            System.out.println("Connected");
            input = new DataInputStream(System.in);
            out = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            System.out.println("error " + e.getMessage());
        }
        while(username.isEmpty() || username.equals("Bye")){ //while the username isn't empty or the disconnect command, keep asking for a username
            try{
            System.out.println("Enter a username: ");
            username = input.readLine();
            } catch (IOException i) {
                System.out.println(i);
            }
        }
        try{
        out.writeUTF("username = "+username); //let the server know what the username is
        } catch (IOException i) {
                System.out.println(i);
        }
        try{
        ClientListen listen = new ClientListen(socket); //make a threaded scanner for incoming messages so that output can be recieved and printed while waiting for input
        listen.start();
        } catch (Exception e) {
            System.out.println("error " + e.getMessage());
        }
        String line = "";
        while (!line.equals("Bye")) { //loop forever while the disconnect command isn't sent
            try {
                line = input.readLine();
                out.writeUTF(line);
            } catch (IOException i) {
                System.out.println(i);
                break;
            }
        }
        try {
            input.close();
            out.close();
            socket.close();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) 
	{ 
            try{
            //String address = args[0];
            //int port = Integer.parseInt(args[1]);
            String address = "localhost"; //for manual testing in netbeans
            int port = 6969;
            MyClient client = new MyClient(address, port); 
            } catch (Exception i) {
                System.out.println(i.getMessage());
                i.printStackTrace();
            }
	}
    
    public class ClientListen extends Thread { //this class lets the client program listen for data from the server while input is being awaited, so output isn't blocked. this has an added benefit of chat messages being buffered for output while the client inputs their username
        private Socket socket;
        private BufferedReader input;
        
        public ClientListen(Socket s) throws IOException{
            this.socket = s;
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        @Override
        public void run(){
            try {
                while(true){ //while still connected, print everything received to the client console
                    String serverResponse = input.readLine();
                    System.out.println(serverResponse);
                }
            } catch (IOException e) {
                //e.printStackTrace(); //debug code
                System.out.println("Disconnected."); //when the client disconnects, this will be called
            } finally {
                try {
                    input.close();
                } catch (Exception e) {
                    System.out.println("Disconnected weirdly.");
                }
            }
        }
    }
}
