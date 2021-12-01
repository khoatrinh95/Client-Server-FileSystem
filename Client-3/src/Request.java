import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.SelectionKey.OP_READ;

public abstract class Request {
    protected String url;
    protected String host;
    protected String path;
    protected String query;
    protected String userAgent;
    protected String contentType;
    protected int port;
    protected boolean isVerbose;
    protected boolean isInputValid = true;
    protected InetSocketAddress serverAddress;
    protected InetSocketAddress routerAddress;
    protected SocketChannel socketChannel;
    protected String response;
    protected String error;
    protected static long cNum = 0;
    protected static long sNum;
    protected HashMap<Long, Packet> buffer;


    private static final String HOST = "Host";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String USER_AGENT = "User-Agent";
    private static final int SYN = 0;
    private static final int SYNACK = 1;
    private static final int ACK = 2;
    private static final int NAK = 3;
    private static final int DATA = 4;
    private static final int DATA_LAST = 5;


    public Request(String url, String option) {
        this.url = url;
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e){
            System.out.println(e.getMessage());
        }

        this.host = uri.getHost();
        this.path = uri.getPath();
        this.path = path.trim();
        this.path = path.replace("'","");
        if (path == null || path.length() == 0) {
            path = "/";
        }


        this.query = uri.getQuery();
        if (query != null) {
            query = "?" + query;
        } else {
            query = "";
        }

        this.port = uri.getPort();
        if (port == -1) {
            port = 80;
        }

        if (option.contains("-v ")){
            isVerbose = true;
        }

        error = "Request sent fail";
        serverAddress = new InetSocketAddress(host, port);
        routerAddress = new InetSocketAddress("localhost", 3000);
        buffer = new HashMap<>();
        response = "";
    }


    private boolean connectSocket() {
        // will try to do 3-way handshake
        // if no response from server after 5s -> redo handshake
        handShake();

        try(DatagramChannel channel = DatagramChannel.open()){
            String msg = packRequest();
//            System.err.println(msg);

            send(DATA, msg, channel);

            while(true){
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = channel.receive(buf);

                buf.flip();
                Packet resp = Packet.fromBuffer(buf);

                int respType = resp.getType();

                if (respType==DATA || respType == DATA_LAST){

                    long respSequence = resp.getSequenceNumber();
                    System.err.println("Seq: " + respSequence);
                    if (respSequence==sNum+1){
                        System.out.println("MATCH " + respSequence);
                        // TODO: SEND ACK + seq
                        send(ACK, "", channel);
                        sNum = respSequence;
                        response += new String(resp.getPayload(), StandardCharsets.UTF_8);
                        // instead of line above, process the buffer
                        if (respType==DATA_LAST){
                            break;
                        }
                    }
                    else if (respSequence>sNum+1){
                        buffer.put(respSequence, resp);
                        System.out.println("*********NAK " + respSequence );
                        // TODO: SEND NAK + seq
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String packResponse(String response) {
        return response;
//        return (isVerbose? response: response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1));
    }

    public String packRequest() {
        return "GET " + path + query + " HTTP/1.0\r\n" + "Host: " + host + "\r\n" + "Connection: close\r\n" + "User-Agent: " + userAgent + "\r\n" + "\r\n";
    }

    public String create() {
        if (!isInputValid){
            return "Your input is not valid";
        }

        boolean requestSuccess = connectSocket();

        String responseToClient = response;
        response = "";
        return (requestSuccess? responseToClient : error);

    }
    public static String help(){
        return "httpc help \n\n" +
                "httpc is a curl-like application but supports HTTP protocol only. \n" +
                "Usage: \n" +
                "\thttpc command [arguments]\n" +
                "The commands are: \n" +
                "\tget\t\t executes a HTTP GET request and prints the response.\n" +
                "\tpost\t executes a HTTP POST request and prints the response.\n" +
                "\thelp\t prints this screen.\n\n" +
                "Use \"httpc help [command]\" for more information about a command.\n";
    }

    public static String helpGet(){
        return "httpc help get\n\n" +
                "usage: httpc get [-v] [-h key:value] URL \n\n" +
                "Get executes a HTTP GET request for a given URL. \n" +
                "\t-v\t\t\t\t Prints the detail of the response such as protocol, status, and headers.\n" +
                "\t-h hey:value\t Associates headers to HTTP Request with the format 'key:value'.\n";
    }

    public static String helpPost(){
        return "httpc help post\n\n" +
                "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL \n\n" +
                "Post executes a HTTP POST request for a given URL with inline data or from file.\n" +
                "\t-v\t\t\t\t Prints the detail of the response such as protocol, status, and headers.\n" +
                "\t-h hey:value\t Associates headers to HTTP Request with the format 'key:value'.\n" +
                "\td string\t\t Associates an inline data to the body HTTP POST request.\n" +
                "\t-f file\t\t\t Associates the content of a file to the body HTTP POST request.\n\n" +
                "Either [-d] or [-f] can be used but not both.";
    }

    public static String invalidInput(){
        return "Invalid input\n\n" +
                "Use 'httpc help' for instructions \n\n";
    }

    public boolean handShake(){
        boolean conEstablished = false;
        while (!conEstablished) {
            try(DatagramChannel channel = DatagramChannel.open()) {
                //send SYN
                System.err.println("Sending SYN...");
                String msg = "";

                send(SYN, msg, channel);

                //listen for SYN-ACK
                ByteBuffer buf = ByteBuffer
                        .allocate(Packet.MAX_LEN)
                        .order(ByteOrder.BIG_ENDIAN);
                while (true) {
                    // Try to receive a packet within 5s
                    channel.configureBlocking(false);
                    Selector selector = Selector.open();
                    channel.register(selector, OP_READ);
                    selector.select(5000);


                    buf.clear();
                    SocketAddress router = channel.receive(buf);

                    // if no reply after 5s -> resend SYN
                    Set<SelectionKey> keys = selector.selectedKeys();
                    if(keys.isEmpty()){
                        break;
                    }

                    // Parse a packet from the received raw data.
                    buf.flip();
                    Packet p = Packet.fromBuffer(buf);
                    buf.flip();

                    int type = p.getType();

                    if (type==1){
                        // extract the Server sequence number
                        sNum = p.getSequenceNumber();
                        System.err.println("Received SYN-ACK. Connection established...");

                        send(ACK, msg, channel);
                        conEstablished = true;
                        keys.clear();
                        TimeUnit.SECONDS.sleep(1);
                        break;
                    }
                }
                // if received within time
                // send ACK and
                // return true

                // else
                // error = could not establish connection with host
                // return false

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void send(int type, String payload, DatagramChannel channel) throws IOException{
        Packet p = new Packet.Builder()
                .setType(type)
                .setSequenceNumber(cNum)
                .setPortNumber(serverAddress.getPort())
                .setPeerAddress(serverAddress.getAddress())
                .setPayload(payload.getBytes())
                .create();
        channel.send(p.toBuffer(), routerAddress);
        cNum++;
    }
}
