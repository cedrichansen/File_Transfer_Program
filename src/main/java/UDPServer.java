import me.tongfei.progressbar.ProgressBar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;


public class UDPServer {

    DatagramSocket socket;
    String FILE_LOCATION;


    public UDPServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }

    
    public boolean acceptFile(String filePath) throws FileNotFoundException {

        FILE_LOCATION = filePath.replace("~", System.getProperty("user.home"));

        System.out.println("Waiting for a connection...");

        //longs are 8 bytes, and we know we are receiving a long first
        byte [] fileSizeBytes = new byte[8];

        DatagramPacket fileSizePacket = new DatagramPacket(fileSizeBytes, fileSizeBytes.length);
        long fileSize = 0;
        try {
            socket.receive(fileSizePacket);
            fileSize = bytesToLong(fileSizePacket.getData());
            socket.send(fileSizePacket);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        byte [] fileData = new byte [(int)fileSize];
        File fileBeingReceived = new File(FILE_LOCATION);
        try {
            fileBeingReceived.createNewFile();
        } catch (IOException e) {
            System.out.println("Filepath is invalid... Please try again");
            return false;
        } 
        FileOutputStream fos = new FileOutputStream(fileBeingReceived, false);
        int numPackets = UDPClient.roundUp(fileData.length , Main.PACKET_SIZE);


        ProgressBar pb = new ProgressBar("Received data", numPackets);

        pb.start();

        for (int i=0; i<numPackets; i++) {
            if (i == numPackets-1) {
                //reading the last packet
                byte [] fileDataBuf = new byte [(int)fileSize- (i*Main.PACKET_SIZE)];
                DatagramPacket data = new DatagramPacket(fileDataBuf, fileDataBuf.length);
                try {
                    socket.receive(data);
                    //respond to the client by sending 1 byte reply
                    byte [] respArr = new byte [1];
                    respArr[0] = (byte)1;
                    DatagramPacket resp = new DatagramPacket(respArr, respArr.length, data.getAddress(), data.getPort());
                    socket.send(resp);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.arraycopy(fileDataBuf, 0, fileData, i*Main.PACKET_SIZE, fileDataBuf.length);

            } else {
                //reading 512 bytes
                byte [] fileDataBuf = new byte [Main.PACKET_SIZE];
                DatagramPacket data = new DatagramPacket(fileDataBuf, fileDataBuf.length);

                try {
                    socket.receive(data);
                    //respond to the client by sending a 1 byte reply
                    byte [] respArr = new byte [1];
                    respArr[0] = (byte)1;
                    DatagramPacket resp = new DatagramPacket(respArr, respArr.length, data.getAddress(), data.getPort());
                    socket.send(resp);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.arraycopy(fileDataBuf, 0, fileData, i*Main.PACKET_SIZE, fileDataBuf.length);
            }
            pb.step();
        }

        try {
            fos.write(fileData);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        pb.stop();

        System.out.println("File written successfully! File is located at: " + FILE_LOCATION);


        return true;
    }



    public void close() {
        socket.close();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

}
