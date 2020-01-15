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
        ateabot.disconnect();
    }
}
