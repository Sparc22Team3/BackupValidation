package sparc.team3.validator;

import com.sun.source.tree.Tree;
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
     * --> Add null checks to all checks
     * Check if all databases are present in restored
     * Choose specific db to test further or loop and automatically check all databases????
     * Check if all tables are present in database(s)
     * CHECK TABLES for corruption (all tables of all dbs or just all tables of chosen db?)
     * Check if all columns are present in on chosen restored table (or in all tables of chosen database or in all tables of all databases??)
     *      column checking ==> number of columns, names of columns, data types of columns
     * Check if all rows are the same
     *      don't have a definition of that yet ==> getObject returns column value as an object, so would need to do so individually for each col of each row
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

        // Select the database to use --> do we want this to automatically check all databases or be customize-able to only check specific dbs?
        String dbName = "Wiki";
        //String tableName = "";
        //tableName = "wiki_recentchanges"; //primaryKey = "rc_id";
        //tableName = "wiki_revision"; //primaryKey = "rev_id";
        //tableName = "wiki_text"; //primaryKey = "old_id";
        //tableName = "wiki_updatelog"; //primaryKey = "ul_key";
        //tableName = "wiki_redirect";



        // get back column names and datatypes and all else in the metadata --> called schema or structure for all the info that isn't data --> how they are all set up
        try (Connection conProd = dsProd.getConnection();
             Connection conRestored = dsRestored.getConnection();
        ) {

            // Check if all databases are present in the restored database
            // Put all Databases in list ---> doing this or specifying db above?
            PreparedStatement pstProdAllDbs = conProd.prepareStatement("SHOW DATABASES;");
            PreparedStatement pstRestoredAllDbs = conProd.prepareStatement("SHOW DATABASES;");

            ResultSet rsProdAllDbs = pstProdAllDbs.executeQuery();
            ResultSet rsRestoredAllDbs = pstRestoredAllDbs.executeQuery();

            ResultSetMetaData metaDataProdAllDbs = rsProdAllDbs.getMetaData();
            ResultSetMetaData metaDataRestoredAllDbs = rsRestoredAllDbs.getMetaData();

            // Would like to loop on the tree sets b/c ordering of ResultSets not guaranteed ;;; I'll look into whether or not the order is not natural but is consistent and then it wouldn't matter
            // Code saved below in note if need to re-work it to use it

            String rsProdDbName = "";
            String rsRestoredDbName = "";
            List<String> listDbsNotInRestored = new ArrayList<>();
            while (rsProdAllDbs.next() && rsRestoredAllDbs.next()) {
                rsProdDbName = rsProdAllDbs.getString(1);
                rsRestoredDbName = rsRestoredAllDbs.getString(1);
                if (!rsProdDbName.equals(rsRestoredDbName)) {
                    listDbsNotInRestored.add(rsRestoredDbName);
                }
            }
            if (listDbsNotInRestored.isEmpty()) {logger.info("All databases are present in restored.");}
            else {
                for (String s : listDbsNotInRestored) {
                    logger.info("{} database is not present.", s);
                }
            }


            // Query for all tables of a specific database
                // Choose database to check or loop on all ??
            rsProdAllDbs.first();
            rsRestoredAllDbs.first();
            dbName = rsRestoredAllDbs.getString(1);

            // Check to see if all tables are present in restored database
            PreparedStatement pstProdDb = conProd.prepareStatement("USE " +dbName+ ";");
            PreparedStatement pstRestoredDb = conRestored.prepareStatement("USE " +dbName+ ";");

            ResultSet rsProdUseDb = pstProdDb.executeQuery();
            ResultSet rsRestoredUseDb = pstRestoredDb.executeQuery();

            PreparedStatement pstProdTables = conProd.prepareStatement("SHOW TABLES;");
            PreparedStatement pstRestoredTables = conRestored.prepareStatement("SHOW TABLES;");

            ResultSet rsProdTables = pstProdTables.executeQuery();
            ResultSet rsRestoredTables = pstRestoredTables.executeQuery();

            ResultSetMetaData metaDataProdTables = rsProdTables.getMetaData();
            ResultSetMetaData metaDataRestoredTables = rsRestoredTables.getMetaData();

            // Check if tables are the same.
            List<String> listProdTables = new ArrayList<>();
            List<String> listRestoredTables = new ArrayList<>();
            while (rsProdTables.next()) {
                listProdTables.add(rsProdTables.getString(1));
            }
            rsProdTables.first();
            while (rsRestoredTables.next()) {
                listRestoredTables.add(rsRestoredTables.getString(1));
            }
            rsRestoredTables.first();
            if (listProdTables.equals(listRestoredTables) == true) {
                logger.info("Both databases have the same number of tables ({} tables).", listProdTables.size());
            }
            else {
                logger.info("The databases have a different number of tables. The production database has {} tables, and the restored has {} tables.", listProdTables.size(), listRestoredTables.size());
                // Get names of tables missing from restored database
                List<String> listMissingTables = new ArrayList<>();
                for (int counter = 0; counter < listProdTables.size(); counter++) {
                    if (!listRestoredTables.contains(listProdTables.get(counter))) {
                        listMissingTables.add(listProdTables.get(counter));
                    }
                }
                // Log missing table names
                logger.info("The following table(s) are missing from the restored database:");
                for (String s : listMissingTables) {logger.info("\t --> {}", s);}
            }


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
            rsRestoredTables.first();
            // Log results
            logger.info("There are {} corrupted tables in the database.", corruptedDbList.size());
            for (ResultSet r : corruptedDbList) {logger.info("Corrupted table: : " + r.getString(1));}


            // Check if all columns present and if all rows match of all tables of restored database
            // Loop through all tables to query and validate each
            for (String tableName : listRestoredTables) {
                String query = "SELECT * FROM " +dbName +"." +tableName+";";

                // Prepared Statements
                PreparedStatement pstProd = conProd.prepareStatement(query);
                PreparedStatement pstRestored = conRestored.prepareStatement(query);

                // Execute SQL query
                ResultSet rsProdRows = pstProd.executeQuery();
                ResultSet rsRestoredRows = pstRestored.executeQuery();

                // Get Meta Data
                ResultSetMetaData metaDataProdRows = rsProdRows.getMetaData();
                ResultSetMetaData metaDataRestoredRows = rsRestoredRows.getMetaData();

                // Check if columns are the same.
                Map<String, String> mapProdColumnNamesToDatatype = new TreeMap();
                Map<String, String> mapRestoredColumnNamesToDatatype = new TreeMap<>();
                int columnCount = metaDataProdRows.getColumnCount();
                int columnNum = 1;
                while (rsProdRows.next() && columnNum <= columnCount) {
                    mapProdColumnNamesToDatatype.put(metaDataProdRows.getColumnName(1), metaDataProdRows.getColumnTypeName(1));
                    columnNum++;
                }
                System.out.println(">------------<");
                System.out.println(mapProdColumnNamesToDatatype);
                rsProdTables.first();
                while (rsRestoredTables.next()) {
                    listRestoredTables.add(rsRestoredTables.getString(1));
                }
                rsRestoredTables.first();
                if (listProdTables.equals(listRestoredTables) == true) {
                    logger.info("Both databases have the same number of tables ({} tables).", listProdTables.size());
                }
                else {
                    logger.info("The databases have a different number of tables. The production database has {} tables, and the restored has {} tables.", listProdTables.size(), listRestoredTables.size());
                    // Get names of tables missing from restored database
                    List<String> listMissingTables = new ArrayList<>();
                    for (int counter = 0; counter < listProdTables.size(); counter++) {
                        if (!listRestoredTables.contains(listProdTables.get(counter))) {
                            listMissingTables.add(listProdTables.get(counter));
                        }
                    }
                    // Log missing table names
                    logger.info("The following table(s) are missing from the restored database:");
                    for (String s : listMissingTables) {logger.info("\t --> {}", s);}
                }


            }
            /*String query = "SELECT * FROM " +dbName +"." +tableName+";";

            // Prepared Statements
            PreparedStatement pstProd = conProd.prepareStatement(query);
            PreparedStatement pstRestored = conRestored.prepareStatement(query);

            // Execute SQL query
            ResultSet rsProd = pstProd.executeQuery();
            ResultSet rsRestored = pstRestored.executeQuery();

            // Get Meta Data
            ResultSetMetaData rsProdMetaData = rsProd.getMetaData();
            ResultSetMetaData rsRestoredMetaData = rsRestored.getMetaData();

            // Check metadata

            // Check number of all columns
            int prodColumnCount = rsProdMetaData.getColumnCount();
            int restoredColumnCount = rsRestoredMetaData.getColumnCount();
            System.out.println("prodColumnCount: " + prodColumnCount);
            System.out.println("restoredColumnCount: " + restoredColumnCount);

            //what is possible with a database restore? can columns mysteriously be added or are we only concerned about any being lost?
            if (prodColumnCount > restoredColumnCount) {
                logger.info("Not all columns present in restored database table.");
            } else {logger.info("The databases have the same number of columns.");}

            // Of those columns in restored database, do the names match that of the names in the production database
            int columnCount = 1;
            while (rsProd.next() && rsRestored.next() && prodColumnCount <= columnCount) {
                System.out.println("chking prod: " +rsProdMetaData.getColumnName(1));
                System.out.println("chking restored: " +rsRestoredMetaData.getColumnName(1));
            }

            // Compare the objects returned in each column of each row
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
            System.out.println("primary KeyId name: " + primaryKeyId);
            dataType = dataType.toLowerCase();
            System.out.println("primary KeyId type: " + dataType);
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
            // This only checks on those present in the restored database. Restored database doesn't include changes made since restore.
            rsProd.first();
            rsRestored.first();
            int column = 1;
            int numColumnsRestored = rsRestoredMetaData.getColumnCount();
            int numColumnsNotValid = 0;
            while (rsProd.next() && rsRestored.next() && column <= numColumnsRestored) {
                final Object rsProdObj = rsProd.getObject(column); //NOPE. This just returns the value of the column for this row.
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
            }*/

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dsProd.close();
        dsRestored.close();
        return true;
    }


    /**    DatabaseMetaData metaDataProd = conProd.getMetaData();
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
     */

// for putting all dbs into set if necessary for ordering situation from result sets
    /**    Set<ResultSet> treeSetProdAllDbs = new TreeSet<>();
     while(rsProdAllDbs.next()) {
     treeSetProdAllDbs.add(rsProdAllDbs);
     //System.out.println(":: " + rsProdAll.getString(1));
     }
     rsProdAllDbs.first();

     Set<ResultSet> treeSetRestoredAllDbs = new TreeSet<>();
     while(rsRestoredAllDbs.next()) {
     treeSetRestoredAllDbs.add(rsRestoredAllDbs);
     System.out.println(":: " +rsRestoredAllDbs.getString(1));
     }
     rsRestoredAllDbs.first();

     // Compare lists of databases (looping on the tree sets b/c ordering of ResultSets not guaranteed)
     Iterator<ResultSet> iterProdAllDbs = treeSetProdAllDbs.iterator();
     Iterator<ResultSet> iterRestoredAllDbs = treeSetRestoredAllDbs.iterator();
     List<ResultSet> listDbsNotInRestored = new ArrayList<>();
     while(iterRestoredAllDbs.hasNext() && iterProdAllDbs.hasNext()) {
     String prodDbName = iterProdAllDbs.next().getString(1);
     String restoredDbName = iterRestoredAllDbs.next().getString(1);
     if (prodDbName.equals(restoredDbName)) {
     System.out.println("yes: " +restoredDbName);
     }
     }
     List<ResultSet> listProdAllDbs = new ArrayList<>(treeSetProdAllDbs);
     dbName = listProdAllDbs.get(0).getString(1);*/

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
