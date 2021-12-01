import Exceptions.InvalidInputException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class httpf {

    public static void main(String[] arr) {
        Scanner scanner = new Scanner(System.in);
        int port = -1;
        String host = "localhost";
        boolean isVerbose = false;
        String pathToDir = "";
        while(true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            try {
                // String to be scanned to find the pattern.
                String pattern = "^(httpfs )(-v )?(-p \\d{4})?(-d )?";

                // Create a Pattern object
                Pattern r = Pattern.compile(pattern);

                // Now create matcher object.
                Matcher m = r.matcher(input);

                // check if string matches regex
                if (m.find()) {
                    String content = input.substring(7);

                    // if the rest contains -v -> set verbose to true
                    if (content.contains("-v ")){
                        isVerbose = true;
                    }

                    /*
                    if the rest contains -p -> extract the 4 digit port after -p
                        -> if not contains -> set port to 8080
                     */
                    if (content.contains("-p ")) {
                        port = Integer.parseInt(content.substring(content.indexOf("-p ") + 3,content.indexOf("-p ") +7));
                    } else {
                        port = 8080;
                    }

                    /*
                    if the rest contains -d -> extract the rest to get file path
                        -> if not contains -> set path to /
                     */
                    if (content.contains("-d ")) {
                        pathToDir = content.substring(content.indexOf("-d ")+3);
                    } else {
                        pathToDir = "/";
                    }
                } else {
                    throw new InvalidInputException("Invalid input");
                }

                // create Server here
                Server server = new Server(port, host, isVerbose, pathToDir);
            }
            catch (InvalidInputException e) {
                System.out.println(e.getMessage());
            }
        }


    }
}
