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



    int PACKET_SIZE = 512;

    public UDPClient(String ip, int port) throws UnknownHostException, SocketException {
        address = InetAddress.getByName(ip);
        this.port = port;
        socket = new DatagramSocket(port);
    }



    public void sendFile(String fileName) throws IOException {

        long fileSizeL = (new File(fileName).length());

        //send to the server how big the file will be
        byte [] fileSize = longToBytes(fileSizeL);
        sendPacket(fileSize);


        //create byte container to send the file
        //size of the buffer is equivalent to the size of the file in bytes
        byte [] data = new byte[(int)fileSizeL];

        FileInputStream fs = new FileInputStream(fileName);

        //read the filestream into the array of bytes
        int i = 0;

        while (fs.available()!=0) {
            data[i] = (byte)fs.read();
            i++;
        }
        fs.close();


        //only send 512 bytes at a time as per IETF RFC 1350, and wait for ack
        //packet less than 512 bytes signals the end of transmission

        int numPackets = roundUp(data.length, PACKET_SIZE);

        //populate byteArrays which will be sent
        for (i = 0; i<numPackets; i++) {
            if (i == numPackets-1) {
                //this is the last byte array to be read, and likely isnt 512 bytes
                byte [] packet = Arrays.copyOfRange(data, i*PACKET_SIZE, data.length+1);
                sendPacket(packet);

            } else {
                //512 byte array
                byte [] packet = Arrays.copyOfRange(data,i*PACKET_SIZE, (i+1)*PACKET_SIZE);
                sendPacket(packet);
            }
        }

    }


    public void sendPacket(byte [] data) throws IOException{

        boolean packetSuccessfullySent = false;

        //try sending the udp packet until it successfully got an acknowledgement from server
        while (!packetSuccessfullySent) {

            DatagramPacket msg = new DatagramPacket(data, data.length, address, port);
            socket.send(msg);

            try {
                DatagramPacket response = new DatagramPacket(data, data.length);
                socket.setSoTimeout(2000);
                socket.receive(response);

                response.getData();
                packetSuccessfullySent = true;

            } catch (SocketException e) {
                System.out.println("Socket timed out");
                e.printStackTrace();
                packetSuccessfullySent =false;
            }
        }


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
