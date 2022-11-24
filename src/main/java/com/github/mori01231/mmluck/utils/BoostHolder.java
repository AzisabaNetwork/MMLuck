package com.github.mori01231.mmluck.utils;

import com.github.mori01231.mmluck.MMLuck;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoostHolder {

    private ArrayList<ArrayList<Long>> boostTimes = new ArrayList<>();
    private ArrayList<ArrayList<Long>> boostTimesBuilder = new ArrayList<>();
    public final Map<UUID, Boolean> silentMode = new ConcurrentHashMap<>();

    private Long totalBoostPercentage;

    private Long lastRefreshTime;

    public Connection connection;
    private String host, database, tableName, playersTableName, username, password;
    private int port;

    public BoostHolder() {
        try {
            openConnection();
            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + playersTableName + "'");
                if (!result.next()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + playersTableName + "` (`id` VARCHAR(36) NOT NULL, `silent` BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY (`id`))");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSilentMode(UUID uuid) {
        if (silentMode.containsKey(uuid)) {
            return silentMode.get(uuid);
        }
        try {
            openConnection();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT `silent` FROM `" + playersTableName + "` WHERE `id` = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet result = stmt.executeQuery();
                boolean flag;
                if (result.next()) {
                    flag = result.getBoolean("silent");
                } else {
                    flag = false;
                }
                silentMode.put(uuid, flag);
                return flag;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setSilentMode(UUID uuid, boolean silent) {
        try {
            silentMode.put(uuid, silent);
            openConnection();
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO `" + playersTableName + "` (`id`, `silent`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `silent` = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setBoolean(2, silent);
                stmt.setBoolean(3, silent);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Long refreshAndGetPercentage(){
        if(lastRefreshTime == null){
            lastRefreshTime = 0L;
        }

        // check if enough time has elapsed since last refresh
        if(lastRefreshTime > System.currentTimeMillis() - 1000){
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
        long boostPercentage = 0L;
        for (ArrayList<Long> boostTime : boostTimes) {
            boostPercentage += boostTime.get(2);
        }
        // update the boost percentage
        totalBoostPercentage = boostPercentage;
    }

    // MySQL methods
    public void addBoost(Long startTime, Long duration, Long percentage){
        ArrayList<Long> boost = new ArrayList<>();
        boost.add(startTime);
        boost.add(duration);
        boost.add(percentage);
        boostTimes.add(boost);

        // add boost to database
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                //This is where you should do your database interaction

                try {
                    openConnection();
                    Statement statement = connection.createStatement();

                    ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "';");
                    if (!result.next()) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (`StartTime` BIGINT UNSIGNED, `Duration` BIGINT UNSIGNED, `Percentage` BIGINT UNSIGNED)");
                    }

                    statement.executeUpdate("INSERT INTO " + tableName + " (StartTime, Duration, Percentage) VALUES ('" + startTime + "', '" + duration + "', '" + percentage + "');");

                } catch(ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(MMLuck.getInstance());
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

                    ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "';");
                    if (!result.next()) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (`StartTime` BIGINT UNSIGNED, `Duration` BIGINT UNSIGNED, `Percentage` BIGINT UNSIGNED)");
                        return;
                    }

                    ResultSet dataList = statement.executeQuery("SELECT * FROM " + database + "." + tableName + ";");
                    while(dataList.next()){
                        long startTime = dataList.getLong(1);
                        long duration = dataList.getLong(2);
                        long percentage = dataList.getLong(3);
                        if((System.currentTimeMillis() - startTime) > duration * 1000){
                            statement.executeUpdate("DELETE FROM " + database + "." + tableName + " WHERE StartTime = " + startTime + ";");
                            Bukkit.getScheduler().runTask(
                                    MMLuck.getInstance(),
                                    () ->
                                            Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.WHITE +
                                                    "+" + percentage + "%" + ChatColor.LIGHT_PURPLE + "のブーストが期限切れになりました。"));
                        }
                    }

                } catch(ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(MMLuck.getInstance());
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
                    ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "';");
                    if (!result.next()) {
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (`StartTime` BIGINT UNSIGNED, `Duration` BIGINT UNSIGNED, `Percentage` BIGINT UNSIGNED)");
                        return;
                    }
                    // clear boost times builder
                    boostTimesBuilder.clear();

                    // populate boost times builder with updated data
                    ResultSet dataList = statement.executeQuery("SELECT * FROM " + database + "." + tableName + ";");
                    while(dataList.next()){
                        // get info
                        long startTime = dataList.getLong(1);
                        long duration = dataList.getLong(2);
                        long percentage = dataList.getLong(3);

                        // create new boost
                        ArrayList<Long> newBoost = new ArrayList<>();
                        newBoost.add(startTime);
                        newBoost.add(duration);
                        newBoost.add(percentage);

                        // add to boost times builder
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
    }


    public void openConnection() throws SQLException, ClassNotFoundException {

        host = MMLuck.getInstance().getConfig().getString("host");
        port = MMLuck.getInstance().getConfig().getInt("port");
        database = MMLuck.getInstance().getConfig().getString("database");
        tableName = MMLuck.getInstance().getConfig().getString("table");
        playersTableName = MMLuck.getInstance().getConfig().getString("players-table", "mmluck_players");
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
