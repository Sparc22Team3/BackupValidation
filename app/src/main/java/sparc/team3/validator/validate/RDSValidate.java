package sparc.team3.validator.validate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.config.settings.InstanceSettings;
import sparc.team3.validator.util.Util;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class tests and validates an RDS instance that was restored from a snapshot.
 */
public class RDSValidate implements Callable<Boolean> {
    private final RdsClient rdsClient;
    private final InstanceSettings settings;
    private final Logger logger;
    private DBInstance dbInstanceRestored;
    private HikariDataSource dsProd;
    private HikariDataSource dsRestored;
    private String dbUsername;
    private String dbPassword;
    private Set<String> databasesToCheck;

    /**
     * Instantiates a new Rds validate.
     *
     * @param rdsClient the rds client
     */
    public RDSValidate(RdsClient rdsClient, InstanceSettings settings) {
        this.rdsClient = rdsClient;
        this.settings = settings;
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    @Override
    public Boolean call() throws InterruptedException, ExecutionException{
        setupDatabasesPools();
        boolean dbsExists = checkAllDatabasesExist();

        List<Future<Boolean>> dbCheckResults;
        List<CheckDatabases> dbCheckTasks = new LinkedList<>();

        for(String database : databasesToCheck){
            dbCheckTasks.add(new CheckDatabases(database, dsProd, dsRestored));
        }

        dbCheckResults = Util.executor.invokeAll(dbCheckTasks);

        Boolean allPassed = null;
        for(Future<Boolean> dbCheck : dbCheckResults){
            if(allPassed == null)
                allPassed = dbCheck.get();
            allPassed = allPassed && dbCheck.get();
        }
        if(allPassed == null)
            allPassed = true;

        return dbsExists && allPassed;
    }

    public void setRestoredDbInstance(DBInstance dbInstanceRestored) {
        this.dbInstanceRestored = dbInstanceRestored;
    }

    public void setDBCredentials(String dbUsername, String dbPassword){
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public void setDatabasesToCheck(Set<String> databasesToCheck){
        this.databasesToCheck = databasesToCheck;
    }

    public void setupDatabasesPools() throws InterruptedException{
        HikariConfig configProd = new HikariConfig();
        HikariConfig configRestored = new HikariConfig();

        DescribeDbInstancesRequest describeDbInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(settings.getProductionName()).build();
        DescribeDbInstancesResponse describeDbInstancesResponse = rdsClient.describeDBInstances(describeDbInstancesRequest);
        DBInstance dbInstanceProd = describeDbInstancesResponse.dbInstances().get(0);

        String engine = "jdbc:mariadb://";
        String urlProduction = engine + dbInstanceProd.endpoint().address() + ":" + dbInstanceProd.endpoint().port();
        String urlRestored = engine + dbInstanceRestored.endpoint().address() + ":" + dbInstanceRestored.endpoint().port();

        if (!System.getProperty("user.name").startsWith("ec2")) {
            System.out.println(CLI.ANSI_YELLOW_BACKGROUND + CLI.ANSI_BLACK + "Running this on your local machine, you need to setup an ssh tunnel.  This can be done with the following command:\n" +
                    "\t\tssh -i \"Path\\to\\PrivateKeyFile\" -N -l ec2-user -L 3306:" + dbInstanceProd.endpoint().address() + ":3306 -L 3307:" + dbInstanceRestored.endpoint().address() + ":3306  [EC2 Public Address With Access to Database With SSH Access From This Machine] -v\n" +
                    "The tunnel will stay open as long as the terminal is open.\n" +
                    CLI.ANSI_GREEN_BACKGROUND + "You have two minutes to setup tunnel before validation continues and fails." + CLI.ANSI_RESET);
            Thread.sleep(150000);
            urlProduction = engine + "localhost:3306";
            urlRestored = engine + "localhost:3307";
        }


        configProd.setJdbcUrl(urlProduction);
        configProd.setUsername(dbUsername);
        configProd.setPassword(dbPassword);
        configProd.addDataSourceProperty("cachePrepStmts", "true");
        configProd.addDataSourceProperty("prepStmtCacheSize", "250");
        configProd.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        configProd.addDataSourceProperty("readOnly", true);

        configRestored.setJdbcUrl(urlRestored);
        configRestored.setUsername(dbUsername);
        configRestored.setPassword(dbPassword);
        configRestored.addDataSourceProperty("cachePrepStmts", "true");
        configRestored.addDataSourceProperty("prepStmtCacheSize", "250");
        configRestored.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        configRestored.addDataSourceProperty("readOnly", true);

        dsProd = new HikariDataSource(configProd);
        dsRestored = new HikariDataSource(configRestored);
    }

    private boolean checkAllDatabasesExist() {
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
                if (!setRestoredDbs.contains(s)) {
                    setDbsNotInRestored.add(s);
                }
            }

            if (setDbsNotInRestored.isEmpty()) {
                logger.info("All databases are present in restored.");
                return true;
            }

            // Get names of database(s) missing from restore.
            for (String s : setDbsNotInRestored) {
                logger.warn("{} database is not present in the restore.", s);
                return false;
            }

            rsProdAllDbs.close();
            rsRestoredAllDbs.close();

        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getMessage(), e);
            return false;
        }
        return false;
    }

    private static class CheckDatabases implements Callable<Boolean> {
        private final Logger logger;
        private String db;
        private HikariDataSource dsProd;
        private HikariDataSource dsRestored;

        public CheckDatabases(String db, HikariDataSource dsProd, HikariDataSource dsRestored) {
            this.logger = LoggerFactory.getLogger(this.getClass().getName());
            this.db = db;
            this.dsProd = dsProd;
            this.dsRestored = dsRestored;
        }

        public Boolean call() {

            try (Connection conProd = dsProd.getConnection();
                 Connection conRestored = dsRestored.getConnection();
            ) {
                boolean passedTablePresentCheck = true;
                // Check to see if all tables are present in restored database
                PreparedStatement pstProdDb = conProd.prepareStatement("USE " + db + ";");
                PreparedStatement pstRestoredDb = conRestored.prepareStatement("USE " + db + ";");

                ResultSet rsProdUseDb = pstProdDb.executeQuery();
                ResultSet rsRestoredUseDb = pstRestoredDb.executeQuery();

                // Check that ResultSets return from each query
                if (rsProdUseDb == null || rsRestoredUseDb == null) {
                    logger.warn("Validation Error: NULL Result Set from \"USE [DatabaseName]\".");
                    return false;
                }

                PreparedStatement pstProdTables = conProd.prepareStatement("SHOW TABLES;");
                PreparedStatement pstRestoredTables = conRestored.prepareStatement("SHOW TABLES;");

                ResultSet rsProdTables = pstProdTables.executeQuery();
                ResultSet rsRestoredTables = pstRestoredTables.executeQuery();

                // Check that ResultSets return from each query
                if (rsProdTables == null || rsRestoredTables == null) {
                    logger.warn("Validation Error: NULL Result Set from \"SHOW TABLES\".");
                    return false;
                }

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
                    if (!setRestoredTables.contains(s)) {
                        setTablesNotInRestored.add(s);
                    }
                }

                if (!setTablesNotInRestored.isEmpty()) {
                    // Get names of tables missing from restored database
                    logger.warn("Validation Error: The databases have a different number of tables. The production database has {} tables, and the restored has {} tables.", setProdTables.size(), setRestoredTables.size());
                    passedTablePresentCheck = false;
                    for (String s : setTablesNotInRestored) {
                        logger.warn("Validation Error: The {} table is missing from the restored database.", s);
                    }
                }

                if(passedTablePresentCheck)
                    logger.info("Validated both databases have {} tables.", setProdTables.size());

                boolean passedCheckTables = true;
                // Run CHECK TABLES on all tables of restored database
                List<String> corruptedDbList = new ArrayList<>();
                Iterator<String> iter = setRestoredTables.iterator();
                while (iter.hasNext()) {
                    // Loop through all tables to query CHECK TABLE on each one
                    PreparedStatement pstRestoredCHECKTable = conRestored.prepareStatement("CHECK TABLE " + iter.next() + ";");
                    ResultSet rsRestoredCHECKTable = pstRestoredCHECKTable.executeQuery();

                    // Check that ResultSet returns from each query
                    if (rsRestoredCHECKTable == null) {
                        logger.warn("Validation Error: NULL Result Set from \"CHECK TABLE [TableName]\".");
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

                if (corruptedDbList.size() != 0) {
                    //Get names of corrupted tables
                    for (String s : corruptedDbList) {
                        logger.warn("Validation Error: {} table is corrupted." + s);
                        passedCheckTables = false;
                    }
                }

                if(passedCheckTables)
                    logger.info("Validated no corrupted tables in the restored database.");


                boolean passedNumRowsCheck = true;
                // Check the metadata of all tables of restored database
                // Loop through all tables to query and validate each (only those present in the restored database)
                iter = setRestoredTables.iterator();
                while (iter.hasNext()) {
                    String tableName = iter.next();
                    if(tableName.toLowerCase().contains("cache") || tableName.toLowerCase().contains("session"))
                        continue;
                    String query = "SELECT COUNT(*) AS numRows FROM " + db + "." + tableName + ";";

                    // Prepared Statements
                    PreparedStatement pstProd = conProd.prepareStatement(query);
                    PreparedStatement pstRestored = conRestored.prepareStatement(query);

                    // Execute SQL query
                    ResultSet rsProdRows = pstProd.executeQuery();
                    ResultSet rsRestoredRows = pstRestored.executeQuery();

                    // Check that ResultSets return from each query
                    if (rsProdRows == null || rsRestoredRows == null) {
                        logger.warn("Validation Error: NULL Result Set from \"SELECT * FROM [DatabaseName].[DatabaseTable]\".");
                        return false;
                    }

                    rsProdRows.next();
                    int numProdRows = rsProdRows.getInt("numRows");

                    rsRestoredRows.next();
                    int numRestoredRows = rsRestoredRows.getInt("numRows");

                    if (numProdRows != numRestoredRows) {
                        logger.warn("Validation Error: The number of rows in {} is not the same. The production database table has {} rows, and the restored has {} rows.", tableName, numProdRows, numRestoredRows);
                        passedNumRowsCheck = false;
                    }
                }

                if(passedNumRowsCheck)
                    logger.info("Validated that production database is ahead of restored database. (Production database has the same or more rows than restored database.)");


                boolean passedSchemaCheck = true;
                // Running query again to get to row 0 of result set to check metadata
                iter = setRestoredTables.iterator();
                while (iter.hasNext()) {
                    String tableName = iter.next();
                    String query = "SELECT * FROM " + db + "." + tableName + " LIMIT 1;";

                    // Prepared Statements
                    PreparedStatement pstProd = conProd.prepareStatement(query);
                    PreparedStatement pstRestored = conRestored.prepareStatement(query);

                    // Execute SQL query
                    ResultSet rsProdRows = pstProd.executeQuery();
                    ResultSet rsRestoredRows = pstRestored.executeQuery();

                    // Check that ResultSets return from each query
                    if (rsProdRows == null || rsRestoredRows == null) {
                        logger.warn("Validation Error: NULL Result Set from \"SELECT * FROM [DatabaseName].[DatabaseTable]\".");
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

                    if (listTablesWithBadColumns.size() != 0) {
                        for (String s : listTablesWithBadColumns) {
                            logger.warn("Table {} has missing or corrupted column(s)", s);
                        }
                        passedSchemaCheck = false;
                    }

                    rsProdRows.close();
                    rsRestoredRows.close();
                }
                if(passedSchemaCheck)
                    logger.info("Validated that tables' structures are the same.");



                rsProdUseDb.close();
                rsRestoredUseDb.close();
                rsProdTables.close();
                rsRestoredTables.close();

                return passedTablePresentCheck && passedCheckTables && passedNumRowsCheck && passedSchemaCheck;
            } catch (SQLException e) {
                logger.error("SQL Error: {}", e.getMessage(), e);
                return false;
            }
        }
    }
}
