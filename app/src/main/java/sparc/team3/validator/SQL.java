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
     * Check if all columns are present in all tables of chosen database (or in all tables of all databases??)
     *      column checking ==> number of columns, names of columns, data types of columns
     * Check if all rows are the same
     *      getObject returns cell value as an object, so need to do so individually for each col of each row
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
            if (listDbsNotInRestored.isEmpty()) {logger.info("All databases are present in restored.");}
            else {
                for (String s : listDbsNotInRestored) {
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
            logger.info("There are {} corrupted tables in the database.", corruptedDbList.size());
            for (ResultSet r : corruptedDbList) {logger.info("Corrupted table: : " + r.getString(1));}


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

                // Get Meta Data
                ResultSetMetaData metaDataProdRows = rsProdRows.getMetaData();
                ResultSetMetaData metaDataRestoredRows = rsRestoredRows.getMetaData();

                // Check if columns are the same (ie. schema (column number), column name, and column data type.
                //what is possible with a database restore? can columns mysteriously be added or are we only concerned about any being lost?
                Map<String, List> mapProdSchemaToColumnNameAndDatatype = new HashMap();
                List<String> listProdColumnNamesAndDatatypes = new ArrayList<>();
                int columnCount = metaDataProdRows.getColumnCount();
                int columnNum = 1;
                while (rsProdRows.next() && columnNum <= columnCount) {
                    listProdColumnNamesAndDatatypes.add(metaDataProdRows.getColumnName(columnNum));
                    listProdColumnNamesAndDatatypes.add(metaDataProdRows.getColumnTypeName(columnNum));
                    mapProdSchemaToColumnNameAndDatatype.put(metaDataProdRows.getSchemaName(columnNum), listProdColumnNamesAndDatatypes);
                    columnNum++;
                }
                rsProdRows.first();

                Map<String, List> mapRestoredSchemaToColumnNameAndDatatype = new HashMap();
                List<String> listRestoredColumnNamesAndDatatypes = new ArrayList<>();
                columnCount = metaDataRestoredRows.getColumnCount();
                columnNum = 1;
                while (rsRestoredRows.next() && columnNum <= columnCount) {
                    listRestoredColumnNamesAndDatatypes.add(metaDataRestoredRows.getColumnName(columnNum));
                    listRestoredColumnNamesAndDatatypes.add(metaDataRestoredRows.getColumnTypeName(columnNum));
                    mapRestoredSchemaToColumnNameAndDatatype.put(metaDataRestoredRows.getSchemaName(columnNum), listRestoredColumnNamesAndDatatypes);
                    columnNum++;
                }
                rsRestoredRows.first();

                List<String> listTablesWithBadColumns = new ArrayList<>();
                if (!mapProdSchemaToColumnNameAndDatatype.equals(mapRestoredSchemaToColumnNameAndDatatype)) {
                    listTablesWithBadColumns.add(tableName);
                }
                // Log table names with bad columns (ie. schema (column number) to [columnName, datatype] don't match that of the production database)
                for (String table : listTablesWithBadColumns) {logger.warn("Table {} has corrupted column(s)", table);}

                // Check if the rows are the same
                //while (rsProdRows.next()) {System.out.println("prod rows: " + rsProdRows.getObject(2));}

            }


            /**
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
}
