
import joptsimple.OptionParser;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Scanner;

public class httpc {

    public static void main(String[] arr) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String input;
        String url;
        String option;

        boolean exit = false;
        boolean validInput = true;

//        establishConnection();

        while (!exit) {
            System.out.print("> ");
            input = scanner.nextLine();
            System.out.println();
            if (input.length() >= 10) {
                if (input.substring(0,5).equalsIgnoreCase("httpc")){
                    String inputOption = input.substring(6);
                    if (inputOption.contains("help")) {
                        String helpString = input.substring(input.indexOf("help")).trim();
                        if (helpString.length()==4){
                            // print help
                            System.out.println(Request.help());
                        }
                        else {
                            String helpOption = helpString.substring(5).trim();

                            if (helpOption.contains("get")){
                                //print help get
                                System.out.println(Request.helpGet());
                            }
                            else if (helpOption.contains("post")) {
                                // print help post
                                System.out.println(Request.helpPost());
                            }
                            else
                                validInput = false;
                        }
                    }
                    else if (inputOption.contains("get")) {
                        url = input.substring(input.indexOf("http://"));
                        option = input.substring(input.indexOf("get") + 4, input.indexOf("http://") - 1);
                        System.out.println(new GetRequest(url, option).create());
                    }

                     else if (inputOption.contains("post")) {
                        url = input.substring(input.indexOf("http://"));
                        option = input.substring(input.indexOf("post") + 5, input.indexOf("http://") - 1);
                        System.out.println(new PostRequest(url, option).create());
                    }
                     else
                         validInput = false;
                }
                else
                    validInput = false;
            }
            else
                validInput = false;

            if (!validInput) {
                // printInvalidInputMenu
                System.out.println(Request.invalidInput());
                validInput = true;
            }
        }


    }


}
