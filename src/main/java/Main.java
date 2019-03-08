import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;


/*

Write a file transfer program. To demonstrate, you'll need a client and a server program, doing the obvious.
-The server awaits connections.
-A client connects, sends the name of a file or directory it wants to upload.
-The client sends the file (or if a directory, recursively) and the server stores it.

Wherever applicable, use the commands and protocol for TFTP (IETF RFC 1350), with the following modifications:

-A client only sends files, never receives.
-Support only binary (octet) transmission.
-Support a command line argument specifiying whether packets are IPv4 vs IPv6 UDP datagrams
-Support a command line argument specifying to use TCP-style sliding windows rather than the sequential acks used in TFTP. To implement this, you may need to design and use additional packet header information than that in TFTP, using the IETF 2347 TFTP Options Extension where applicable.
-Support a command line argument controlling whether to pretend to drop 1 percent of the packets;

Create a web page showing throughput across varying conditions (V4 vs V6; sequential vs windowed acks; drops vs no drops)


 */

/*
some good ressources

http://www.cs.rpi.edu/courses/fall02/netprog/notes/javaudp/javaudp.pdf
https://tools.ietf.org/html/rfc1350



 */


public class Main {

    //toggle IPv4 vs IPv6. If no argument is provided, IPv6 is used by default
    public static boolean IPv4 = false;
    public static boolean IPV6 = true;

    //toggle TCP-style sliding windows vs sequential acks used in TFTP. If no argument is provided, Sequential acks will be default
    public static boolean SLIDING_WINDOWS = false;
    public static boolean SEQUENTIAL_ACKS = true;

    //toggle if we pretend to drop packets
    public static boolean DROP_PACKETS = false;

    //rate at which we will pretend to drop packets
    public static float DROP_PACKET_RATE = 0.01f;

    final static int PORT = 2689;

    public static int PACKET_SIZE = 512;


    public static void main (String [] args) {

        processArgs(args);


        printFlags();

        int role = Integer.parseInt(getInput("Type 1 for client, 2 for server"));


        if (role == 1) {
            String ip = getInput("Enter server IP address: ");
            String file = getInput("Enter the file you would like to send: ");
            try {
                UDPClient client = new UDPClient(ip, PORT);
                client.sendFile(file);
                client.close();

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        } else if (role == 2) {

            try {
                UDPServer server = new UDPServer(PORT);
                boolean result = server.acceptFile(getInput("Enter the destination filepath"));
                printExternalIP();
                server.close();


            } catch (SocketException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }



    }





    public static void processArgs(String [] args) {
        for (String s : args) {
            if (s.equals("4")) {
                IPv4 = true;
                IPV6 = false;
            } else if (s.equals("6")) {
                IPV6 = true;
                IPv4 = false;
            } else if (s.equals("-w")) {
                SLIDING_WINDOWS = true;
                SEQUENTIAL_ACKS = false;
            } else if (s.equals("-s")) {
                SEQUENTIAL_ACKS = true;
                SLIDING_WINDOWS = false;
            } else if (s.equals("-d")) {
                DROP_PACKETS = true;
            }
        }
    }


    //return input for a given message
    public static String getInput(String message) {
        System.out.println(message);
        return (new Scanner(System.in)).nextLine();
    }

    public static void printFlags() {

        System.out.println("IPv4: " + IPv4);
        System.out.println("IPv6: " + IPV6);
        System.out.println("Sliding windows: " + SLIDING_WINDOWS);
        System.out.println("Sequential acks: " + SEQUENTIAL_ACKS);
        System.out.println("Drop packets: " + DROP_PACKETS);
    }



    public static void printExternalIP() throws UnknownHostException {

        // Find public IP address
        String systemipaddress;
        try
        {
            URL url_name = new URL("http://bot.whatismyipaddress.com");

            BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));

            // reads system IPAddress
            systemipaddress = sc.readLine().trim();
        }
        catch (Exception e)
        {
            systemipaddress = "Cannot Execute Properly";
        }
        System.out.println("Public IP Address: " + systemipaddress);
    }

}



