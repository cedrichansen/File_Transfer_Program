import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;



public class UDPServer {

    DatagramSocket socket;
    int numPacketsReceived = 0;


    public UDPServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }


    public boolean acceptFile(String filePath) throws FileNotFoundException {

        String fileLocation = filePath.replace("~", System.getProperty("user.home"));

        System.out.println("Waiting for a connection...");

        try {
            //try to receive a WRQ
            DataPacket clientrequest = receiveWRQ();

            //the message String here is the filepath
            fileLocation = fileLocation + clientrequest.messageStr;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cannot properly receive WRQ packet properly");
        }

        System.out.println("Connection established!");

        ArrayList<Byte> fileBytes = new ArrayList<>();
        ArrayList<DataPacket> lastPacketsReceived = new ArrayList<>();

        int numPacketsProcessed = 0;


        //while true. i is simply used as a counter for an eventual loading bar
        while (true) {

            //try to receive a dataPacket
            ArrayList<DataPacket> incomingPackets = receiveDataPackets();


            if (fileBytes.size() == 0) {
                //this is the first Datapacket list we receive, so we hold on until we receive the next packet.
                //this insures that the client has successfully received the ack.
                lastPacketsReceived = incomingPackets;
            } else {
                //we have received at least one packet of data, so we add the previous packet to the list of bytes
                for (DataPacket d:lastPacketsReceived) {
                    //add the data to files data
                    for (byte b : d.data) {
                        fileBytes.add(b);
                    }
                }


                numPacketsProcessed += lastPacketsReceived.get(0).windowSize;
                System.out.print("\rData Received: " + numPacketsProcessed);

                //make sure to add the data of the previously held block
                lastPacketsReceived = incomingPackets;

                //if its the last packet, break
                if (lastPacketsReceived.get(lastPacketsReceived.size()-1).getBytes().length < DataPacket.DATA_PACKET_SIZE) {
                    //the data doesnt reach the end of the packet, so add the last packet
                    //add the data to files data
                    for (DataPacket d:lastPacketsReceived) {
                        //add the data to files data
                        for (byte b : d.data) {
                            fileBytes.add(b);
                        }
                    }
                    numPacketsProcessed += lastPacketsReceived.get(0).windowSize;
                    System.out.print("\rData Received: " + numPacketsProcessed);
                    //done receiving data
                    break;
                }
            }
        }

        byte[] fileBytesArr = new byte[fileBytes.size()];

        for (int i = 0; i < fileBytes.size(); i++) {
            fileBytesArr[i] = fileBytes.get(i);
        }

        writeFile(fileLocation, fileBytesArr);

        return true;
    }


    public ArrayList<DataPacket> receiveDataPackets() {

        ArrayList<DataPacket> packets = new ArrayList<>();

        //prepare to receive a number of packets of data
        byte[] dataBytes = new byte[DataPacket.DATA_PACKET_SIZE];
        DatagramPacket msg = new DatagramPacket(dataBytes, dataBytes.length);
        try {
            socket.receive(msg);
        } catch (IOException e) {
            System.out.println("Problem receiving from socket 1 ");
            e.printStackTrace();
        }

        //look at how much data was actually received... This is important because the last
        //message is less than 512 bytes
        int numDataBytes = msg.getLength();
        byte[] receivedBytes = new byte[numDataBytes];
        System.arraycopy(dataBytes, 0, receivedBytes, 0, numDataBytes);

        //create a data packet from which to extract the fileData
        DataPacket data = DataPacket.readPacket(receivedBytes);
        packets.add(data);

        System.out.println("\rReceived packets: " + numPacketsReceived++);

        //we expect to receive windowSize number of packets
        short windowSize = data.windowSize;


        //skip first index because that is the initial packet we have already received
        for (int i = 1; i < windowSize; i++) {
            try {
                socket.setSoTimeout(5000);
                socket.receive(msg);
                System.out.println("\rReceived packets: " + numPacketsReceived++);
            } catch (IOException e) {
                System.out.println("Problem receiving from socket loop");
                e.printStackTrace();
            }
            packets.add(DataPacket.readPacket(msg.getData()));

        }


        if (Main.DROP_PACKETS) {
            Random r = new Random();
            if (r.nextFloat() < Main.DROP_PACKET_RATE) {
                //exit preemptively. By never sending ack, client must resend the data
                return packets;
            }
        }

        //Receiving data, create an ack packet, and send back to client
        DataPacket ack = DataPacket.createAckPacket(data.blockNum, (short) packets.size());
        byte[] ackBytes = ack.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, msg.getAddress(), msg.getPort());
        try {
            socket.send(ackPacket);
        } catch (IOException e) {
            System.out.println("Problem with sending the ack");
        }

        //sort packets so they are in order of blockNum
        Collections.sort(packets);

        return packets;
    }


    public DataPacket receiveWRQ() throws IOException {
        //used only to receive a WRQ from someone who wants to write on system

        byte[] dataBytes = new byte[DataPacket.WRQ_PACKET_SIZE];
        DatagramPacket msg = new DatagramPacket(dataBytes, dataBytes.length);
        socket.receive(msg);

        //create a data packet from which to extract the fileData
        DataPacket data = DataPacket.readPacket(msg.getData());

        //someone is trying to write data... we must reply with an ack with blockNum = 0;
        DataPacket ack = DataPacket.createAckPacket(0,(short)1);
        byte[] ackBytes = ack.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, msg.getAddress(), msg.getPort());
        socket.send(ackPacket);

        return data;
    }


    public void close() {
        socket.close();
    }


    public static boolean writeFile(String filePath, byte[] fileBytes) {

        try {

            File fileBeingReceived = new File(filePath);

            try {
                fileBeingReceived.createNewFile();
            } catch (IOException e) {
                System.out.println("Filepath is invalid... Please try again");
                return false;
            }
            FileOutputStream fos = new FileOutputStream(fileBeingReceived, false);

            fos.write(fileBytes);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Something wrong with writing file");
            return false;
        }

        if (filePath.contains(".zip")) {
            try {
                ZipFile zip = new ZipFile(filePath);
                zip.extractAll(filePath.replace(".zip", ""));

            } catch (ZipException e) {
                e.printStackTrace();
            }

        }

        System.out.println("\nFile written successfully! File is located at: " + filePath.replace(".zip", ""));


        return true;
    }


}
