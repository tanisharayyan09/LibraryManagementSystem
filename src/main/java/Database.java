import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    public static Connection connect() {
        try {
            String url = "jdbc:mysql://localhost:3306/librarymanagement";
            String username = "root";
            String password = "tanisha@09";

            Connection con = DriverManager.getConnection(url, username, password);

            System.out.println("Database Connected Successfully!");
            return con;

        } catch (Exception e) {
            System.out.println("Database Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }
}