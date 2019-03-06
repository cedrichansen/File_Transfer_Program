import java.io.IOException;
import java.net.*;


public class UDPServer {

    DatagramSocket socket;


    public UDPServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }



    public void close() {
        socket.close();
    }


}
