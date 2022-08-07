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

import javax.xml.transform.Result;
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
     * Check if all columns are present in all tables of chosen database (or in all tables of all databases??)
     *      column checking ==> number of columns, names of columns, data types of columns
     * Check if all rows are the same
     *      getObject returns cell value as an object, so need to do so individually for each col of each row
     */
    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
        if(isValid()) {
            System.out.println("Restored Database is valid by the definitions set forth in this file.");
        } else {System.out.println("Failed. Check logs for more information.");}

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

        // Retain list of failed components and list of passed components of all the checks
        // If any null checks fail, this returns false immediately
        List<String> listOfFailedValidityTests = new ArrayList<>();
        List<String> listOfPassedValidityTests = new ArrayList<>();

        // Connect to database
        try (Connection conProd = dsProd.getConnection();
             Connection conRestored = dsRestored.getConnection();
        ) {

            // Check if all databases are present in the restored database
            // Put all Databases in list ---> doing this or specifying db above?
            PreparedStatement pstProdAllDbs = conProd.prepareStatement("SHOW DATABASES;");
            PreparedStatement pstRestoredAllDbs = conProd.prepareStatement("SHOW DATABASES;");

            ResultSet rsProdAllDbs = pstProdAllDbs.executeQuery();
            ResultSet rsRestoredAllDbs = pstRestoredAllDbs.executeQuery();

            // Check that ResultSets return from each query
            if (rsProdAllDbs == null || rsRestoredAllDbs == null) {
                logger.warn("Alert: NULL Result Set from \"SHOW DATABASES\".");
                return false;
            }

            ResultSetMetaData metaDataProdAllDbs = rsProdAllDbs.getMetaData();
            ResultSetMetaData metaDataRestoredAllDbs = rsRestoredAllDbs.getMetaData();

            // Would like to loop on the tree sets b/c ordering of ResultSets not guaranteed ;;; I'll look into whether or not the order is not natural but is consistent and then it wouldn't matter

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
            if (listDbsNotInRestored.isEmpty()) {
                listOfPassedValidityTests.add("All databases present.");
                logger.info("All databases are present in restored.");
            }
            else {
                for (String s : listDbsNotInRestored) {
                    listOfFailedValidityTests.add("The " +s+ " databases was not restored.");
                    logger.info("{} database is not present.", s);
                }
            }


            // Query for all tables of a specific database
            // Select the database to use --> do we want this to automatically check all databases or be customize-able to only check specific dbs?
            String dbName = "Wiki";
            rsProdAllDbs.first();
            rsRestoredAllDbs.first();
            dbName = rsRestoredAllDbs.getString(1);

            // Check to see if all tables are present in restored database
            PreparedStatement pstProdDb = conProd.prepareStatement("USE " +dbName+ ";");
            PreparedStatement pstRestoredDb = conRestored.prepareStatement("USE " +dbName+ ";");

            ResultSet rsProdUseDb = pstProdDb.executeQuery();
            ResultSet rsRestoredUseDb = pstRestoredDb.executeQuery();

            // Check that ResultSets return from each query
            if (rsProdUseDb == null || rsRestoredUseDb == null) {
                logger.warn("Alert: NULL Result Set from \"USE [DatabaseName]\".");
                return false;
            }

            PreparedStatement pstProdTables = conProd.prepareStatement("SHOW TABLES;");
            PreparedStatement pstRestoredTables = conRestored.prepareStatement("SHOW TABLES;");

            ResultSet rsProdTables = pstProdTables.executeQuery();
            ResultSet rsRestoredTables = pstRestoredTables.executeQuery();

            // Check that ResultSets return from each query
            if (rsProdTables == null || rsRestoredTables == null) {
                logger.warn("Alert: NULL Result Set from \"SHOW TABLES\".");
                return false;
            }

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
                listOfPassedValidityTests.add("All tables present.");
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
                for (String s : listMissingTables) {
                    listOfFailedValidityTests.add("Not all tables present. " +s+ " is missing.");
                    logger.info("\t --> {}", s);}
            }


            // Run CHECK TABLES on all tables of restored database
            List<ResultSet> corruptedDbList = new ArrayList<>();
            while (rsRestoredTables.next()) {
                // Loop through all tables to query CHECK TABLE on each one
                PreparedStatement pstRestoredCHECKTable = conRestored.prepareStatement("CHECK TABLE " +rsRestoredTables.getString(1)+ ";");

                ResultSet rsRestoredCHECKTable = pstRestoredCHECKTable.executeQuery();

                ResultSetMetaData m = rsRestoredCHECKTable.getMetaData();

                // Check that ResultSet returns from each query
                if (rsRestoredCHECKTable == null) {
                    logger.warn("Alert: NULL Result Set from \"CHECK TABLE [TableName]\".");
                    return false;
                }

                rsRestoredCHECKTable.next();
                String checkMsg = rsRestoredCHECKTable.getString(m.getColumnName(4));
                // If Msg_text column returns anything other thank "OK", add to corruptedDbList
                if (!checkMsg.equals("OK")) {
                    corruptedDbList.add(rsRestoredCHECKTable); }
            }
            rsRestoredTables.first();
            // Log results
            if (corruptedDbList.size() == 0) {listOfPassedValidityTests.add("No corrupted tables.");}
            logger.info("There are {} corrupted tables in the database.", corruptedDbList.size());
            for (ResultSet r : corruptedDbList) {
                String corruptedTable = r.getString(1);
                listOfFailedValidityTests.add(corruptedTable+ " is a corrupted table");
                logger.info("Corrupted table: : " + corruptedTable);
            }


            // Check all columns and all rows of all tables of restored database
            // Loop through all tables to query and validate each (only those present in the restored database)
            Iterator<String> iter = listRestoredTables.iterator();
            while (iter.hasNext()) {
                String tableName = iter.next();
                String query = "SELECT * FROM " +dbName +"." +tableName+";";

                // Prepared Statements
                PreparedStatement pstProd = conProd.prepareStatement(query);
                PreparedStatement pstRestored = conRestored.prepareStatement(query);

                // Execute SQL query
                ResultSet rsProdRows = pstProd.executeQuery();
                ResultSet rsRestoredRows = pstRestored.executeQuery();

                // Check that ResultSets return from each query
                if (rsProdRows == null || rsRestoredRows == null) {
                    logger.warn("Alert: NULL Result Set from \"SELECT * FROM [DatabaseName].[DatabaseTable]\".");
                    return false;
                }

                int numProdRows = 0;
                int numRestoredRows = 0;
                while(rsProdRows.next()) {numProdRows++;}
                while (rsRestoredRows.next()) {numRestoredRows++;}
                if (numProdRows == numRestoredRows) {
                    listOfPassedValidityTests.add("The number of rows in " +tableName+ " is the same.");
                    logger.info("The production and restored tables of {} have the same number of rows.", tableName);
                }
                else {
                    logger.info("The tables have a different number of rows. The production database table has {} rows, and the restored has {} rows.", numProdRows, numRestoredRows);
                    listOfFailedValidityTests.add("The number of rows in " +tableName+ " is not the same.");
                }
                rsProdRows.first();
                rsRestoredRows.first();


                // Get Meta Data
                ResultSetMetaData metaDataProdRows = rsProdRows.getMetaData();
                ResultSetMetaData metaDataRestoredRows = rsRestoredRows.getMetaData();

                // Check if columns are the same (ie. schema (column number), column name, and column data type.
                //what is possible with a database restore? can columns mysteriously be added or are we only concerned about any being lost?
                Map<String, Object> mapProdSchemaToMetaData = new HashMap();
                int columnCountProd = metaDataProdRows.getColumnCount();
                int columnNum = 1;
                while (rsProdRows.next() && columnNum <= columnCountProd) {
                    mapProdSchemaToMetaData.put("catalogName", metaDataProdRows.getCatalogName(columnNum));
                    mapProdSchemaToMetaData.put("columnClassName", metaDataProdRows.getColumnClassName(columnNum));
                    mapProdSchemaToMetaData.put("columnCount", metaDataProdRows.getColumnCount());
                    mapProdSchemaToMetaData.put("columnDisplaySize", metaDataProdRows.getColumnDisplaySize(columnNum));
                    mapProdSchemaToMetaData.put("columnLabel", metaDataProdRows.getColumnLabel(columnNum));
                    mapProdSchemaToMetaData.put("columnName", metaDataProdRows.getColumnName(columnNum));
                    mapProdSchemaToMetaData.put("columnType", metaDataProdRows.getColumnType(columnNum));
                    mapProdSchemaToMetaData.put("columnTypeName", metaDataProdRows.getColumnTypeName(columnNum));
                    mapProdSchemaToMetaData.put("precision", metaDataProdRows.getPrecision(columnNum));
                    mapProdSchemaToMetaData.put("scale", metaDataProdRows.getScale(columnNum));
                    mapProdSchemaToMetaData.put("schemaName", metaDataProdRows.getSchemaName(columnNum));
                    mapProdSchemaToMetaData.put("tableName", metaDataProdRows.getTableName(columnNum));
                    columnNum++;
                }
                System.out.println("mapProdSchemaToMetaData : " +mapProdSchemaToMetaData);
                rsProdRows.first();

                Map<String, Object> mapRestoredSchemaToMetaData = new HashMap();
                int columnCountRestored = metaDataRestoredRows.getColumnCount();
                columnNum = 1;
                while (rsRestoredRows.next() && columnNum <= columnCountRestored) {
                    mapRestoredSchemaToMetaData.put("catalogName", metaDataRestoredRows.getCatalogName(columnNum));
                    mapRestoredSchemaToMetaData.put("columnClassName", metaDataRestoredRows.getColumnClassName(columnNum));
                    mapRestoredSchemaToMetaData.put("columnCount", metaDataRestoredRows.getColumnCount());
                    mapRestoredSchemaToMetaData.put("columnDisplaySize", metaDataRestoredRows.getColumnDisplaySize(columnNum));
                    mapRestoredSchemaToMetaData.put("columnLabel", metaDataRestoredRows.getColumnLabel(columnNum));
                    mapRestoredSchemaToMetaData.put("columnName", metaDataRestoredRows.getColumnName(columnNum));
                    mapRestoredSchemaToMetaData.put("columnType", metaDataRestoredRows.getColumnType(columnNum));
                    mapRestoredSchemaToMetaData.put("columnTypeName", metaDataRestoredRows.getColumnTypeName(columnNum));
                    mapRestoredSchemaToMetaData.put("precision", metaDataRestoredRows.getPrecision(columnNum));
                    mapRestoredSchemaToMetaData.put("scale", metaDataRestoredRows.getScale(columnNum));
                    mapRestoredSchemaToMetaData.put("schemaName", metaDataRestoredRows.getSchemaName(columnNum));
                    mapRestoredSchemaToMetaData.put("tableName", metaDataRestoredRows.getTableName(columnNum));
                    columnNum++;
                }
                System.out.println("mapRestoredSchemaToMetaData : " +mapRestoredSchemaToMetaData);
                rsRestoredRows.first();

                List<String> listTablesWithBadColumns = new ArrayList<>();
                if (!mapProdSchemaToMetaData.equals(mapRestoredSchemaToMetaData)) {
                    listTablesWithBadColumns.add(tableName);
                }
                for (String s : listTablesWithBadColumns) {System.out.println("<---- S ----> : " +s);}

                if (listTablesWithBadColumns.size() == 0) {
                    listOfPassedValidityTests.add("Column meta data in " + tableName+ " is the same.");
                    logger.info("{} table does not have any missing or corrupted columns.", tableName);
                }
                else {
                    // Log table names with bad columns (ie. schema (column number) to [columnName, datatype] don't match that of the production database)
                    for (String table : listTablesWithBadColumns) {
                        listOfFailedValidityTests.add("Column meta data in " +table+ " is not the same.");
                        logger.warn("Table {} has missing or corrupted column(s)", table);
                    }
                }


            /**        Collections.sort(listRestoredMetaData, new Comparator<Object>() {
                        @Override
                        public int compare(Object a1, Object a2) {
                            return a1.toString().compareToIgnoreCase(a2.toString());
                        }
                    }); */
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dsProd.close();
        dsRestored.close();

        if (listOfFailedValidityTests.size() == 0) return true;
        else {
            System.out.println();
            System.out.println("-------- SUMMARY OF FAILED VALIDATION TESTING RESULTS --------");
            System.out.println();
            System.out.println("List of Failed Tests:");
            for (String s : listOfFailedValidityTests) {
                System.out.println("\t- "+s);
            }
            System.out.println();
        }
        return false;
    }
}
