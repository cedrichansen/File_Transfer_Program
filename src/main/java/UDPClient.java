import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UDPClient {


    DatagramSocket socket;
    InetAddress address;
    int port;


    public UDPClient(String ip, int port) throws UnknownHostException, SocketException {
        address = InetAddress.getByName(ip);
        this.port = port;
        socket = new DatagramSocket(port);
    }


    public void sendFile(String filePath) throws IOException {

        filePath = filePath.replace("~", System.getProperty("user.home"));

        long fileSizeL = (new File(filePath).length());

        //send to the server how big the file will be
        byte[] fileSize = longToBytes(fileSizeL);
        sendPacket(fileSize);

        //create byte container to send the file
        //size of the buffer is equivalent to the size of the file in bytes
        byte[] data = new byte[(int) fileSizeL];
        FileInputStream fs = new FileInputStream(filePath);
        fs.read(data);
        fs.close();


        //only send 512 bytes at a time as per IETF RFC 1350, and wait for ack
        //packet less than 512 bytes signals the end of transmission

        int numPackets = roundUp(data.length, Main.PACKET_SIZE);

        ProgressBar pb = new ProgressBar("Sent Data", numPackets);

        pb.start();

        //Populate and send the byte arrays
        for (int i = 0; i < numPackets; i++) {
            if (i == numPackets - 1) {
                //this is the last byte array to be read, and likely isnt 512 bytes
                byte[] packet = Arrays.copyOfRange(data, i * Main.PACKET_SIZE, data.length + 1);
                sendPacket(packet);

            } else {
                //512 byte array
                byte[] packet = Arrays.copyOfRange(data, i * Main.PACKET_SIZE, (i + 1) * Main.PACKET_SIZE);
                sendPacket(packet);
            }

            pb.step();

        }

        pb.stop();

        System.out.println("Successfully sent the file");

    }


    public void sendPacket(byte[] data) {

        boolean packetSuccessfullySent = false;
        DatagramPacket msg = new DatagramPacket(data, data.length, address, port);

        // the response
        byte[] resp = new byte[1];
        DatagramPacket response = new DatagramPacket(resp, resp.length, address, port);
        
        //try sending the udp packet until it successfully got an acknowledgement from server
         do {

            try {
                socket.send(msg);
                socket.setSoTimeout(2000);
                socket.receive(response);

                packetSuccessfullySent = true;

            } catch (IOException e) {
                System.out.println("\nTimeout - Resending");
            }

        } while (!packetSuccessfullySent);


    }


    public void close() {
        socket.close();
    }



    /*
    Helper functions
     */

    public static int roundUp(int num, int divisor) {
        return (num + divisor - 1) / divisor;
    }

    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }


}
