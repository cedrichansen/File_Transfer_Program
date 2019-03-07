import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;


public class UDPServer {

    DatagramSocket socket;
    String FILE_LOCATION = "/home/chansen/Desktop/test.txt";



    public UDPServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }


    public boolean acceptFile() throws FileNotFoundException {

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
        }

        byte [] fileData = new byte [(int)fileSize];
        FileOutputStream fos = new FileOutputStream(FILE_LOCATION);
        int numPackets = UDPClient.roundUp(fileData.length , Main.PACKET_SIZE);


        for (int i=0; i<numPackets; i++) {
            if (i == numPackets-1) {
                //reading the last packet
                byte [] fileDataBuf = new byte [(int)fileSize- (i*Main.PACKET_SIZE)];
                DatagramPacket data = new DatagramPacket(fileDataBuf, fileDataBuf.length);
                try {
                    socket.receive(data);
                    //respond to the client by echoing the data
                    socket.send(data);

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
                    //respond to the client by echoing the data
                    socket.send(data);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.arraycopy(fileDataBuf, 0, fileData, i*Main.PACKET_SIZE, fileDataBuf.length);
            }
        }

        try {
            fos.write(fileData);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
