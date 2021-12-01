import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import static java.nio.charset.StandardCharsets.UTF_8;

public class httpcListening {
    static int listeningPort = 56000;
    static Packet responsePacket = null;

    public static void main(String[] args) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(listeningPort));
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            for (; ; ) {
                buf.clear();
                SocketAddress router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                responsePacket = Packet.fromBuffer(buf);
                buf.flip();

                String payload = new String(responsePacket.getPayload(), UTF_8);

                System.out.println(payload);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
