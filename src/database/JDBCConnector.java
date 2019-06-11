package database;

import java.sql.*;

public class JDBCConnector extends DBConfigs {

    private final String DB_URL = "jdbc:postgresql://"+dbHost+(dbPort.length() == 0 ?"":":"+dbPort)+"/" + dbName;
    private final String USER = dbUser;
    private final String PASS = dbPassword;


    private static final FileLogger logger = new FileLogger();

    private Connection connection;

    public JDBCConnector() {
        LoadingPrinter loadingPrinter = new LoadingPrinter();
        boolean firstCreation = true;
        while(true) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(DB_URL, USER, PASS);
                loadingPrinter.stop();
                System.out.println("\nСоединение с БД установлено!");
                break;
            } catch (ClassNotFoundException e) {
                System.err.println("Ошибка JDBC драйвер не найден!");
                System.exit(1);
            } catch (SQLException e) {
                if (e.getSQLState().equals("08001")) {
                    if(firstCreation) {
                        System.out.println("Ошибка: невозможно подключиться к серверу БД, так как сервер не доступен!");
                        System.out.println("Попытка подключения:");
                        new Thread(loadingPrinter::printLoadingLine).start();
                        firstCreation = false;
                    }
                }
            }
        }

        String users_table = String.format("CREATE TABLE IF NOT EXISTS %s(\n" +
                "%s INT,\n" +
                "%s VARCHAR,\n" +
                "%s VARCHAR,\n" +
                "%s VARCHAR,\n" +
                "PRIMARY KEY(%s)\n" +
                ");", DBConst.USERS_TABLE, DBConst.TABLES_ID, DBConst.USERS_LOGIN, DBConst.USERS_PASSWORD, DBConst.USERS_EMAIL,
                DBConst.TABLES_ID);

        String trolls_table = String.format("CREATE TABLE IF NOT EXISTS %s(\n" +
                "%s INT,\n" +
                "%s VARCHAR,\n" +
                "%s INT,\n" +
                "%s INT,\n" +
                "%s BOOLEAN,\n" +
                "%s BOOLEAN,\n" +
                "%s VARCHAR,\n" +
                "%s VARCHAR,\n" +
                "PRIMARY KEY(%s)\n" +
                ");",DBConst.TROLLS_TABLE,DBConst.TABLES_ID,DBConst.TROLL_NAME,DBConst.TROLL_AGE,DBConst.TROLL_HP,
                    DBConst.TROLL_IS_SIT,DBConst.TROLL_IS_SAD,DBConst.TROLL_INITDATE,DBConst.TROLL_USER,DBConst.TABLES_ID);

        String things_table = String.format("CREATE TABLE IF NOT EXISTS %s(\n" +
                "%s INT,\n" +
                "%s INT,\n" +
                "%s VARCHAR,\n" +
                "%s VARCHAR,\n" +
                "%s INT,\n" +
                        "PRIMARY KEY(%s)\n" +
                        ");",DBConst.THINGS_TABLE,DBConst.TABLES_ID,DBConst.THING_TROLL_ID,DBConst.THING_NAME,
                            DBConst.THING_CONDITION,DBConst.THING_WEIGHT,DBConst.TABLES_ID);

        execSQLUpdate(users_table);
        execSQLUpdate(trolls_table);
        execSQLUpdate(things_table);
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Executing a query and returning resultSet. After calling this method and processing resultSet, you must call PreparedStatement.close() method.
     * @param query SQL query
     * @return Pair of PreparedStatement and ResultSet
     */
    public Pair<PreparedStatement, ResultSet> execSQLQuery(String query){
        try {
            PreparedStatement statement = connection.
                    prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            return new Pair<>(statement, resultSet);
        } catch (SQLException e) {
            if (e.getSQLState().equals("08001") || e.getSQLState().equals("08006")) {
                resetConnection();
                return execSQLQuery(query);
            }
            else {
                logger.log(e.getMessage() + "\nSqlState = " + e.getSQLState());
                return null;
            }
        }
    }

    private boolean resetConnection(){
        LoadingPrinter loadingPrinter = new LoadingPrinter();
        boolean firstCreation = true;
        while(true) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(DB_URL, USER, PASS);
                loadingPrinter.stop();
                System.out.println("\nСоединение с БД установлено!");
                return true;
            } catch (ClassNotFoundException e) {
                System.err.println("Ошибка JDBC драйвер не найден!");
                System.exit(1);
            } catch (SQLException e) {
                if (e.getSQLState().equals("08001")) {
                    if(firstCreation) {
                        new Thread(loadingPrinter::printLoadingLine).start();
                        firstCreation = false;
                    }
                }
            }
        }
    }

    public boolean execSQLUpdate(String query){
        try (PreparedStatement statement = connection.
                prepareStatement(query)) {
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("08001") || e.getSQLState().equals("08006")) {
                resetConnection();
                return execSQLUpdate(query);
            }
            else {
                logger.log(e.getMessage() + "\nSqlState = " + e.getSQLState());
                return false;
            }
        }
    }
}