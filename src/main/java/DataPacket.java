import java.nio.ByteBuffer;

public class DataPacket {

        /*


        Some Modifications have been made from the original version of tftp. 

     2 bytes   string (128 bytes)   
      -----------------------
     | Opcode |  Filename   | 
      -----------------------

             Figure 5-1: RRQ/WRQ packet


       2 bytes     4 bytes   512 bytes MAX
       ----------------------------------
      | Opcode |   Block #  |   Data     |
       ----------------------------------

         Figure 5-2: DATA packet



         2 bytes     4 bytes
         ---------------------
        | Opcode |   Block #  |
         ---------------------

          Figure 5-3: ACK packet


       2 bytes     2 bytes      string(128 bytes)   
       ----------------------------------------------
      | Opcode |  ErrorCode |         ErrMsg        |
       ----------------------------------------------

         Figure 5-4: ERROR packet


     */

    byte [] data;
    short opCode;
    int blockNum;

    //128 will be max size (in chars) for filename AND errMessage strings
    byte [] message;
    String messageStr;
    // only supported mode will be binary (octect) transmission. This will be default
    //byte [] mode;



    //OpCodes to be used
    final static short RRQ = 1;
    final static short WRQ = 2;
    final static short DATA = 3;
    final static short ACK = 4;
    final static short ERROR = 5;

    //sizes for various fields in the packets
    final static short OPCODESIZE = 2;
    final static short FILENAMESIZE = 128;
    final static short BLOCKNUMSIZE = 4;
    final static short ERRCODESIZE = 2;
    final static short ERRMESSAGESIZE = 128;
    final static short DATASIZE = 512;


    //Constant values for packet sizes
    final static int RRQ_PACKET_SIZE = OPCODESIZE + FILENAMESIZE;
    final static int WRQ_PACKET_SIZE = OPCODESIZE + FILENAMESIZE;
    final static int ACKSIZE_PACKET_SIZE = OPCODESIZE + BLOCKNUMSIZE;
    final static int ERRSIZE_PACKET_SIZE = OPCODESIZE + ERRCODESIZE + ERRMESSAGESIZE;
    final static int DATA_PACKET_SIZE = OPCODESIZE + BLOCKNUMSIZE + DATASIZE;

    //Data packets are the biggest of all the types of Datapackets
    final static int MAXDATASIZE=DATA_PACKET_SIZE;
     


    // used for RRQ/WRQ packet AND Error packet
    private DataPacket(short opCode, String messageStr) {
        this.message = new byte [128];
        this.opCode = opCode;
        System.arraycopy(messageStr.getBytes(), 0, this.message, 0, messageStr.getBytes().length);
        this.messageStr = trimTrailingZeros(this.message);
    }


    //used to send data
    private DataPacket(short opCode, int blockNum, byte [] data) {
        this.opCode = opCode;
        this.blockNum = blockNum;
        this.data = data;
    }

    //used for ACK's
    private DataPacket(short opCode, int blockNum) {
        this.opCode = opCode;
        this.blockNum = blockNum;
    }





    public static DataPacket createRrqPacket(String message) {
        return new DataPacket(RRQ, message);
    }

    public static DataPacket createWrqPacket(String message) {
        return new DataPacket(WRQ, message);
    }

    public static DataPacket createAckPacket(int blockNum) {
        return new DataPacket(ACK, blockNum);
    }

    public static DataPacket createDataPacket(int blockNum, byte [] data) {
        return new DataPacket(DATA, blockNum, data);
    }

    public static DataPacket createErrPacket(String message) {
        return new DataPacket(ERROR, message);
    }



    //this function is used to read an incoming byte [] and extract the appropriate packet data
    public static DataPacket readPacket(byte [] bytes) {

        ByteBuffer bb = ByteBuffer.allocate(MAXDATASIZE);
        bb.put(bytes);

        short opcode = bb.getShort(0);

        if (opcode == RRQ) {
            return recoverRRQPacket(bytes);
        } else if (opcode == WRQ) {
            return recoverWRQPacket(bytes);
        } else if (opcode == DATA) {
            return recoverDATAPacket(bytes);
        } else if (opcode == ACK) {
            return recoverACKPacket(bytes);
        } else if (opcode == ERROR) {
            return recoverERRORPacket(bytes);
        }

        return createErrPacket("Something went wrong with reading the packets...");

    }

    private static DataPacket recoverRRQPacket(byte [] bytes) {

        ByteBuffer bb = ByteBuffer.allocate(RRQ_PACKET_SIZE);
        bb.put(bytes);
        //short opCode = bb.getShort(0);
        byte [] fileNameBytes = new byte[FILENAMESIZE];
        System.arraycopy(bytes, OPCODESIZE, fileNameBytes, 0, FILENAMESIZE);
        String fileName = new String(fileNameBytes);
        return createRrqPacket(fileName);

    }

    private static DataPacket recoverWRQPacket(byte [] bytes) {

        ByteBuffer bb = ByteBuffer.allocate(WRQ_PACKET_SIZE);
        bb.put(bytes);
        //short opCode = bb.getShort(0);
        byte [] fileNameBytes = new byte[FILENAMESIZE];
        System.arraycopy(bytes, OPCODESIZE, fileNameBytes, 0, FILENAMESIZE);
        String fileName = new String(fileNameBytes);
        return createWrqPacket(fileName);
    }

    private static DataPacket recoverDATAPacket(byte [] bytes) {

        ByteBuffer bb = ByteBuffer.allocate(DATA_PACKET_SIZE);
        bb.put(bytes);
        //short opCode = bb.getShort(0);
        int blockNum = bb.getInt(OPCODESIZE);
        byte [] dataBytes = new byte[DATASIZE];
        System.arraycopy(bytes, OPCODESIZE+BLOCKNUMSIZE, dataBytes, 0, DATASIZE);
        return createDataPacket(blockNum, dataBytes);

    }

    private static DataPacket recoverACKPacket(byte [] bytes) {
        ByteBuffer bb = ByteBuffer.allocate(ACKSIZE_PACKET_SIZE);
        bb.put(bytes);
        //short opCode = bb.getShort(0);
        int blockNum = bb.getInt(OPCODESIZE);
        return createAckPacket(blockNum);

    }

    private static DataPacket recoverERRORPacket(byte [] bytes) {
        ByteBuffer bb = ByteBuffer.allocate(ERRSIZE_PACKET_SIZE);
        bb.put(bytes);
        //short opCode = bb.getShort(0);
        byte [] errorMessageName = new byte[FILENAMESIZE];
        System.arraycopy(bytes, OPCODESIZE, errorMessageName, 0, FILENAMESIZE);
        String errorMessage = new String(errorMessageName);
        return createErrPacket(errorMessage);

    }










    //this function is used to get the correct byte [] representation for the current packet
    public byte [] getBytes() {

        if (this.opCode == RRQ) {
            return this.getRRQBytes();
        } else if (this.opCode == WRQ) {
            return this.getWRQBytes();
        } else if (this.opCode == DATA) {
            return this.getDataBytes();
        } else if (this.opCode == ACK) {
            return this.getAckBytes();
        } else if (this.opCode == ERROR) {
            return this.getErrorBytes();
        }

        //should never reach this
        return null;
    }




    private byte [] getRRQBytes(){
        ByteBuffer bb = ByteBuffer.allocate(RRQ_PACKET_SIZE);
        bb.putShort(this.opCode);
        bb.put(this.message);
        bb.flip();
        return bb.array();
    }

    private byte [] getWRQBytes() {
        ByteBuffer bb = ByteBuffer.allocate(WRQ_PACKET_SIZE);
        bb.putShort(this.opCode);
        bb.put(this.message);
        bb.flip();
        return bb.array();
    }

    private byte [] getDataBytes() {
        ByteBuffer bb = ByteBuffer.allocate(DATA_PACKET_SIZE);
        bb.putShort(this.opCode);
        bb.putInt(this.blockNum);
        bb.put(this.data);
        bb.flip();
        return bb.array();

    }

    private byte [] getAckBytes() {
        ByteBuffer bb = ByteBuffer.allocate(ACKSIZE_PACKET_SIZE);
        bb.putShort(this.opCode);
        bb.putInt(this.blockNum);
        bb.flip();
        return bb.array();
    }

    private byte [] getErrorBytes() {
        ByteBuffer bb = ByteBuffer.allocate(ERRSIZE_PACKET_SIZE);
        bb.putShort(this.opCode);
        bb.put(this.message);
        bb.flip();
        return bb.array();
    }


    private String trimTrailingZeros(byte [] messageBytes) {

        int count =0;
        for (;count<messageBytes.length; count++) {
            if (messageBytes[count] == 0) {
                break;
            }
        }

        byte [] trimmedFileName = new byte [count];

        System.arraycopy(messageBytes, 0, trimmedFileName, 0, count);

        return new String(trimmedFileName);

    }



    @Override
    public String toString() {
        return "Opcode: " + this.opCode + " BlockNum: " + blockNum + " Message str: " + this.messageStr;
    }
}
