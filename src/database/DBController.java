package database;

import main.Main;
import network.User;
import org.postgresql.core.SqlCommand;
import shared.Thing;
import shared.Troll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DBController {

    private JDBCConnector connector;

    public DBController(JDBCConnector connector) {
        this.connector = connector;
    }

    public void addTrollToDb(Troll troll, User user) {
        connector.execSQLQuery(troll.getInsertSqlQuery().replace("USER_TO_REPLACE", user.getLogin()).replace("ID_TO_REPLACE", String.valueOf(troll.hashCode())));
        troll.getThingsInHands().forEach(t -> connector.execSQLQuery(t.getInsertSqlQuery().replace("ID_TO_REPLACE", String.valueOf(t.hashCode())).replace("TROLL_ID_TO_REPLACE", String.valueOf(troll.hashCode()))));
    }

    public void deleteTrollFromDb(Troll troll, User user) {
        connector.execSQLQuery(troll.getDeleteSqlQuery().replace("USER_TO_REPLACE", user.getLogin()));
        troll.getThingsInHands().forEach(t -> connector.execSQLQuery(t.getDeleteSqlQuery().replace("TROLL_ID_TO_REPLACE", String.valueOf(troll.hashCode()))));
    }

    public void addUserToDb(User user) {
        connector.execSQLQuery(user.getInsertSqlQuery());
    }

    public void deleteUserFromDB(User user) {
        connector.execSQLQuery(user.getDeleteSqlQuery());
    }

    public List<Pair<Troll, String>> getTrollsFromDB() throws SQLException{
        Pair<PreparedStatement, ResultSet> pair = connector.execSQLQuery("SELECT * FROM trolls;");
        ResultSet trollsSet = pair.getValue();
        List<Pair<Troll, String>> result = new ArrayList<>();
        while (trollsSet.next()) {
            int id = trollsSet.getInt(DBConst.TABLES_ID);
            String login = trollsSet.getString(DBConst.TROLL_USER);
            String name = trollsSet.getString(DBConst.TROLL_NAME);
            int age = trollsSet.getInt(DBConst.TROLL_AGE);
            int HP = trollsSet.getInt(DBConst.TROLL_HP);
            boolean isSit = trollsSet.getBoolean(DBConst.TROLL_IS_SIT);
            boolean isSad = trollsSet.getBoolean(DBConst.TROLL_IS_SAD);
            OffsetDateTime time = OffsetDateTime.parse(trollsSet.getString(DBConst.TROLL_INITDATE));
            Troll troll = new Troll(name, age, HP, isSit, isSad, time);
            Pair<PreparedStatement, ResultSet> pair2 = connector.execSQLQuery("SELECT * FROM things;");
            ResultSet thingsSet = pair2.getValue();
            while (thingsSet.next()) {
                if (thingsSet.getInt(DBConst.THING_TROLL_ID) == id) {
                    String thingName = thingsSet.getString(DBConst.THING_NAME);
                    Thing.Condition thingCondition = Thing.Condition.valueOf(thingsSet.getString(DBConst.THING_CONDITION));
                    int thingWeight = thingsSet.getInt(DBConst.THING_WEIGHT);
                    Thing thing = new Thing(thingName, thingCondition, thingWeight);
                    troll.addThing(thing);
                }
            }
            result.add(new Pair<>(troll, login));
        }
        return result;
    }

    public void synchronizeCollection() {
        Pair<PreparedStatement, ResultSet> pair = connector.execSQLQuery("SELECT COUNT(id) FROM trolls;");
        ResultSet resultSet = pair.getValue();
        boolean isEquals = false;
        try {
            while (resultSet.next()) {
                isEquals = resultSet.getInt("count") == Main.objectsLinkedDeque.size();
            }
        } catch (SQLException ex) {
            isEquals = false;
        }

        if (!isEquals) {
            try {
                Main.objectsLinkedDeque.clear();
                Main.objectsLinkedDeque.addAll(getTrollsFromDB());
            } catch (SQLException ex) {
                System.err.println("Ошибка при синхронизации с БД");
            }
        }
    }

    public void reloadCollection(){
        connector.execSQLUpdate("TRUNCATE trolls;");
        connector.execSQLUpdate("TRUNCATE things;");

        /**
         * debug-method
         */
        //connector.execSQLUpdate("TRUNCATE users");

        Main.getObjectsLinkedDeque().forEach(p->addTrollToDb(p.getKey(),new User(p.getValue(),"")));
    }

    public boolean isUserExistsInDB(User user){
        Pair<PreparedStatement, ResultSet> resultPair = connector.execSQLQuery("SELECT users.login FROM users;");
        ResultSet set = resultPair.getValue();
        try {
            while (set.next()) {
                if (set.getString(DBConst.USERS_LOGIN).equals(user.getLogin())) {
                    return true;
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean isUserCorrectInDB(User user){
        Pair<PreparedStatement, ResultSet> resultPair = connector.execSQLQuery("SELECT users.login, users.password FROM users;");
        ResultSet set = resultPair.getValue();
        try {
            while (set.next()) {
                if (set.getString(DBConst.USERS_LOGIN).equals(user.getLogin()) && set.getString("password").equals(user.getHashedPassword())) {
                    return true;
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public void addTrollsCollectionToDB(Collection<Troll> trolls, User user){
        trolls.forEach(p -> addTrollToDb(p,user));
    }

    public boolean isEmailExistsInDb(User user){
        Pair<PreparedStatement, ResultSet> resultPair = connector.execSQLQuery("SELECT "+DBConst.USERS_EMAIL+" FROM users;");
        ResultSet set = resultPair.getValue();
        try {
            while (set.next()) {
                if (set.getString(DBConst.USERS_EMAIL).equals(user.getEmail())) {
                    return true;
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }
}
