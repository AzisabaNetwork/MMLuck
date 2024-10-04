package com.github.mori01231.mmluck.utils;

import com.github.mori01231.mmluck.BoostData;
import com.github.mori01231.mmluck.MMLuck;
import net.azisaba.rarity.api.RarityAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.intellij.lang.annotations.Subst;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class BoostHolder {

    private ArrayList<ArrayList<Long>> boostTimes = new ArrayList<>();
    public final Map<UUID, Boolean> silentMode = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> alwaysStash = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> minimumStashRarity = new ConcurrentHashMap<>();

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
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + playersTableName + "` (`id` VARCHAR(36) NOT NULL, `silent` BOOLEAN NOT NULL DEFAULT FALSE, `always_stash` BOOLEAN NOT NULL DEFAULT FALSE, `minimum_stash_rarity` INT NOT NULL DEFAULT -1, PRIMARY KEY (`id`))");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<BoostData> getBoostData() {
        List<BoostData> list = new ArrayList<>();
        try {
            openConnection();
            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "';");
            if (!result.next()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (`StartTime` BIGINT UNSIGNED, `Duration` BIGINT UNSIGNED, `Percentage` BIGINT UNSIGNED)");
                return list;
            }

            ResultSet dataList = statement.executeQuery("SELECT * FROM " + database + "." + tableName + ";");
            while (dataList.next()) {
                long startTime = dataList.getLong(1);
                long duration = dataList.getLong(2);
                long percentage = dataList.getLong(3);
                list.add(new BoostData(startTime, duration, percentage));
            }

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return list;
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

    // ALTER TABLE mmluck_players ADD `always_stash` BOOLEAN NOT NULL DEFAULT FALSE;
    public boolean isAlwaysStash(UUID uuid) {
        if (alwaysStash.containsKey(uuid)) {
            return alwaysStash.get(uuid);
        }
        try {
            openConnection();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT `always_stash` FROM `" + playersTableName + "` WHERE `id` = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet result = stmt.executeQuery();
                boolean flag;
                if (result.next()) {
                    flag = result.getBoolean("always_stash");
                } else {
                    flag = false;
                }
                alwaysStash.put(uuid, flag);
                return flag;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setAlwaysStash(UUID uuid, boolean b) {
        try {
            alwaysStash.put(uuid, b);
            openConnection();
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO `" + playersTableName + "` (`id`, `always_stash`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `always_stash` = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setBoolean(2, b);
                stmt.setBoolean(3, b);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ALTER TABLE mmluck_players ADD `minimum_stash_rarity` INT NOT NULL DEFAULT -1;
    public int getMinimumStashRarity(UUID uuid) {
        if (minimumStashRarity.containsKey(uuid)) {
            return minimumStashRarity.get(uuid);
        }
        try {
            openConnection();
            try (PreparedStatement stmt = connection.prepareStatement("SELECT `minimum_stash_rarity` FROM `" + playersTableName + "` WHERE `id` = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet result = stmt.executeQuery();
                int num = -1;
                if (result.next()) {
                    num = result.getInt("minimum_stash_rarity");
                }
                if (num == -1) {
                    @Subst("rare")
                    String rarity = Objects.requireNonNull(MMLuck.getInstance().getConfig().getString("minimumStashRarityDefault", "rare"));
                    num = RarityAPIProvider.get().getRarityById(rarity).getWeight();
                }
                minimumStashRarity.put(uuid, num);
                return num;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setMinimumStashRarity(UUID uuid, int i) {
        try {
            minimumStashRarity.put(uuid, i);
            openConnection();
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO `" + playersTableName + "` (`id`, `minimum_stash_rarity`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `minimum_stash_rarity` = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, i);
                stmt.setInt(3, i);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Long> refreshAndGetPercentage(boolean force, boolean immediate) {
        if(lastRefreshTime == null){
            lastRefreshTime = 0L;
        }

        // check if enough time has elapsed since last refresh
        if(!force && lastRefreshTime > System.currentTimeMillis() - 1000){
            return CompletableFuture.completedFuture(totalBoostPercentage);
        }
        lastRefreshTime = System.currentTimeMillis();

        // remove old entries from database
        removeOld();

        // get the newest data from database and replace boostTimes with it
        // also update boost percentage value
        if (immediate) {
            updateFromDatabase();
            updateBoostPercentage();
            return CompletableFuture.completedFuture(totalBoostPercentage);
        }
        return updateFromDatabase().thenRunAsync(this::updateBoostPercentage).thenApplyAsync(v -> totalBoostPercentage);
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
    public CompletableFuture<Void> addBoost(Long startTime, Long duration, Long percentage) {
        ArrayList<Long> boost = new ArrayList<>();
        boost.add(startTime);
        boost.add(duration);
        boost.add(percentage);
        boostTimes.add(boost);

        // add boost to database
        return CompletableFuture.runAsync(() -> {
            //This is where you should do your database interaction

            try {
                openConnection();
                Statement statement = connection.createStatement();

                ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "';");
                if (!result.next()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (`StartTime` BIGINT UNSIGNED, `Duration` BIGINT UNSIGNED, `Percentage` BIGINT UNSIGNED)");
                }

                statement.executeUpdate("INSERT INTO " + tableName + " (StartTime, Duration, Percentage) VALUES ('" + startTime + "', '" + duration + "', '" + percentage + "');");

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        });
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
                            String sp = Long.toString(percentage);
                            if (percentage >= 0) {
                                sp = "+" + sp;
                            }
                            String finalSp = sp;
                            Bukkit.getScheduler().runTask(
                                    MMLuck.getInstance(),
                                    () ->
                                            Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.WHITE +
                                                    finalSp + "%" + ChatColor.LIGHT_PURPLE + "のブーストが期限切れになりました。"));
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
    private CompletableFuture<Void> updateFromDatabase() {
        // database interaction
        return CompletableFuture.runAsync(() -> {
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
                ArrayList<ArrayList<Long>> boostTimesBuilder = new ArrayList<>();

                // populate boost times builder with updated data
                ResultSet dataList = statement.executeQuery("SELECT * FROM " + database + "." + tableName + ";");
                while (dataList.next()) {
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
                boostTimes = boostTimesBuilder;

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        });
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
