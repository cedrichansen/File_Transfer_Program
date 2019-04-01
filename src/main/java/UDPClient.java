import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class UDPClient {


    DatagramSocket socket;
    InetAddress address;
    int port;
    short windowSize = 1;
    int MAX_WINDOW_SIZE = (int)Math.pow(DataPacket.BLOCKNUMSIZE, 2) -1;

    boolean lastPacketsSentSuccessFully = true;
    int packetsSuccessfullySent = 0;

    static List<String> filesListInDir = new ArrayList<String>();


    public UDPClient(String ip, int port) throws UnknownHostException, SocketException {
        address = InetAddress.getByName(ip);
        this.port = port;
        socket = new DatagramSocket(this.port);
    }


    public void sendFile(String filePath) throws IOException {

        filePath = filePath.replace("~", System.getProperty("user.home"));


        File f = new File(filePath);

        if (f.isDirectory()) {
            //zip file

            if (filePath.charAt(filePath.length()-1) == '/') {
                filePath = filePath.substring(0, filePath.length());
            }

            String zipFilePath = filePath + ".zip";
            filePath = zipFilePath;
            zipDirectory(f, zipFilePath);
        }


        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);


        //sending 0 as the blockNum for the WRQ is what is required by the TFTP protocol
        // the 0 here never actually gets written or sent, but we check that the ack
        // received is indeed a 0 (all done inside the method)
        DataPacket WRQ_Packet = DataPacket.createWrqPacket(fileName);
        sendWRQPacket(WRQ_Packet.getBytes(), 0);

        System.out.println("Successfully connected!");

        //create byte container to send the file
        //size of the buffer is equivalent to the size of the file in bytes
        byte[] fileData = new byte[(int) (new File(filePath).length())];
        FileInputStream fs = new FileInputStream(filePath);
        fs.read(fileData);
        fs.close();


        int numPackets = roundUp(fileData.length, DataPacket.DATASIZE);
        ArrayList<byte []> messages = splitUpBytes(numPackets, fileData);

        int successMultiplier = 1;

        if (Main.SLIDING_WINDOWS) {
            successMultiplier = 2;
        }

        long start = System.currentTimeMillis();

        while (packetsSuccessfullySent < messages.size()){
            //determine how many packets to send
            if (lastPacketsSentSuccessFully && windowSize<MAX_WINDOW_SIZE) {
                windowSize *= successMultiplier;
            }

            if (!lastPacketsSentSuccessFully) {
                windowSize /= successMultiplier;
            }

            sendDataPackets(messages, packetsSuccessfullySent);

            //increment packetsSuccessFullySent by appropriate ammount only if all frames were received by server
            if (lastPacketsSentSuccessFully) {
                packetsSuccessfullySent += windowSize;
                System.out.print("\rCurrent window size: " + windowSize + " Data Sent: " + packetsSuccessfullySent + "/" + numPackets );
            } else {
                //packet not successfully sent, so resend the same info but window size will now be reduced
                System.out.print("\rShrinking window size");
            }


        }

        long time = (System.currentTimeMillis()-start);
        System.out.println("\nTotal time to send " + ((int) (new File(filePath).length())) + " bytes : " + time + " ms");
        System.out.println("Throughput: " + ((((int) (new File(filePath).length()))/time) * 125));



        if (filePath.contains(".zip")) {
            System.out.println("Removing temporary zip file");
            (new File(filePath)).delete();
        }

        System.out.println("\nSuccessfully sent the file");


    }


    public void sendDataPackets(ArrayList<byte []> fileData, int startIndex) {


        if (startIndex + windowSize > fileData.size()) {
            windowSize = (short)(fileData.size() -startIndex);
        }

        DatagramPacket [] packetsToBeSent = new DatagramPacket[windowSize];

        for (int i = 0; i<windowSize; i++) {
            DataPacket data = DataPacket.createDataPacket(packetsSuccessfullySent+i, windowSize, fileData.get(startIndex+i));
            byte [] dataBytes = data.getBytes();

            DatagramPacket msg = new DatagramPacket(dataBytes, dataBytes.length, address, port);
            packetsToBeSent[i] = msg;
        }


        boolean packetsSuccessfullySent = false;
        // the response which will acknowledge all packets have been delivered
        byte[] resp = new byte[DataPacket.ACKSIZE_PACKET_SIZE];
        DatagramPacket response = new DatagramPacket(resp, resp.length, address, port);
        
        //try sending the udp packet until it successfully got an acknowledgement from server
         do {

            try {
                for (int i =0; i<packetsToBeSent.length; i++) {
                    if (packetsToBeSent[i] != null) {
                        socket.send(packetsToBeSent[i]);
                    }
                }
                //
                socket.setSoTimeout(5000);
                socket.receive(response);

                //response that we received has the correct blocknum, so the server received the data properly
                DataPacket ack = DataPacket.readPacket(response.getData());

                //the server will reply with what it thinks the current window size is. If the window size matches, then
                // we know the server received all the data
                if (ack.windowSize == windowSize) {
                    packetsSuccessfullySent = true;
                    lastPacketsSentSuccessFully = true;
                } else {
                    lastPacketsSentSuccessFully = false;
                    break;
                }

            } catch (IOException e) {
                System.out.println("\nTimeout - Resending");
                lastPacketsSentSuccessFully = false;
            }

        } while (!packetsSuccessfullySent);

    }


    public void sendWRQPacket(byte[] data, int blockNum) {

        boolean packetSuccessfullySent = false;
        DatagramPacket msg = new DatagramPacket(data, data.length, address, port);

        // the response
        byte[] resp = new byte[DataPacket.ACKSIZE_PACKET_SIZE];
        DatagramPacket response = new DatagramPacket(resp, resp.length, address, port);

        //try sending the udp packet until it successfully got an acknowledgement from server
        do {

            try {
                socket.send(msg);
                socket.setSoTimeout(2000);
                socket.receive(response);

                //response that we received has the correct blocknum, so the server received the data properly
                DataPacket ack = DataPacket.readPacket(response.getData());
                if (ack.blockNum == blockNum) {
                    packetSuccessfullySent = true;
                }

            } catch (IOException e) {
                System.out.println("\nTimeout - Resending");
            }

        } while (!packetSuccessfullySent);



    }



    //return a list, which is essentially all of the packets that are needed to send.
    public ArrayList<byte []> splitUpBytes(int numPackets, byte [] fileData) {
        ArrayList<byte []> messages = new ArrayList<>();

        //Populate and send the byte arrays
        for (int i = 0; i < numPackets; i++) {
            if (i == numPackets - 1) {
                //this is the last byte array to be read, and likely isnt 512 bytes
                byte[] packet = Arrays.copyOfRange(fileData, i * DataPacket.DATASIZE, fileData.length + 1);
                messages.add(packet);

            } else {
                //512 byte array
                byte[] packet = Arrays.copyOfRange(fileData, i * DataPacket.DATASIZE, (i + 1) * DataPacket.DATASIZE);
                messages.add(packet);
            }
        }

        return messages;
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


    private static void zipDirectory(File dir, String zipDirName) {
        try {
            populateFilesList(dir);
            //now zip files one by one
            //create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(zipDirName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            for(String filePath : filesListInDir){
                System.out.println("Zipping "+filePath);
                //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()+1, filePath.length()));
                zos.putNextEntry(ze);
                //read the file and write to ZipOutputStream
                FileInputStream fis = new FileInputStream(filePath);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        for(File file : files){
            if(file.isFile()) filesListInDir.add(file.getAbsolutePath());
            else populateFilesList(file);
        }
    }

}
