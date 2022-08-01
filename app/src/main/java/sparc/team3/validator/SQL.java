package sparc.team3.validator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sparc.team3.validator.util.Settings;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SQL {

    public static void main(String[] args) throws SQLException, IOException, InterruptedException {

        Configurator configurator = new Configurator();
        Settings settings = configurator.loadSettings();
        String url = "jdbc:mariadb://database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com";

        if(!System.getProperty("user.name").startsWith("ec2")){
            System.out.println("Running this on your local machine, you need to setup an ssh tunnel.  This can be done with the following command:\n" +
                    "\t\tssh -i \"Path\\to\\PrivateKeyFile\" -N -l ec2-user -L 3306:database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 ec2-18-215-239-112.compute-1.amazonaws.com -v\n" +
                    "The tunnel will stay open as long as the terminal is open.");
            //ssh -i "/Users/rachelfriend/.ssh/SPARC_EC2_key" -N -l ec2-user -L 3306:database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 ec2-18-215-239-112.compute-1.amazonaws.com -v
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

        //get db
        //use prepared statement
        //get tables
        //get column names
        //validate data from here; how much; how?
        HikariDataSource ds = new HikariDataSource(config);
        String query = "SHOW DATABASES";
        try (Connection con = ds.getConnection();
             PreparedStatement pst = con.prepareStatement( query );
             //diff methods for diff datatypes; specifiy by get int and get column that i want
             ResultSet rs = pst.executeQuery();) {
            ResultSetMetaData rsMetaData = rs.getMetaData(); //method to get columns

            // use particular database ;; just grabs the first one "Wiki"
            rs.next();
            String dbName = rs.getString(1).toString();
            String queryForDatabase = "Use " +dbName;
            System.out.println("query for db: " +queryForDatabase);
            PreparedStatement pstDb = con.prepareStatement(queryForDatabase);
            ResultSet rsDb = pstDb.executeQuery();
            ResultSetMetaData rsDbMetaData = rsDb.getMetaData();
            System.out.println("wiki number of columns: " + rsDbMetaData.getColumnCount());
            while(rsDb.next()) {System.out.println(rsDbMetaData.getColumnName(1));}

            // get list of all that db's tables and store in a list
            PreparedStatement pstTables = con.prepareStatement("SHOW TABLES");
            ResultSet rsTables = pstTables.executeQuery();
            ResultSetMetaData rsTablesMetaData = rsTables.getMetaData();

            List<String> tableNames = new ArrayList<>();
            System.out.println("-----Tables in Wiki Db-----");
            while (rsTables.next()) {
                tableNames.add(rsTables.getString(1));
            }
            for (String s : tableNames) { System.out.println(s); }

            // use particular table and get column names with data types (data types coming soon) of that table
            String tableName = tableNames.get(5);
            System.out.println("table name : " + tableName);

            String queryTableColumns = "SHOW COLUMNS\n" +
                    "  FROM "+tableName+"\n" +
                    "  FROM "+dbName+";";
            PreparedStatement pstTableColumns = con.prepareStatement(queryTableColumns);
            ResultSet rsTableColumns = pstTableColumns.executeQuery();
            ResultSetMetaData rsTableColumnsMetaData = rsTableColumns.getMetaData();

            List<String> columnNames = new ArrayList<>();
            System.out.println("-----Columns in wiki_cargo__Characters table-----");
            System.out.println("num columns: " +rsTableColumnsMetaData.getColumnCount());
            while (rsTableColumns.next()) {
                columnNames.add(rsTableColumns.getString(1));
                for(int i = 1; i <= rsTableColumnsMetaData.getColumnCount(); i++) {
                    System.out.println("columns : " +rsTableColumns.getString(i));
                }
                System.out.println("column names: " +rsTableColumns.getString(1));
                System.out.println("column type: " +rsTableColumnsMetaData.getColumnTypeName(1));}
            for (String s : columnNames) { System.out.println(s); }

        } catch (SQLException e) {
            throw new RuntimeException(e);
            }
        ds.close();
        }

            //System.out.println("number of columns: " +rsMetaData.getColumnCount());
            //System.out.println("column name 1: " +rsMetaData.getColumnName(1));
            //while(rs.next()){ //each rs is a row
                //for(int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                    //System.out.print(rs.getString(1));
                    //System.out.println();
                //}

            // get data types?
           /** String queryColumnDataTypes = "SELECT COLUMN_NAME,DATA_TYPE\n" +
                    "  FROM INFORMATION_SCHEMA.COLUMNS\n" +
                    "  WHERE TABLE_SCHEMA = Database()\n" +
                    "  AND TABLE_NAME = "+tableName+"\n" +
                    "  AND COLUMN_NAME LIKE 'age';";
            PreparedStatement pstColumnsDataTypes = con.prepareStatement(queryColumnDataTypes);
            ResultSet rsColumnsDataTypes = pstColumnsDataTypes.executeQuery();*/

    }
