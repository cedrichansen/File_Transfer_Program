import javax.xml.crypto.Data;
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
    public DataPacket(short opCode, String messageStr) {
        this.message = new byte [128];
        this.opCode = opCode;
        System.arraycopy(messageStr.getBytes(), 0, this.message, 0, messageStr.getBytes().length);
    }


    //used to send data
    public DataPacket(short opCode, int blockNum, byte [] data) {
        this.opCode = opCode;
        this.blockNum = blockNum;
        this.data = data;
    }

    //used for ACK's
    public DataPacket(short opCode, int blockNum) {
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
        bb.put(bytes[0]);
        bb.put(bytes[0]);

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

    public static DataPacket recoverRRQPacket(byte [] bytes) {

        ByteBuffer bb = ByteBuffer.allocate(RRQ_PACKET_SIZE);
        bb.put(bytes,OPCODESIZE, FILENAMESIZE);
        byte [] fileNameBytes = bb.array();
        String fileName = new String(fileNameBytes);
        return createRrqPacket(fileName);

    }

    public static DataPacket recoverWRQPacket(byte [] bytes) {

        ByteBuffer bb = ByteBuffer.allocate(WRQ_PACKET_SIZE);
        bb.put(bytes, OPCODESIZE, FILENAMESIZE);
        byte [] fileNameBytes = bb.array();
        String fileName = new String(fileNameBytes);
        return createWrqPacket(fileName);
    }

    public static DataPacket recoverDATAPacket(byte [] bytes) {

        ByteBuffer bb = ByteBuffer.allocate(DATA_PACKET_SIZE);
        bb.put(bytes, OPCODESIZE, BLOCKNUMSIZE);
        int blockNum = bb.getInt();
        bb.clear();
        byte [] data = new byte[DATASIZE];
        System.arraycopy(bytes, OPCODESIZE+BLOCKNUMSIZE, data, 0,DATASIZE);
        return createDataPacket(blockNum, data);

    }

    public static DataPacket recoverACKPacket(byte [] bytes) {
        ByteBuffer bb = ByteBuffer.allocate(ACKSIZE_PACKET_SIZE);
        bb.put(bytes, OPCODESIZE, BLOCKNUMSIZE);
        int blockNum = bb.getInt();
        bb.clear();
        return createAckPacket(blockNum);

    }

    public static DataPacket recoverERRORPacket(byte [] bytes) {
        ByteBuffer bb = ByteBuffer.allocate(ERRSIZE_PACKET_SIZE);
        bb.put(bytes, OPCODESIZE, ERRCODESIZE);
        //Currently not using errCode for anything in this project, but could be implemented if very robust
        //protocol is implemented
        short errCode = bb.getShort();
        bb.clear();
        bb.put(bytes, OPCODESIZE + ERRCODESIZE, ERRMESSAGESIZE);
        byte [] errorMessageBytes = bb.array();
        String errorMessage = new String(errorMessageBytes);
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


    
    //TODO: Implement following functions

    public byte [] getRRQBytes(){
        byte [] rrqBytes = new byte[RRQ_PACKET_SIZE];

        return rrqBytes;
    }

    public byte [] getWRQBytes() {
        byte [] wrqBytes = new byte[WRQ_PACKET_SIZE];
        return wrqBytes;
    }

    public byte [] getDataBytes() {
        byte [] dataBytes = new byte[DATA_PACKET_SIZE];
        return dataBytes;
    }

    public byte [] getAckBytes() {
        byte [] ackBytes = new byte[ACKSIZE_PACKET_SIZE];
        return ackBytes;
    }

    public byte [] getErrorBytes() {
        byte [] errorBytes = new byte[ERRSIZE_PACKET_SIZE];
        return errorBytes;
    }








    public static byte [] convertIntToByteArray(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(value);
        return buffer.array();
    }
    public static byte[] convertShortToByteArray(short value) {
        byte[] bytes = new byte[2];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putShort(value);
        return buffer.array();
    }



}
