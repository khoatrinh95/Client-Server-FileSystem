import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

public class Connection {
    protected static int cNum = 0;

    public boolean handShake(SocketAddress routerAddr, InetSocketAddress serverAddr){
        try(DatagramChannel channel = DatagramChannel.open()) {
            //send SYN
            String msg = "";
            Packet p = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(cNum)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(msg.getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddr);

            //listen for SYN-ACK
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);
            while (true) {
                buf.clear();
                SocketAddress router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                p = Packet.fromBuffer(buf);
                buf.flip();

                int type = p.getType();

                if (type==1){
                    System.out.println("Connection established...");
                    p = new Packet.Builder()
                            .setType(2)
                            .setPortNumber(serverAddr.getPort())
                            .setPeerAddress(serverAddr.getAddress())
                            .setPayload(msg.getBytes())
                            .create();
                    channel.send(p.toBuffer(), routerAddr);
                    break;
                }
            }
            // if received within time
            // send ACK and
            // return true

            // else
            // error = could not establish connection with host
            // return false

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
