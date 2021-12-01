import Exceptions.ForbiddenAccessException;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {
    private int port;
    private String host;
    private boolean isVerbose;
    private String pathToDir;
    private Packet requestPacket;
    private static long sNum = 0;
    private static long cNum;

    // response attributes
    private ArrayList<String> request= new ArrayList<>();
    private String responseStatus = "";
    private String responseContent = "";
    private FileTime lastModified = null;


    private static final String FILE_NOT_FOUND = "HTTP/1.0 404 Not Found";
    private static final String BAD_REQUEST = "HTTP/1.0 400 Bad Request";
    private static final String FORBIDDEN = "HTTP/1.0 403 Forbidden";
    private static final String OK = "HTTP/1.0 200 OK";
    private static final String SERVER_ERROR = "HTTP/1.0 500 Internal Server Error";
    private static final int MAX_PAYLOAD_SIZE = 1013;
    private static final int SYN = 0;
    private static final int SYNACK = 1;
    private static final int ACK = 2;
    private static final int NAK = 3;
    private static final int DATA = 4;
    private static final int DATA_LAST = 5;

    public Server(int port, String host, boolean isVerbose, String pathToDir) {
        System.out.println("Starting server on: ");
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Verbose: " + isVerbose);
        System.out.println("Path: " + pathToDir);
        System.out.println();

        this.port = port;
        this.host = host;
        this.isVerbose = isVerbose;
        this.pathToDir = pathToDir.substring(1);

        // start server
        startServer();
    }

    private void startServer() {
        connectSocket();
    }

    private boolean connectSocket() {

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(port));
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            for (; ; ) {
                buf.clear();
                SocketAddress router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                 requestPacket = Packet.fromBuffer(buf);
                buf.flip();

                // if received SYN
                if (requestPacket.getType()==0){
//                    System.out.println("RECEIVED SYN");
                    //send SYN-ACK
                    Packet resp = requestPacket.toBuilder()
                            .setType(1)
                            .setSequenceNumber(sNum)
                            .setPayload(parseResponse().getBytes())
                            .create();
                    channel.send(resp.toBuffer(), router);
                    sNum++;
                }


                // if received data
                if (requestPacket.getType()==4){
                    String payload = new String(requestPacket.getPayload(), UTF_8);
                    request = stringToArray(payload);

                    // check if request is get or post
                    if (request.get(0).contains("GET")){
                        responseContent = processGetRequest(request);
                    } else if (request.get(0).contains("POST")) {
                        responseContent = processPostRequest(request);
                    }

                    // Send the response to the router not the client.
                    // The peer address of the packet is the address of the client already.
                    // We can use toBuilder to copy properties of the current packet.
                    // This demonstrate how to create a new packet from an existing packet.
                    String responsePayload = parseResponse();

                    // split response into 1013 byte chunks
                    ArrayList<byte[]> listDatagrams = divideArray(responsePayload.getBytes(), MAX_PAYLOAD_SIZE);

                    // continuously send the chunks
                    for (int i =0; i< listDatagrams.size(); i++){
                        if (i== listDatagrams.size()-1){
                            Packet resp = requestPacket.toBuilder()
                                    .setType(DATA_LAST)
                                    .setPayload(listDatagrams.get(i))
                                    .setSequenceNumber(sNum)
                                    .create();
                            channel.send(resp.toBuffer(), router);
                        } else {
                            Packet resp = requestPacket.toBuilder()
                                    .setType(DATA)
                                    .setPayload(listDatagrams.get(i))
                                    .setSequenceNumber(sNum)
                                    .create();
                            channel.send(resp.toBuffer(), router);
                        }
                        sNum++;
                    }
                    for(byte[] datagrama : listDatagrams){


                    }

                    printDebuggingMessage();
                }

            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }
    private ArrayList<String> stringToArray(String s){
        return new ArrayList(Arrays.asList(s.split("\n")));
    }
    private String processGetRequest(ArrayList<String> request) {
        String firstLine = request.get(0);
        String[] firstLineSplit = firstLine.split(" ");
        String path = firstLineSplit[1];
        String result = "";

        // if path = / -> get list
        if (path.equalsIgnoreCase("/")){
            result = getListOfFiles(path);
        }
        // else -> read file (throw exc if filenotfound)
        else {
            result = readFile(path);
        }
        return result;
    }

    private String processPostRequest(ArrayList<String> request) {
        String firstLine = request.get(0);
        String[] firstLineSplit = firstLine.split(" ");
        String path = firstLineSplit[1];

        ArrayList<String> toBeWritten = new ArrayList<>();
        boolean startAdding = false;
        for (String s : request) {
            if (startAdding){
                toBeWritten.add(s);
            }
            if (s.equalsIgnoreCase(" \r")){
                startAdding = true;
            }
        }

        writeToFile(path, toBeWritten);

        return readFile(path);
    }

    private String getListOfFiles (String path) {
        File folder = new File(pathToDir + "/" + path);
        File[] listOfFiles = folder.listFiles();

        String stringListOfFiles = "";
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                stringListOfFiles += (listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                stringListOfFiles += ("/" + listOfFiles[i].getName());
            }
            if (i!= listOfFiles.length){
                stringListOfFiles += "\n";
            }
        }
        responseStatus = OK;
        return stringListOfFiles;
    }

    private String readFile (String path) {
        String content = "";
        try {
            // security access: check if path provided is inside pathToDir
            // if not -> throw ForbiddenAccessException
            File fileTestSecurity = new File(pathToDir + "/" + path);
            if (path.contains("..") || !fileTestSecurity.getParent().substring(0,pathToDir.length()).equalsIgnoreCase(pathToDir)) {
                throw new ForbiddenAccessException();
            }

            // security access: pathToDir + "/" + path
            // this will not allow client to go outside of /pathToDir
            BufferedReader br = new BufferedReader(new FileReader(pathToDir + "/" + path));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            content = sb.toString();
            responseStatus = OK;

            // get file attribute
            Path file = Paths.get(pathToDir + "/" + path);
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            lastModified = attr.lastModifiedTime();

            br.close();
        } catch (FileNotFoundException e) {
            responseStatus = FILE_NOT_FOUND;
//            printDebuggingMessage(responseStatus);
        } catch (IOException e) {
            responseStatus = SERVER_ERROR;
//            printDebuggingMessage(responseStatus);
        } catch (ForbiddenAccessException e) {
            responseStatus = FORBIDDEN;
//            printDebuggingMessage(responseStatus);
        }
        return content;
    }

    private String parseResponse () {
        String response = "";

        // get current time
        ZonedDateTime now = ZonedDateTime.now();

        // get expired time
        ZonedDateTime nextDay = ZonedDateTime.now().plusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        String timeNow = now.format(formatter);
        String timeNextDay = nextDay.format(formatter);

        response += (responseStatus + "\r\n");
        response += ("Date: " + timeNow +"\r\n");
        response += ("Server: Apache/0.8.4\r\n");
        response += ("Content-Type: text\r\n");
        response += ("Content-Length: " + responseContent.length() + "\r\n");
        response += ("Expires: " + timeNextDay +"\r\n");

        if (lastModified != null) {
            long cTime = lastModified.toMillis();
            ZonedDateTime temp = Instant.ofEpochMilli(cTime).atZone(ZoneId.of("UTC-4"));
            String timeFileLastModified = temp.format(formatter);
            response += ("Last-modified: " + timeFileLastModified + "\r\n");
        }

        response += ("\r\n");
        response += (responseContent);
        return response;
    }

    private String writeToFile(String path, ArrayList<String> toBeWritten) {
        try {
            FileWriter fw = new FileWriter(pathToDir + "/" + path);

            for (String s : toBeWritten) {
                fw.write(s);
                fw.write("\n");
            }
            responseStatus = OK;
            fw.close();
        } catch (IOException e) {
            responseStatus = SERVER_ERROR;
//            printDebuggingMessage(responseStatus);
        }
        return null;
    }

    private void clearResponse() {
        request.clear();
        responseStatus = "";
        responseContent = "";
        lastModified = null;
    }

    private void printDebuggingMessage(){
        if (isVerbose) {
            System.err.println("Receiving request from Client: ");
            System.err.println("Port: " + requestPacket.getPeerPort());
            System.err.println("Address: " + requestPacket.getPeerAddress());
            for (String s : request) {
                System.err.println(s);
            }
            System.err.println("STATUS: " + responseStatus);
        }
    }

    public static ArrayList<byte[]> divideArray(byte[] source, int chunksize) {

        ArrayList<byte[]> result = new ArrayList<>();
        int start = 0;
        while (start < source.length) {
            int end = Math.min(source.length, start + chunksize);
            result.add(Arrays.copyOfRange(source, start, end));
            start += chunksize;
        }

        return result;
    }

//    private void send(int type, String payload, DatagramChannel channel) throws IOException{
//        Packet p = new Packet.Builder()
//                .setType(type)
//                .setSequenceNumber(cNum)
//                .setPortNumber(serverAddress.getPort())
//                .setPeerAddress(serverAddress.getAddress())
//                .setPayload(payload.getBytes())
//                .create();
//        channel.send(p.toBuffer(), routerAddress);
//        cNum++;
//    }
}
