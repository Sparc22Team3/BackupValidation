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
import sparc.team3.validator.config.ConfigLoader;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.Settings;
import sparc.team3.validator.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.sql.*;
import java.util.*;

// -Dsql-log-level=INFO
public class SQL {

    /**
     * Check if all databases are present in restored
     * Choose specific db to test further
     * Check if all tables are present in database
     * Run CHECK TABLES for corruption on all tables of specified database
     * Check if all metadata of all columns in all tables of chosen database
     * Check if number of rows of every table is the same
     * If any null checks fail, this method logs that and returns false immediately
     */
    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
        if (isRdsRestoredValid()) {
            System.out.println("Restored Database is valid by the definitions set forth in this file.");
        } else {
            System.out.println("Failed. Check logs for more information.");
        }

    }


    private static boolean isRdsRestoredValid() throws SQLException, IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(SQL.class);
        ConfigLoader configLoader = new ConfigLoader(new CLI(), Util.DEFAULT_CONFIG.toString());
        Settings settings = configLoader.loadSettings();
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

        // Retain list of failed and passed validity tests for all checks
        List<String> listOfFailedValidityTests = new ArrayList<>();
        List<String> listOfPassedValidityTests = new ArrayList<>();

        // Connect to database
        try (Connection conProd = dsProd.getConnection();
             Connection conRestored = dsRestored.getConnection();
        ) {

            // Check if all databases are present in the restored database
            PreparedStatement pstProdAllDbs = conProd.prepareStatement("SHOW DATABASES;");
            PreparedStatement pstRestoredAllDbs = conRestored.prepareStatement("SHOW DATABASES;");

            ResultSet rsProdAllDbs = pstProdAllDbs.executeQuery();
            ResultSet rsRestoredAllDbs = pstRestoredAllDbs.executeQuery();

            // Check that ResultSets return from each query
            if (rsProdAllDbs == null || rsRestoredAllDbs == null) {
                logger.warn("Alert: NULL Result Set from \"SHOW DATABASES\".");
                return false;
            }

            ResultSetMetaData metaDataProdAllDbs = rsProdAllDbs.getMetaData();
            ResultSetMetaData metaDataRestoredAllDbs = rsRestoredAllDbs.getMetaData();

            String rsProdDbName = "";
            Set<String> setProdDbs = new TreeSet<>();
            while (rsProdAllDbs.next()) {
                rsProdDbName = rsProdAllDbs.getString(1);
                setProdDbs.add(rsProdDbName);
            }

            String rsRestoredDbName = "";
            Set<String> setRestoredDbs = new TreeSet<>();
            while (rsRestoredAllDbs.next()) {
                rsRestoredDbName = rsRestoredAllDbs.getString(1);
                setRestoredDbs.add(rsRestoredDbName);
            }

            Set<String> setDbsNotInRestored = new TreeSet<>();
            for (String s : setProdDbs) {
                if (!setRestoredDbs.contains(s)) { setDbsNotInRestored.add(s); }
            }

            if (setDbsNotInRestored.isEmpty()) {
                listOfPassedValidityTests.add("All databases present.");
                logger.info("All databases are present in restored.");
            } else {
                // Get names of database(s) missing from restore.
                for (String s : setDbsNotInRestored) {
                    listOfFailedValidityTests.add("The " + s + " database was not restored.");
                    logger.warn("{} database is not present in the restore.", s);
                }
            }


            // Query for all tables of a specific database
            String dbName = "Wiki";
            //dbName = rsRestoredAllDbs.getString(1);

            // Check to see if all tables are present in restored database
            PreparedStatement pstProdDb = conProd.prepareStatement("USE " + dbName + ";");
            PreparedStatement pstRestoredDb = conRestored.prepareStatement("USE " + dbName + ";");

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
            String rsProdTableName = "";
            Set<String> setProdTables = new TreeSet<>();
            while (rsProdTables.next()) {
                rsProdTableName = rsProdTables.getString(1);
                setProdTables.add(rsProdTableName);
            }

            String rsRestoredTableName = "";
            Set<String> setRestoredTables = new TreeSet<>();
            while (rsRestoredTables.next()) {
                rsRestoredTableName = rsRestoredTables.getString(1);
                setRestoredTables.add(rsRestoredTableName);
            }

            Set<String> setTablesNotInRestored = new TreeSet<>();
            for (String s : setProdTables) {
                if (!setRestoredTables.contains(s)) { setTablesNotInRestored.add(s); }
            }

            if (setTablesNotInRestored.isEmpty()) {
                listOfPassedValidityTests.add("All tables present.");
                logger.info("Both databases have {} tables.", setProdTables.size());
            }
            else {
                // Get names of tables missing from restored database
                logger.warn("The databases have a different number of tables. The production database has {} tables, and the restored has {} tables.", setProdTables.size(), setRestoredTables.size());
                for (String s : setTablesNotInRestored) {
                    listOfFailedValidityTests.add("Not all tables present. " + s + " is missing.");
                    logger.warn("The {} table is missing from the restored database.", s);
                }
            }


            // Run CHECK TABLES on all tables of restored database
            List<String> corruptedDbList = new ArrayList<>();
            Iterator<String> iter = setRestoredTables.iterator();
            while(iter.hasNext()) {
                // Loop through all tables to query CHECK TABLE on each one
                PreparedStatement pstRestoredCHECKTable = conRestored.prepareStatement("CHECK TABLE " + iter.next() + ";");
                ResultSet rsRestoredCHECKTable = pstRestoredCHECKTable.executeQuery();

                // Check that ResultSet returns from each query
                if (rsRestoredCHECKTable == null) {
                    logger.warn("Alert: NULL Result Set from \"CHECK TABLE [TableName]\".");
                    return false;
                }

                ResultSetMetaData metaDataRestoredCHECKTable = rsRestoredCHECKTable.getMetaData();

                rsRestoredCHECKTable.next();
                String rsRestoredCorruptedTableName = "";
                // If Msg_text column (4) returns anything other thank "OK", add to corruptedDbList
                String checkMsg = rsRestoredCHECKTable.getString(metaDataRestoredCHECKTable.getColumnName(4));
                if (!checkMsg.equals("OK")) {
                    rsRestoredCorruptedTableName = rsRestoredCHECKTable.getString(1);
                    corruptedDbList.add(rsRestoredCorruptedTableName);
                }
                rsRestoredCHECKTable.close();
            }

            // Log results
            if (corruptedDbList.size() == 0) {
                listOfPassedValidityTests.add("No corrupted tables.");
                logger.info("There are no corrupted tables in the restored database.");
            }
            else {
                //Get names of corrupted tables
                for (String s : corruptedDbList) {
                    listOfFailedValidityTests.add(s + " is a corrupted table.");
                    logger.warn("{} table is corrupted." + s);
                }
            }


            // Check the metadata of all tables of restored database
            // Loop through all tables to query and validate each (only those present in the restored database)
            iter = setRestoredTables.iterator();
            while (iter.hasNext()) {
                String tableName = iter.next();
                String query = "SELECT COUNT(*) AS numRows FROM " + dbName + "." + tableName + ";";

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

                rsProdRows.next();
                int numProdRows = rsProdRows.getInt("numRows");

                rsRestoredRows.next();
                int numRestoredRows = rsRestoredRows.getInt("numRows");

                if (numProdRows == numRestoredRows) {
                    listOfPassedValidityTests.add("The number of rows in " + tableName + " is the same in both databases.");
                    logger.info("The number of rows in {} table is the same in both databases.", tableName);
                } else {
                    listOfFailedValidityTests.add("The number of rows in " + tableName + " is not the same.");
                    logger.warn("The number of rows in {} is not the same. The production database table has {} rows, and the restored has {} rows.", tableName, numProdRows, numRestoredRows);
                }
            }


            // Running query again to get to row 0 of result set to check metadata
            iter = setRestoredTables.iterator();
            while (iter.hasNext()) {
                String tableName = iter.next();
                String query = "SELECT * FROM " + dbName + "." + tableName + " LIMIT 1;";

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

                // Get Meta Data
                ResultSetMetaData metaDataProdRows = rsProdRows.getMetaData();
                ResultSetMetaData metaDataRestoredRows = rsRestoredRows.getMetaData();

                // Store metadata, then compare results
                Map<String, Object> mapProdSchemaToMetaData = new HashMap();
                int columnCountProd = metaDataProdRows.getColumnCount();
                int columnNum = 1;
                rsProdRows.next();
                while (columnNum <= columnCountProd) {
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

                Map<String, Object> mapRestoredSchemaToMetaData = new HashMap();
                int columnCountRestored = metaDataRestoredRows.getColumnCount();
                columnNum = 1;
                rsRestoredRows.next();
                while (columnNum <= columnCountRestored) {
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

                // Store tables that have missing or corrupted metadata
                List<String> listTablesWithBadColumns = new ArrayList<>();
                if (!mapProdSchemaToMetaData.equals(mapRestoredSchemaToMetaData)) {
                    listTablesWithBadColumns.add(tableName);
                }

                if (listTablesWithBadColumns.size() == 0) {
                    listOfPassedValidityTests.add("Column meta data in " + tableName + " is the same.");
                    logger.info("{} table does not have any missing or corrupted columns.", tableName);
                } else {
                    // Log table names with bad columns
                    for (String s : listTablesWithBadColumns) {
                        listOfFailedValidityTests.add("Column meta data in " + s + " is not the same.");
                        logger.warn("Table {} has missing or corrupted column(s)", s);
                    }
                }
                rsProdRows.close();
                rsRestoredRows.close();
            }
            rsProdAllDbs.close();
            rsRestoredAllDbs.close();
            rsProdUseDb.close();
            rsRestoredUseDb.close();
            rsProdTables.close();
            rsRestoredTables.close();


        } catch (SQLException e) {
            logger.error("SQL Exception");
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
                System.out.println("\t- " + s);
            }
            System.out.println();
        }
        return false;
    }
}

