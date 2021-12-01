import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class testDriver {
    static String pathToDir = "data";
    public static void main(String[] args) {
//        getListOfFiles("data1");

//        System.out.println(readFile("foo"));
//        ZonedDateTime now = ZonedDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
//        String time = now.format(formatter);
//        System.out.println(time);

        writeToFile("foo2");

    }

    private static String getListOfFiles (String path) {
        File folder = new File("data/"+ path);
        File[] listOfFiles = folder.listFiles();


        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println(listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println(listOfFiles[i].getName());
            }
        }
        return null;
    }

    private static String readFile (String path) {
        String pathToDir = "data";
        String content = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(pathToDir + "/" + path));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            content = sb.toString();
            br.close();
        } catch (FileNotFoundException e) {
//            status = FILE_NOT_FOUND;
        } catch (IOException e) {
//            status = SERVER_ERROR;
        }
        return content;
    }

    private static String writeToFile(String path) {
        try {
            FileWriter fw = new FileWriter(pathToDir + "/" + path);

            for (int i = 0; i < 10; i++) {
                fw.write("something");
            }

            fw.close();
        } catch (IOException e) {
//            responseStatus = SERVER_ERROR;
            e.printStackTrace();
        }

        return null;
    }

}
