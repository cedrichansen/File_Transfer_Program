import me.tongfei.progressbar.ProgressBar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.ArrayList;


public class UDPServer {

    DatagramSocket socket;

    public UDPServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }

    
    public boolean acceptFile(String filePath) throws FileNotFoundException {

        String fileLocation = filePath.replace("~", System.getProperty("user.home"));

        System.out.println("Waiting for a connection...");

        ArrayList<Byte> fileBytes = new ArrayList<>();

        DataPacket lastPacketReceived = DataPacket.createErrPacket("DataPacket never initialized");
        boolean receivedData = false;

        //while true. i is simply used as a counter for an eventual loading bar
        for (int i=0; ; i++) {

            try {
                DataPacket message = receivePacket();

                if (message.opCode == DataPacket.DATA) {


                    if (!receivedData) {
                        //this is the first Datapacket we receive, so we hold on until we receive the next packet.
                        //this insures that the client has succesfully received the ack.
                        receivedData = true;
                        lastPacketReceived = message;
                    } else {
                        //we have received at least one packet of data, so we add the previous packet to the list of bytes
                        byte [] previousPacketData = lastPacketReceived.data;

                        //add the data to files data
                        for (byte b : previousPacketData) {
                            fileBytes.add(b);
                        }

                        //make sure to add the data of the previously held block
                        lastPacketReceived = message;

                    }


                    //if its the last packet, break
                    if (message.data[DataPacket.DATASIZE] == 0) {
                        //the data doesnt reach the end of the packet, so add the last packet

                        byte [] previousPacketData = lastPacketReceived.data;

                        //add the data to files data
                        for (byte b : previousPacketData) {
                            fileBytes.add(b);
                        }

                        //done receiving data
                        break;
                    }
                }  else if (message.opCode == DataPacket.WRQ) {
                    //should only ever occur once
                    fileLocation = fileLocation + message.messageStr;
                }

            } catch (IOException e) {
                System.out.println("IO Exception, probably something with sockets...");
                e.printStackTrace();
                return false;
            }

        }

        byte [] fileBytesArr = new byte [fileBytes.size()];

        for (int i =0; i<fileBytes.size(); i++) {
            fileBytesArr[i] = fileBytes.get(i);
        }

        writeFile(fileLocation, fileBytesArr);


        return true;
    }


    public DataPacket receivePacket() throws IOException {
        //prepare to receive a packet of data
        byte [] dataBytes = new byte [DataPacket.DATA_PACKET_SIZE];
        DatagramPacket msg = new DatagramPacket(dataBytes, dataBytes.length);
        socket.receive(msg);

        //create a data packet from which to extract the fileData
        DataPacket data = DataPacket.readPacket(msg.getData());

        //create a reply packet, and send back to client
        DataPacket ack = DataPacket.createAckPacket(data.blockNum);
        DatagramPacket ackPacket = new DatagramPacket(ack.data, ack.data.length, msg.getAddress(), msg.getPort());
        socket.send(ackPacket);
        return data;
    }



    public void close() {
        socket.close();
    }



    public static boolean writeFile(String filePath, byte [] fileBytes) {

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

        System.out.println("File written successfully! File is located at: " + filePath);


        return true;
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

}
