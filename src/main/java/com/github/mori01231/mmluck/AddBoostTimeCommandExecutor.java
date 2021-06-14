package com.github.mori01231.mmluck;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;

import static org.bukkit.Bukkit.getLogger;

public class AddBoostTimeCommandExecutor implements CommandExecutor {

    public Connection connection;
    private String host, database, username, password;
    private int port;

    private String TableName;
    private Boolean isConsole;
    Player player;


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if ((sender instanceof Player)) {
            player = (Player) sender;
            isConsole = false;
        } else{
            isConsole = true;
        }

        // send plugin message to other servers to save

        // declare variables
        String currentTime = Long.toString(System.currentTimeMillis());
        String duration;
        String percentage;

        // get values of variables from the command arguments
        try{
            duration = args[0];
            percentage = args[1];
        }catch(Exception e){
            sender.sendMessage("Invalid arguments");
            return true;
        }

        // add boost information to ListStore
        MMLuck.getInstance().BoostTimes.Add( currentTime + ":" + duration + ":" + percentage);
        MMLuck.getInstance().BoostTimes.Save();

        // loop through boost information and remove expired boosts
        MMLuck.getInstance().RemoveExpiredBoost();


        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                //This is where you should do your database interaction

                try {
                    openConnection();
                    Statement statement = connection.createStatement();

                    ResultSet result = statement.executeQuery("SHOW TABLES LIKE '" + TableName + "';");
                    if (result.next() == false) {
                        FeedBack("&c" + TableName + "MMLuck用のテーブルを作成中。");
                        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + TableName + "` (`StartTime` DOUBLE, `Duration` DOUBLE, `Percentage` INT)");
                        FeedBack("&e新たに" + TableName + "MMLuck用のテーブルを作成しました。");
                    }

                    statement.executeUpdate("INSERT INTO " + TableName + " (StarTime, Duration, Percentage) VALUES ('" + currentTime + "', '" + duration + "', '" + percentage + "');");

                } catch(ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        };

        r.runTaskAsynchronously(MMLuck.getInstance());

        return true;
    }

    public void openConnection() throws SQLException, ClassNotFoundException {

        host = MMLuck.getInstance().getConfig().getString("host");
        port = MMLuck.getInstance().getConfig().getInt("port");
        database = MMLuck.getInstance().getConfig().getString("database");
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

    public void FeedBack(String message){
        if(isConsole){
            getLogger().info(ChatColor.translateAlternateColorCodes('&',message));
        }else{
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
        }
    }
}
