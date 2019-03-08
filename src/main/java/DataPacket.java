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
    byte [] opCode;
    byte [] blockNum;

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


    //Constant values for packet sizes
    final static int RRQSIZE = 130;
    final static int ACKSIZE = 6;
    final static int ERRSIZE = 132;

    //max data size for a data packet is 518, but the final packet will be smaller
    final static int DATASIZE = 518;
     




    // used for RRQ/WRQ packet AND Error packet
    public DataPacket(short opCode, String messageStr) {
        this.message = new byte [128];
        this.opCode = convertShortToByteArray(opCode);
        System.arraycopy(messageStr.getBytes(), 0, this.message, 0, messageStr.getBytes().length);
    }


    //used to send data
    public DataPacket(short opCode, int blockNum, byte [] data) {
        this.opCode = convertShortToByteArray(opCode);
        this.blockNum = convertIntToByteArray(blockNum);
        this.data = data;
    }

    //used for ACK's
    public DataPacket(short opCode, int blockNum) {
        this.opCode = convertShortToByteArray(opCode);
        this.blockNum = convertIntToByteArray(blockNum);
    }





    public static DataPacket rrqPacket(String message) {
        return new DataPacket(RRQ, message);
    }

    public static DataPacket wrqPacket(String message) {
        return new DataPacket(WRQ, message);
    }

    public static DataPacket ackPacket(int blockNum) {
        return new DataPacket(ACK, blockNum);
    }

    public static DataPacket dataPacket(int blockNum, byte [] data) {
        return new DataPacket(DATA, blockNum, data);
    }

    public static DataPacket errPacket(String message) {
        return new DataPacket(ERROR, message);
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





    
    public byte [] getBytes() {

        byte [] packetBytes;

        if (data != null) {
            packetBytes = new byte [opCode.length + blockNum.length + data.length];
            System.arraycopy(data, 0, packetBytes, blockNum.length + opCode.length, data.length);
        } else {
            packetBytes = new byte[opCode.length + blockNum.length];
        }

        System.arraycopy(opCode, 0, packetBytes, 0, opCode.length);
        System.arraycopy(blockNum, 0, packetBytes, opCode.length, blockNum.length);
        return packetBytes;
    }



}
