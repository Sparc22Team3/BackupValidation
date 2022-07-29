package sparc.team3.validator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sparc.team3.validator.util.Settings;

import java.io.IOException;
import java.sql.*;

public class SQL {

    public static void main(String[] args) throws SQLException, IOException, InterruptedException {

        Configurator configurator = new Configurator();
        Settings settings = configurator.loadSettings();
        String url = "jdbc:mariadb://database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com";

        if(!System.getProperty("user.name").startsWith("ec2")){
            System.out.println("Running this on your local machine, you need to setup an ssh tunnel.  This can be done with the following command:\n" +
                    "\t\tssh -i \"Path\\to\\PrivateKeyFile\" -N -l ec2-user -L 3306:database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 ec2-18-215-239-112.compute-1.amazonaws.com -v\n" +
                    "The tunnel will stay open as long as the terminal is open.");
            url = "jdbc:mariadb://localhost";
        }


        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(settings.getDbUsername());
        config.setPassword(settings.getDbPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("readOnly", true);

        HikariDataSource ds = new HikariDataSource(config);
        String query = "SHOW DATABASES";
        try (Connection con = ds.getConnection();
             PreparedStatement pst = con.prepareStatement( query );
             ResultSet rs = pst.executeQuery();) {
            ResultSetMetaData rsMetaData = rs.getMetaData();
            while(rs.next()){
                for(int i = 1; i <= rsMetaData.getColumnCount(); i++)
                    System.out.print(rs.getString(i));
                System.out.println();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ds.close();
    }
}