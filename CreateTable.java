import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateTable {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/itdg";
        String user = "itdg";
        String password = "itdg123";

        try (Connection con = DriverManager.getConnection(url, user, password);
                Statement stmt = con.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS korean_test (" +
                    "id SERIAL PRIMARY KEY, " +
                    "user_name VARCHAR(50), " +
                    "home_address VARCHAR(200))";

            stmt.executeUpdate(sql);
            System.out.println("Table korean_test created successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
