package com.github.mori01231.mmluck.utils;

import com.github.mori01231.mmluck.MMLuck;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.ArrayList;

public class BoostHolder {

    private ArrayList<ArrayList<Long>> boostTimes = new ArrayList<>();
    private ArrayList<ArrayList<Long>> boostTimesBuilder = new ArrayList<>();

    private Long totalBoostPercentage;

    private Long lastRefreshTime;

    public Connection connection;
    private String host, database, TableName, username, password;
    private int port;


    public ArrayList<Long> createNewBoost(Long startTime, Long duration, Long percentage){
        ArrayList<Long> boost = new ArrayList<>();
        boost.add(startTime);
        boost.add(duration);
        boost.add(percentage);

        return boost;
    }


    public Long refreshAndGetPercentage(){
        // check if enough time has elapsed since last refresh
        if(lastRefreshTime > System.currentTimeMillis() - 5000){
            return totalBoostPercentage;
        }
        lastRefreshTime = System.currentTimeMillis();

        // remove old entries from database
        removeOld();

        // get newest data from database and replace boostTimes with it
        updateFromDatabase();

        // update the boost percentage
        updateBoostPercentage();

        return totalBoostPercentage;
    }

    private void updateBoostPercentage(){
        // calculate the boost percentage
        Long boostPercentage = 0l;
        for(int i=0; i<boostTimes.size();i++){
            boostPercentage += boostTimes.get(i).get(0);
        }
        // update the boost percentage
        totalBoostPercentage = boostPercentage;
    }

    // MySQL methods
    public void addBoost(ArrayList<Long> boost){
        boostTimes.add(boost);

        // declare variables
        Long currentTime = boost.get(0);
        Long duration = boost.get(1);
        Long percentage = boost.get(2);
        // add boost to database
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                //This is where you should do your database interaction

                try {
                    openConnection();
                    Statement statement = connection.createStatement();

                    ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + TableName + "';");
                    if (result.next() == false) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + TableName + "` (`StartTime` UNSIGNED BIGINT, `Duration` UNSIGNED BIGINT, `Percentage` UNSIGNED BIGINT)");
                    }

                    statement.executeUpdate("INSERT INTO " + TableName + " (StartTime, Duration, Percentage) VALUES ('" + currentTime + "', '" + duration + "', '" + percentage + "');");

                } catch(ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(MMLuck.getInstance());
        return;
    }

    // go through database and delete any boosts where duration + startTime > currentTime
    private void removeOld(){

        // database interaction
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                //This is where you should do your database interaction

                try {
                    openConnection();
                    Statement statement = connection.createStatement();

                    ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + TableName + "';");
                    if (result.next() == false) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + TableName + "` (`StartTime` UNSIGNED BIGINT, `Duration` UNSIGNED BIGINT, `Percentage` UNSIGNED BIGINT)");
                        return;
                    }

                    ResultSet dataList = statement.executeQuery("SELECT * FROM " + database + "." + TableName + ";");
                    while(dataList.next()){
                        Long startTime = dataList.getLong(1);
                        Long duration = dataList.getLong(2);
                        if((System.currentTimeMillis() - startTime) > duration){
                            ResultSet deletedRow = statement.executeQuery("DELETE FROM " + database + "." + TableName + " WHERE StartTime = " + startTime + ";");
                        }
                    }

                } catch(ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(MMLuck.getInstance());
        return;
    }

    // get info from database and update local arraylist
    private void updateFromDatabase(){

        // database interaction
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                // async database interaction
                try {
                    openConnection();
                    Statement statement = connection.createStatement();
                    // make sure table exists
                    ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + TableName + "';");
                    if (result.next() == false) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + TableName + "` (`StartTime` UNSIGNED BIGINT, `Duration` UNSIGNED BIGINT, `Percentage` UNSIGNED BIGINT)");
                        return;
                    }
                    // clear boost times builder
                    boostTimesBuilder.clear();

                    // populate boost times builder with updated data
                    ResultSet dataList = statement.executeQuery("SELECT * FROM " + database + "." + TableName + ";");
                    while(dataList.next()){
                        // get info
                        Long startTime = dataList.getLong(1);
                        Long duration = dataList.getLong(2);
                        Long percentage = dataList.getLong(3);
                        // create new boost
                        ArrayList newBoost = createNewBoost(startTime,duration,percentage);

                        boostTimesBuilder.add(newBoost);
                    }

                    // update boost data
                    boostTimes = (ArrayList<ArrayList<Long>>) boostTimesBuilder.clone();

                } catch(ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(MMLuck.getInstance());
        return;
    }


    public void openConnection() throws SQLException, ClassNotFoundException {

        host = MMLuck.getInstance().getConfig().getString("host");
        port = MMLuck.getInstance().getConfig().getInt("port");
        database = MMLuck.getInstance().getConfig().getString("database");
        TableName = MMLuck.getInstance().getConfig().getString("table");
        username = MMLuck.getInstance().getConfig().getString("username");
        password = MMLuck.getInstance().getConfig().getString("password");

        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database, this.username, this.password);
        }
    }
}
