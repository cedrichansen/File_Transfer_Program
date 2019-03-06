import java.nio.ByteBuffer;

public class DataPacket {

        /*

     2 bytes     string    1 byte     string   1 byte
      ------------------------------------------------
     | Opcode |  Filename  |   0  |    Mode    |   0  |
      ------------------------------------------------

             Figure 5-1: RRQ/WRQ packet


       2 bytes     2 bytes      n bytes
       ----------------------------------
      | Opcode |   Block #  |   Data     |
       ----------------------------------

         Figure 5-2: DATA packet



         2 bytes     2 bytes
         ---------------------
        | Opcode |   Block #  |
         ---------------------

          Figure 5-3: ACK packet


       2 bytes     2 bytes      string    1 byte
       -----------------------------------------
      | Opcode |  ErrorCode |   ErrMsg   |   0  |
       -----------------------------------------

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
    static short RRQ = 1;
    static short WRQ = 2;
    static short DATA = 3;
    static short ACK = 4;
    static short ERROR = 5;

    
    // used for RRQ/WRQ packet AND Error packet
    public DataPacket(short opCode, String messageStr) {
        this.message = new byte [128];
        this.opCode = convertShortToByteArray(opCode);
        System.arraycopy(messageStr.getBytes(), 0, this.message, 0, messageStr.getBytes().length);
    }


    //used to send data
    public DataPacket(short opCode, short blockNum, byte [] data) {
        this.opCode = convertShortToByteArray(opCode);
        this.blockNum = convertShortToByteArray(blockNum);
        this.data = data;
    }

    //used for ACK's
    public DataPacket(short opCode, short blockNum) {
        this.opCode = convertShortToByteArray(opCode);
        this.blockNum = convertShortToByteArray(blockNum);
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
