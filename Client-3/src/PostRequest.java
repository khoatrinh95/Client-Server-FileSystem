import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class PostRequest extends Request{
    private String inlineData;
    private String fileName;
    private int dataLength;

    public PostRequest(String url, String option) {
        super(url, option);

        if (option.contains("-d ") && option.contains("-f ")){
            isInputValid = false;
        }
        else if (option.contains("-d ")) {
            inlineData = option.substring(option.indexOf("{", option.indexOf("-d")), option.indexOf("}") + 1);
            dataLength = inlineData.length();
        }
        else if (option.contains("-f ")) {
            String temp = option.replace(" ","");
            int indexOfDashF = temp.indexOf("-f");
            int indexOfBeginFileName = indexOfDashF+2;
            fileName = temp.substring(indexOfBeginFileName);

            inlineData = "{";
            try {
                File myObj = new File(fileName);
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    inlineData += data;
                    if (myReader.hasNextLine())
                        inlineData += ", ";
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
            inlineData += "}";
            dataLength = inlineData.length();
        }

        if (option.contains("-h ")) {
            if (option.contains("Host")) {
                host = option.substring(option.indexOf(":", option.indexOf("Host")) + 1, option.indexOf(" ", option.indexOf("Host")));
            }

            if (option.contains("Content-Type")) {
                contentType = option.substring(option.indexOf(":", option.indexOf("Content-Type")) + 1, option.indexOf(" ", option.indexOf("Content-Type")));
            }

            if (option.contains("Content-Length")) {
                dataLength = Integer.parseInt(option.substring(option.indexOf(":", option.indexOf("Content-Length")) + 1, option.indexOf(" ", option.indexOf("Content-Length"))));
            }

            if (option.contains("User-Agent")) {
                userAgent = option.substring(option.indexOf(":", option.indexOf("User-Agent")) + 1, option.indexOf(" ", option.indexOf("User-Agent")));
            }
        }
    }

    public String packRequest(){
        return "POST " + path + " HTTP/1.0\r\n" + "Host: " + host + "\r\n" + "Content-Length: " + dataLength + "\r\n" + "Content-Type: " + contentType + "\r\n"
                + "User-Agent: " + userAgent
                + "\r\n"
                + " \r\n"
                + inlineData
                + "\r\n"
                + "\r\n"
                ;
    }
}
