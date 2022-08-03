package sparc.team3.validator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import sparc.team3.validator.util.Settings;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

// -Dsql-log-level=INFO
public class SQL {

    /**
     * get db
     * use prepared statement
     * get tables
     * get column names
     * validate data from here, whatever that means
     */
    public static void main(String[] args) throws SQLException, IOException, InterruptedException {

        Logger logger = LoggerFactory.getLogger(SQL.class);

        Configurator configurator = new Configurator();
        Settings settings = configurator.loadSettings();
        logger.debug("Settings: {}", settings);
        RdsClient rdsClient = RdsClient.builder().region(Region.of(settings.getAwsRegion())).build();
        DescribeDbInstancesRequest describeDbInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(settings.getRdsSettings().getProductionName()).build();
        DescribeDbInstancesResponse describeDbInstancesResponse = rdsClient.describeDBInstances(describeDbInstancesRequest);
        DBInstance dbInstanceProd = describeDbInstancesResponse.dbInstances().get(0);


        String url = "jdbc:mariadb://" + dbInstanceProd.endpoint().address() + ":" + dbInstanceProd.endpoint().port();

        if(!System.getProperty("user.name").startsWith("ec2")){
            System.out.println("Running this on your local machine, you need to setup an ssh tunnel.  This can be done with the following command:\n" +
                    "\t\tssh -i \"Path\\to\\PrivateKeyFile\" -N -l ec2-user -L 3306:database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 ec2-18-215-239-112.compute-1.amazonaws.com -v\n" +
                    "The tunnel will stay open as long as the terminal is open.");
            //ssh -i "/Users/rachelfriend/.ssh/SPARC_EC2_key" -N -l ec2-user -L 3306:database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 ec2-18-215-239-112.compute-1.amazonaws.com -v
            //ssh -i "/Users/rachelfriend/.ssh/SPARC_EC2_key" -N -l ec2-user -L 3307:databasetestrestorevalidate.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 ec2-18-215-239-112.compute-1.amazonaws.com -v
            //ssh -i "/Users/rachelfriend/.ssh/SPARC_EC2_key" -N -l ec2-user -L 3306:database-1.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 -L 3307:databasetestrestorevalidate.c6r4qgx3wvjo.us-east-1.rds.amazonaws.com:3306 ec2-18-215-239-112.compute-1.amazonaws.com -v
            url = "jdbc:mariadb://localhost";
        }

        HikariConfig configProd = new HikariConfig();
        configProd.setJdbcUrl(url);
        configProd.setUsername(settings.getDbUsername());
        configProd.setPassword(settings.getDbPassword());
        configProd.addDataSourceProperty("cachePrepStmts", "true");
        configProd.addDataSourceProperty("prepStmtCacheSize", "250");
        configProd.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        configProd.addDataSourceProperty("readOnly", true);

        HikariConfig configRestored = new HikariConfig();
        configRestored.setJdbcUrl("jdbc:mariadb://localhost:3307");
        configRestored.setUsername(settings.getDbUsername());
        configRestored.setPassword(settings.getDbPassword());
        configRestored.addDataSourceProperty("cachePrepStmts", "true");
        configRestored.addDataSourceProperty("prepStmtCacheSize", "250");
        configRestored.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        configRestored.addDataSourceProperty("readOnly", true);


        HikariDataSource dsProd = new HikariDataSource(configProd);
        HikariDataSource dsRestored = new HikariDataSource(configRestored);

        // Select the database to use
        // For wiki_revision use "rev_id" until primary key isn't hard-coded
        // For wiki_text use "old_id" until primary key isn't hard-coded
        // For wiki_recentchanges use "rc_id" until primary key isn't hard-coded
        String queryWikiRevision = "SELECT * FROM Wiki.wiki_revision\n" +
                "  ORDER BY rev_id DESC;";
        String queryWikiText = "SELECT * FROM Wiki.wiki_text\n" +
                "  ORDER BY old_id DESC;";
        String queryWikiRecentChanges = "SELECT * FROM Wiki.wiki_recentchanges\n" +
                "  ORDER BY rc_id DESC;";
        try (Connection conProd = dsProd.getConnection();
        Connection conRestored = dsRestored.getConnection();
        ) {

            // Prepared Statements
            PreparedStatement pstProd = conProd.prepareStatement(queryWikiRecentChanges);
            PreparedStatement pstRestored = conRestored.prepareStatement(queryWikiRecentChanges);

            // Execute SQL query
            ResultSet rsProd = pstProd.executeQuery();
            ResultSet rsRestored = pstRestored.executeQuery();

            // Get Meta Data
            ResultSetMetaData rsProdMetaData = rsProd.getMetaData();
            ResultSetMetaData rsRestoredMetaData = rsRestored.getMetaData();


            // Sanity check on number of rows of each ==> delete
   /***         int prodRows = 0;
            int restoredRows = 0;
            while (rsProd.next()) {prodRows++;}
            while (rsRestored.next()) {restoredRows++;}
            System.out.println("num prod rows: " + prodRows + "; num restored rows: " + restoredRows);
    */

            // Compare the objects returned in each row
                // Absolute validity check. Any changes made to Wiki since backup, will cause this method to fail.
                // Relative validity check. This method bypasses any changes made to Wiki since backup.
                // Query orders the result set (by primary id) descending
            // Check that we have a ResultSet from each database
            if (rsProd == null || rsRestored == null) {logger.info("Alert: We have a NULL database.");}

            // Sanity checking
                // How to not hard-code "rev_id" ;; way to say "primary key"? ==>

                    //DatabaseMetaData metaData = conProd.getMetaData();
                    //ResultSet primaryKeys = metaData.getPrimaryKeys("Wiki", null, "wiki_recentchanges");
                    //primaryKeys.next();
                    //System.out.println("primary: " +primaryKeys.getString("pk_name"));
                        // outputs: PRIMARY which is not recognized below for columnLabel


            rsRestored.next();
            rsProd.next();
            // Get primary key id's
            //int rdRestoredStartId = rsRestored.getInt("rc_id");
            // is primary always the first column?
            int rdRestoredStartId = rsRestored.getInt(1);
            int rsProdStartId = rsProd.getInt(1);
            System.out.println("restoredStartId: " + rdRestoredStartId + "; rsProdStartId: " + rsProdStartId);

            // if primary key values are the same for both database, then can run absolute validity test
            if (rdRestoredStartId == rsProdStartId) {
                System.out.println("Should be TRUE to run \"absolute\" validity test: " + (rdRestoredStartId == rsProdStartId));
                //run absolute validity test

                // Get number of columns
                int numColumns = rsProdMetaData.getColumnCount();
                int column = 1;
                // Loop through all rows
                while (rsProd.next() && rsRestored.next() && column <= numColumns) {
                    // Get column object returned from each ResultSet and compare with built-in equals()
                    final Object rsProdObj = rsProd.getObject(column);
                    final Object rsRestoredObj = rsRestored.getObject(column);
                    if (!rsProdObj.equals(rsRestoredObj)) {
                        // Should this throw exception or just be logged? Or else?
                        //throw new RuntimeException(String.format("%s and %s aren't equal at common position %d",
                        //        rsProdObj, rsRestoredObj, column));
                        logger.info("\"Absolute\" validity: Prod and Restored are NOT equal in column {}", column);
                    }
                    logger.info("\"Absolute\" validity: Prod and Restored are equal in column {}", column);
                    // Number of rows should be the same for absolute validity check
                    if ((rsProd.isLast() != rsRestored.isLast())) {
                        //throw new RuntimeException("The two ResultSets contain a different number of columns!");
                        logger.info("\"Absolute\" validity: Prod and Restored do NOT have the same number of columns at column: {}", column);
                    }
                    logger.info("\"Absolute\" validity: Prod and Restored have the same number of columns at column: {}", column);
                    column++;
                }
            }
            else {
                //run relative validity test ==> NOTE: Mostly repeated code here; differences: 1. starting point for prod, and 2. logger message.

                //Loop until production database primary id equals that of the (older) restored database
                int numColumns = rsProdMetaData.getColumnCount();
                int column = 1;
                while (rsProd.next()) {
                    if (rsProd.getInt(1) != rdRestoredStartId) {
                        continue;
                    } else {
                        rsProdStartId = rsProd.getInt(1);
                        // getting hacky here to get the number of columns after back scanning prod to most recent entry in restored.
                        numColumns -= rsProdStartId;
                        while(rsProd.next() && rsRestored.next() && column <= numColumns) {
                            // all repeated code (minus logger messages) from absolute validity in this while loop
                            final Object rsProdObj = rsProd.getObject(column);
                            final Object rsRestoredObj = rsRestored.getObject(column);
                            if (!rsProdObj.equals(rsRestoredObj)) {
                                logger.info("\"Relative\" validity: Prod and Restored are NOT equal in column {}", column);
                            }
                            logger.info("\"Relative\" validity: Prod and Restored are equal in column {}", column);
                            // Number of rows should be the same for prod-restored relative validity check
                            if ((rsProd.isLast() != rsRestored.isLast())) {
                                logger.info("\"Relative\" validity: Prod and Restored do NOT have the same number of columns at column: {}", column);
                            }
                            logger.info("\"Relative\" validity: Prod and Restored have the same number of columns at column: {}", column);
                            column++;
                        } break;
                    }
                }
            }

            //query = "SELECT * FROM Wiki.wiki_recentchanges"; // guessing title means it changed recently so could be good or bad for testing haha?


    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
        dsProd.close();
        dsRestored.close();

}

/*
Sort descending
if there are results from both
    get first result from restored
    loop production results until id matches restored
    check the first results

    loop through rest of results from both


 */


    /**
     * public boolean compareResultSets(ResultSet resultSet1, ResultSet resultSet2) throws SQLException{
     *         while (resultSet1.next()) {
     *             resultSet2.next();
     *             ResultSetMetaData resultSetMetaData = resultSet1.getMetaData();
     *             int count = resultSetMetaData.getColumnCount();
     *             for (int i = 1; i <= count; i++) {
     *                 if (!resultSet1.getObject(i).equals(resultSet2.getObject(i))) {
     *                     return false;
     *                 }
     *             }
     *         }
     *         return true;
     *     }
     */

    /**
     * int col = 1;
     *     while (rs1.next() && rs2.next()) {
     *         final Object res1 = rs1.getObject(col);
     *         final Object res2 = rs2.getObject(col);
     *         // Check values
     *         if (!res1.equals(res2)) {
     *             throw new RuntimeException(String.format("%s and %s aren't equal at common position %d",
     *                 res1, res2, col));
     *         }
     *
     *         // rs1 and rs2 must reach last row in the same iteration
     *         if ((rs1.isLast() != rs2.isLast())) {
     *             throw new RuntimeException("The two ResultSets contains different number of columns!");
     *         }
     *
     *         col++;
     *     }
     */
    /**    String nameProd = rsProd.getString("given_name");
     String nameRestored = rsRestored.getString("given_name");
     int ageProd = rsProd.getInt("age");
     int ageRestored = rsRestored.getInt("age");
     logger.info("Prod: {} {}\tRestored: {} {}", nameProd, ageProd, nameRestored, ageRestored);
     nameToAgeMap.put(nameProd, ageProd);
     */
    //}
    //for (Map.Entry<String, Integer> entry : nameToAgeMap.entrySet()) {System.out.println(entry);}
/**
 *
 * playing / learning
 *
 *

 //get list of tables to test others
 PreparedStatement db = conProd.prepareStatement("SHOW TABLES");
 ResultSet r0 = db.executeQuery();) {
 while (r0.next()) {System.out.println(r0.getString(1));}


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

 String query = "SELECT * FROM Wiki.wiki_cargo_Characters where age> ?";
 // Set Parameters
 //pstProd.setInt(1, 100);
 //pstRestored.setInt(1, 100);

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
