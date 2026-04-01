import java.net.*;
import java.io.*;

public class TestClient {
    // initialize socket and input output streams
    private Socket socket            = null;
    private DataInputStream  input   = null;
    private DataOutputStream out     = null;

    // constructor to put ip address and port
    public TestClient(String address, int port, String message)
    {
        // establish a connection
        try
        {
            socket = new Socket(address, port);
            System.out.println("Connected to server");

            // sends output to the socket
            out    = new DataOutputStream(socket.getOutputStream());
            
            // send message
            out.writeUTF(message);
            System.out.println("Message sent: " + message);
        }
        catch(UnknownHostException u)
        {
            System.out.println(u);
        }
        catch(IOException i)
        {
            System.out.println(i);
        }

        // close the connection
        try
        {
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Connection closed");
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    public static void main(String[] args)
    {
        if (args.length < 2) {
            System.out.println("Usage: java TestClient <ip_address> <message>");
            return;
        }
        new TestClient(args[0], 7775, args[1]);
    }
}
