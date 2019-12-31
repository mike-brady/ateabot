import atea.Atea;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException, SQLException {
        Properties properties = new Properties();
        properties.load(Main.class.getClassLoader().getResourceAsStream(".properties"));
        String host = properties.getProperty("db_host");
        String username = properties.getProperty("db_username");
        String password = properties.getProperty("db_password");

        Atea atea = new Atea(host, username, password);
        Ateabot ateabot = new Ateabot(atea);
        ateabot.run();

//        boolean quit = false;
//        long maxSearchTime = 300;
//        Date d = new Date();
//        long now;
//        long start = d.getTime()/1000;
//        while(!quit) {
//            ateabot.findAbbreviationExample("all");
//            Scanner scnr = new Scanner(System.in);  // Create a Scanner object
//            d = new Date();
//            now = d.getTime()/1000;
//            if(now > start + maxSearchTime) {
//                System.out.print("Continue? (y/n): ");
//                if (!scnr.nextLine().equals("y")) {
//                    quit = true;
//                } else {
//                    d = new Date();
//                    start = d.getTime()/1000;
//                }
//            }
//        }

        //ateabot.disconnect();
    }
}
