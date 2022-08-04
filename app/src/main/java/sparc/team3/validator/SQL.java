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
        if(isValid()) {
            System.out.println("Restored Database is valid.");
        }

    }

    private static boolean isValid() throws SQLException, IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(SQL.class);
        Configurator configurator = new Configurator();
        Settings settings = configurator.loadSettings();
        logger.debug("Settings: {}", settings);
        RdsClient rdsClient = RdsClient.builder().region(Region.of(settings.getAwsRegion())).build();
        DescribeDbInstancesRequest describeDbInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(settings.getRdsSettings().getProductionName()).build();
        DescribeDbInstancesResponse describeDbInstancesResponse = rdsClient.describeDBInstances(describeDbInstancesRequest);
        DBInstance dbInstanceProd = describeDbInstancesResponse.dbInstances().get(0);


        String url = "jdbc:mariadb://" + dbInstanceProd.endpoint().address() + ":" + dbInstanceProd.endpoint().port();

        if (!System.getProperty("user.name").startsWith("ec2")) {
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
            // Still hard-coding primary key id
        String dbName = " ";
        String tableName = " ";
        dbName = "Wiki";
        //tableName = "wiki_recentchanges"; //primaryKey = "rc_id";
        tableName = "wiki_revision"; //primaryKey = "rev_id";
        //tableName = "wiki_text"; //primaryKey = "old_id";
        //tableName = "wiki_updatelog"; //primaryKey = "ul_key";
        String primaryKey = "rev_id";

        String query = "SELECT * FROM " +dbName +"." +tableName+"\n" +
                "  ORDER BY " +primaryKey+ " DESC;";

        // Check that all tables are present in restored
        // Run check table
        // get back column names and datatypes and all else in the metadata --> called schema or structure for all the info that isn't data --> how they are all set up
        try (Connection conProd = dsProd.getConnection();
             Connection conRestored = dsRestored.getConnection();
        ) {

            // Check to see if all tables are present in restored database
            PreparedStatement pstProdDb = conProd.prepareStatement("USE " +dbName+ ";");
            PreparedStatement pstRestoredDb = conRestored.prepareStatement("USE " +dbName+ ";");

            ResultSet rsProd1 = pstProdDb.executeQuery();
            ResultSet rsRestored1 = pstRestoredDb.executeQuery();

            PreparedStatement pstProdTables = conProd.prepareStatement("SHOW TABLES;");
            PreparedStatement pstRestoredTables = conRestored.prepareStatement("SHOW TABLES;");

            ResultSet rsProdTables = pstProdTables.executeQuery();
            ResultSet rsRestoredTables = pstRestoredTables.executeQuery();

            ResultSetMetaData rsProdMetaData1 = rsProdTables.getMetaData();
            ResultSetMetaData rsRestoredMetaData1 = rsRestoredTables.getMetaData();

            // Check if number of tables is the same.
            if (rsProdMetaData1.getColumnCount() != rsRestoredMetaData1.getColumnCount()) {
                logger.info("The production and restored databases have a different number of tables.");
                return false;
            }
            else { logger.info("The production and restored databases have the same number of tables.");};

            // Run CHECK TABLES on all tables of restored database
            List<ResultSet> corruptedDbList = new ArrayList<>();
            while (rsRestoredTables.next()) {
                // Loop through all tables to query CHECK TABLE on each one
                PreparedStatement pstRestoredCHECKTable = conRestored.prepareStatement("CHECK TABLE " +rsRestoredTables.getString(1)+ ";");
                ResultSet rsRestoredCHECKTable = pstRestoredCHECKTable.executeQuery();
                ResultSetMetaData m = rsRestoredCHECKTable.getMetaData();
                rsRestoredCHECKTable.next();
                String checkMsg = rsRestoredCHECKTable.getString(m.getColumnName(4));
                // If Msg_text column returns anything other thank "OK", add to corruptedDbList
                if (!checkMsg.equals("OK")) {
                    corruptedDbList.add(rsRestoredCHECKTable); }
            }
            // Log results
            logger.info("There are {} corrupted tables in the database.", corruptedDbList.size());
            for (ResultSet r : corruptedDbList) {logger.info("Corrupted table: : " + r.getString(1));}


            DatabaseMetaData metaDataProd = conProd.getMetaData();
            DatabaseMetaData metaDataRestore = conRestored.getMetaData();

            String[] typesProd = {"TABLE"};
            String[] typesRestored = {"TABLE"};
            //Retrieving the columns in the database
            ResultSet tablesProd = metaDataProd.getTables(null, null, "%", typesProd);
            ResultSet tablesRestored = metaDataProd.getTables(null, null, "%", typesRestored);
            int numTablesMatching = 0;
            while (tablesProd.next() && tablesRestored.next()) {
                final Object rsProdTableObj = tablesProd.getObject(1);
                final Object rsRestoredTableObj = tablesRestored.getObject(1);
                if (!rsProdTableObj.equals(rsRestoredTableObj)) {
                    logger.info("Table(s) missing.");
                    return false;
                } else {numTablesMatching++;}
            }
            logger.info("All {} tables are in both databases.", numTablesMatching);


            // Prepared Statements
            PreparedStatement pstProd = conProd.prepareStatement(query);
            PreparedStatement pstRestored = conRestored.prepareStatement(query);

            // Execute SQL query
            ResultSet rsProd = pstProd.executeQuery();
            ResultSet rsRestored = pstRestored.executeQuery();

            // Get Meta Data
            ResultSetMetaData rsProdMetaData = rsProd.getMetaData();
            ResultSetMetaData rsRestoredMetaData = rsRestored.getMetaData();

            if (rsProdMetaData.getColumnCount() > rsRestoredMetaData.getColumnCount()) {
                logger.info("Not all columns present in restored database table");
            }

            // Compare the objects returned in each row
            // Query orders the result set (by primary id) descending
            // Check that we have a ResultSet from each database
            if (rsProd == null || rsRestored == null) {
                logger.info("Alert: We have a NULL database.");
                return false;
            }

            // Get primary key name and datatype
            // How to not hard-code primary key datatype?? don't hard-code db or table (wiki and wiki_recentchanges) ???????
            // Plus data type differences ie. Integer Unsigned (SQL) vs int (java)

            DatabaseMetaData metaData = conProd.getMetaData();
            ResultSet primaryKeys = metaData.getPrimaryKeys(dbName, null, tableName);
            primaryKeys.next();

            // get all primary key id
            String primaryKeyId = primaryKeys.getString("COLUMN_NAME"); // returns rc_id for ex

            // Get primary key data type --> but still how to assign it dynamically?
            int c = rsProdMetaData.getColumnCount();
            String dataType = "";
            for (int i = 1; i <= c; i++) {
                if (rsProdMetaData.getColumnName(i).equals(primaryKeyId)) {
                    dataType = rsProdMetaData.getColumnTypeName(i);
                    break;
                }
            }
            System.out.println("primary KyeId: " + primaryKeyId);
            dataType = dataType.toLowerCase();
            System.out.println("lower case: " + dataType);
            //if (dataType.contains("int") || dataType.contains("dec") || dataType.contains("num") || dataType.contains("flo") || dataType.contains("dou") || dataType.contains("bit")) {
                //int rdRestoredStartId = rsRestored.getInt(primaryKeyId);
               // int rsProdStartId = rsProd.getInt(primaryKeyId);
               // System.out.println("restoredStartId: " + rdRestoredStartId + "; rsProdStartId: " + rsProdStartId);
            //}
            //else {
            //    System.out.println("FJDSLKFJDSLFJDSKL:FJDSLFJDSLK:");
            //    logger.debug("Primary key is not numeric: cannot get primary key data type");
             //   return false;
            //}

            // Check that the next row exists before looping
            if (!rsRestored.next() || !rsProd.next()) {
                logger.info("Alert: No row in database.");
                return false;
            }

            // Compare table rows. Report whether or not they all match.
            int column = 1;
            int numColumnsRestored = rsRestoredMetaData.getColumnCount();
            int numColumnsNotValid = 0;
            while (rsProd.next() && rsRestored.next() && column <= numColumnsRestored) {
                final Object rsProdObj = rsProd.getObject(column);
                final Object rsRestoredObj = rsRestored.getObject(column);
                if (!rsProdObj.equals(rsRestoredObj)) {
                    numColumnsNotValid++;
                    logger.info("Validity: Prod and Restored are NOT equal in column {}", column);
                }
                else {
                    logger.info("Validity: Prod and Restored are equal in column {}", column);
                }
                column++;
            }
            System.out.println("numColumnsNotValid = " + numColumnsNotValid);
            if (numColumnsNotValid > 0) {
                logger.info("{} number of rows do not match. Fails validity test.", numColumnsNotValid);
                return false;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dsProd.close();
        dsRestored.close();
        return true;
    }

/*
Sort descending
if there are results from both
    get first result from restored
    loop production results until id matches restored
    check the first results

    loop through rest of results from both


 */

    // Sanity check on number of rows of each ==> delete
    /***         int prodRows = 0;
     int restoredRows = 0;
     while (rsProd.next()) {prodRows++;}
     while (rsRestored.next()) {restoredRows++;}
     System.out.println("num prod rows: " + prodRows + "; num restored rows: " + restoredRows);
     */
/**
    // Number of rows should be the same for prod-restored relative validity check
    // I don't think this check is necessary because of only looping on restored number of columns
            if ((rsProd.isLast() != rsRestored.isLast())) {
        logger.info("Validity: Prod and Restored do NOT have the same number of columns");
    }
            logger.info("Validity: Prod and Restored have the same number of columns");
 */

    /**
     //Loop until production database primary id equals that of the (older) restored database
     // Restore row position to first row
     rsRestored.previous();
     rsProd.previous();

     while (rsProd.next() != rsRestored.next()) {
     rsProd.next();
     }
     //int numColumns = rsProdMetaData.getColumnCount() - rsProd.getInt(1);
     int numColumnsRestored = rsRestoredMetaData.getColumnCount();
     System.out.println("numColumns restored db = " + numColumnsRestored);
     int numColumnsProd = rsProdMetaData.getColumnCount();
     System.out.println("numColumns prod db = " + numColumnsProd);

     int column = 1;
     int numColumnsNotValid = 0;

     while (rsProd.next() && rsRestored.next() && column <= numColumnsRestored) {
     final Object rsProdObj = rsProd.getObject(column);
     final Object rsRestoredObj = rsRestored.getObject(column);
     if (!rsProdObj.equals(rsRestoredObj)) {
     numColumnsNotValid++;
     logger.info("Validity: Prod and Restored are NOT equal in column {}", column);
     }
     logger.info("Validity: Prod and Restored are equal in column {}", column);

     // Number of rows should be the same for prod-restored relative validity check
     if ((rsProd.isLast() != rsRestored.isLast())) {
     logger.info("Validity: Prod and Restored do NOT have the same number of columns at column: {}", column);
     }
     logger.info("Validity: Prod and Restored have the same number of columns at column: {}", column);
     column++;
     }
     System.out.println("numColumnsNotValid = " + numColumnsNotValid);
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
