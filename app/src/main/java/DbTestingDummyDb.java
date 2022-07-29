import software.amazon.awssdk.services.rds.RdsClient;

import java.sql.*;

public class DbTestingDummyDb {

    private RdsClient rdsClient;

    public DbTestingDummyDb(RdsClient rdsClient) {
        this.rdsClient = rdsClient;
    }

    boolean dbRowsTest() throws SQLException, ClassNotFoundException {
        //update password
        Connection connection = DriverManager.getConnection("jdbc:mariadb://localhost:3306/database-dummy1", "admin","716Qmg&B^jwg");
       // System.out.println(connection.getMetaData());
        System.out.println(connection);


        Statement statement;
        statement = connection.createStatement();
        ResultSet resultSet;
        resultSet = statement.executeQuery(
                "select * from designation");
        String city, airportCode;
        while (resultSet.next()) {
            city = resultSet.getString("city").trim();;
            airportCode = resultSet.getString("airportCode");
            System.out.println("City : " + city
                    + " AirportCode : " + airportCode);
        }
        resultSet.close();
        statement.close();

        connection.close();

        return false;

    }

}
