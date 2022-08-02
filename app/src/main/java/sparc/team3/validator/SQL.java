package sparc.team3.validator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sparc.team3.validator.config.ConfigLoader;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.Settings;
import sparc.team3.validator.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

public class SQL {

    public static void main(String[] args) throws SQLException, IOException, InterruptedException {

        ConfigLoader configLoader = new ConfigLoader(new CLI(), Util.DEFAULT_CONFIG.toString());
        Settings settings = configLoader.loadSettings();
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

        /**
         * get db
         * use prepared statement
         * get tables
         * get column names
         * validate data from here, whatever that means
         */

        HikariDataSource ds = new HikariDataSource(config);

        // Prepared Statement
        //String q = "SELECT * FROM ? where age> ?"; // not working
        String query = "SELECT * FROM wiki_cargo__Characters where age> ?";
        try (Connection con = ds.getConnection();
        // Select the database to use
        PreparedStatement psDb = con.prepareStatement("USE Wiki");
        ResultSet r = psDb.executeQuery();) {

        // Prepare Statement
            PreparedStatement pst = con.prepareStatement(query);

        // Set Parameters
        //ps.setString(1, "wiki_cargo__Characters"); // not sure how to do for db name
        pst.setInt(1, 100);

        // Execute SQL query
        ResultSet rs = pst.executeQuery();

        // Get Meta Data
        ResultSetMetaData rsMetaData = rs.getMetaData();

        // put result into Map for comparing
        Map<String, Integer> nameToAgeMap = new TreeMap<>();
        while (rs.next()) {
            String name = rs.getString("given_name");
            int age = rs.getInt("age");
            nameToAgeMap.put(name, age);
        }
        for (Map.Entry<String, Integer> entry : nameToAgeMap.entrySet()) {System.out.println(entry);}

            /**
             //get list of tables to test others
             PreparedStatement db = con.prepareStatement("SHOW TABLES");
             ResultSet r0 = db.executeQuery();) {
             while (r0.next()) {System.out.println(r0.getString(1));} */

        query = "SELECT * FROM wiki_recentchanges"; // guessing title means it changed recently so could be good or bad for testing?
            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            ResultSetMetaData md = rs.getMetaData();
            System.out.println(md.getColumnCount());
/**
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                System.out.println(md.getColumnTypeName(i));
                System.out.println(md.getColumnName(1));
                System.out.println(md.getColumnName(2));
                System.out.println(md.getColumnName(3));
                System.out.println(md.getColumnName(4));
            }*/

    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
        ds.close();
}


/**
 * playing / learning
 *
        String query = "SHOW DATABASES";
        try (Connection con = ds.getConnection();
             PreparedStatement pst = con.prepareStatement( query );
             //diff methods for diff datatypes; specifiy by get int and get column that i want
             ResultSet rs = pst.executeQuery();) {
            //ResultSetMetaData rsMetaData = rs.getMetaData(); //method to get columns

            // use particular database ;; just grabs the first one "Wiki"
            rs.next();
            String dbName = rs.getString(1).toString();
            String queryForDatabase = "Use " +dbName;
            PreparedStatement db = con.prepareStatement(queryForDatabase);
            ResultSet rDb = db.executeQuery();
            ResultSetMetaData rsDbMetaData = rDb.getMetaData();

            // get list of all that db's tables and store in a list
            String queryForTables = "SHOW TABLES";
            PreparedStatement pstTables = con.prepareStatement(queryForTables);
            ResultSet rsTables = pstTables.executeQuery();
            ResultSetMetaData rsTablesMetaData = rsTables.getMetaData();

            List<String> tableNames = new ArrayList<>();
            //System.out.println("-----Tables in Wiki Db-----");
            while (rsTables.next()) {
                //System.out.println(rsTables.getString((1)));
                tableNames.add(rsTables.getString(1));
                //////////
                System.out.println(rsTables.getString("table_names.tables_in_wiki"));
            }
            //for (String s : tableNames) { System.out.println(s); }

            // use particular table and get column names with data types (data types coming soon) of that table
            String tableName = tableNames.get(5);
            System.out.println("table name : " + tableName);

            String queryTableColumns = "SHOW COLUMNS\n" +
                    "  FROM "+tableName+"\n" +
                    "  FROM "+dbName+";";
            PreparedStatement pstTableColumns = con.prepareStatement(queryTableColumns);
            ResultSet rsTableColumns = pstTableColumns.executeQuery();
            ResultSetMetaData rsTableColumnsMetaData = rsTableColumns.getMetaData();

            //List<String> columnNames = new ArrayList<>();
            Map<String, String> columnNameToDataType = new TreeMap<>();
            System.out.println("-----Columns in wiki_cargo__Characters table-----");
            //System.out.println("num columns: " +rsTableColumnsMetaData.getColumnCount()); //6 why? only the first 2 seem to matter for this
            while (rsTableColumns.next()) { //rows
                System.out.println(rsTableColumns.getString("columns.type"));
                columnNameToDataType.put(rsTableColumns.getString(1), rsTableColumns.getString(2));
                //columnNames.add(rsTableColumns.getString(1));
                // in table order:
                //System.out.println("column1 : " +rsTableColumns.getString(1));
                //System.out.println("column2 : " +rsTableColumns.getString(2));
            }
            //for (String s : columnNames) { System.out.println(s);
            for (Map.Entry<String, String> entry : columnNameToDataType.entrySet()) {System.out.println(entry);}

            String sql = "SELECT * FROM wiki_cargo__Characters";
            PreparedStatement p = con.prepareStatement(sql);
            ResultSet r = p.executeQuery();
            int count = 0;
            while(r.next()) {
                String name = r.getString("given_name");
                int age = r.getInt("age");
                System.out.println("hello : " +name + ", aged " + age);
                count++;
            }
            System.out.println("count: " + count);
            */

            // get data types?
           /** String queryColumnDataTypes = "SELECT COLUMN_NAME,DATA_TYPE\n" +
                    "  FROM INFORMATION_SCHEMA.COLUMNS\n" +
                    "  WHERE TABLE_SCHEMA = Database()\n" +
                    "  AND TABLE_NAME = "+tableName+"\n" +
                    "  AND COLUMN_NAME LIKE 'age';";
            PreparedStatement pstColumnsDataTypes = con.prepareStatement(queryColumnDataTypes);
            ResultSet rsColumnsDataTypes = pstColumnsDataTypes.executeQuery();*/

    }
