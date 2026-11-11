package com.github.mori01231.mmluck.utils

import com.github.mori01231.mmluck.BoostData
import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.i18n.Messages
import net.azisaba.rarity.api.RarityAPIProvider
import org.bukkit.Bukkit
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class BoostHolder {
    @Volatile
    private var boostTimes: List<BoostData> = emptyList()
    val silentMode: MutableMap<UUID, Boolean> = ConcurrentHashMap()
    val verboseMode: MutableMap<UUID, Boolean> = ConcurrentHashMap()
    val alwaysStash: MutableMap<UUID, Boolean> = ConcurrentHashMap()
    val minimumStashRarity: MutableMap<UUID, Int> = ConcurrentHashMap()

    @Volatile
    private var totalBoostPercentage = 0L

    @Volatile
    private var lastRefreshTime = 0L

    @Volatile
    private var connection: Connection? = null
    private var tableName = "MMLuck"
    private var playersTableName = "mmluck_players"

    init {
        val conn = openConnection()
        conn.createStatement().use { statement ->
            val exists = statement.executeQuery("SHOW TABLES LIKE '$playersTableName'").use { it.next() }
            if (!exists) {
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `$playersTableName` (`id` VARCHAR(36) NOT NULL, `silent` BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "`verbose` BOOLEAN NOT NULL DEFAULT FALSE, `always_stash` BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "`minimum_stash_rarity` INT NOT NULL DEFAULT -1, PRIMARY KEY (`id`))",
                )
            } else {
                // migrate existing table
                val hasVerbose = statement.executeQuery("SHOW COLUMNS FROM `$playersTableName` LIKE 'verbose'").use { it.next() }
                if (!hasVerbose) {
                    statement.executeUpdate("ALTER TABLE `$playersTableName` ADD COLUMN `verbose` BOOLEAN NOT NULL DEFAULT FALSE AFTER `silent`")
                }
            }
        }
    }

    fun getBoostData(): List<BoostData> = readBoosts()

    fun isSilentMode(uuid: UUID): Boolean = silentMode.getOrPut(uuid) { selectBoolean(uuid, "silent") }

    fun setSilentMode(
        uuid: UUID,
        silent: Boolean,
    ) {
        silentMode[uuid] = silent
        upsert(uuid, "silent") { stmt, index -> stmt.setBoolean(index, silent) }
    }

    fun isVerboseMode(uuid: UUID): Boolean = verboseMode.getOrPut(uuid) { selectBoolean(uuid, "verbose") }

    fun setVerboseMode(
        uuid: UUID,
        verbose: Boolean,
    ) {
        verboseMode[uuid] = verbose
        upsert(uuid, "verbose") { stmt, index -> stmt.setBoolean(index, verbose) }
    }

    fun isAlwaysStash(uuid: UUID): Boolean = alwaysStash.getOrPut(uuid) { selectBoolean(uuid, "always_stash") }

    fun setAlwaysStash(
        uuid: UUID,
        value: Boolean,
    ) {
        alwaysStash[uuid] = value
        upsert(uuid, "always_stash") { stmt, index -> stmt.setBoolean(index, value) }
    }

    fun getMinimumStashRarity(uuid: UUID): Int =
        minimumStashRarity.getOrPut(uuid) {
            val stored = select(uuid, "minimum_stash_rarity") { it.getInt("minimum_stash_rarity") } ?: -1
            if (stored != -1) {
                stored
            } else {
                val rarity = MMLuck.instance.config.getString("minimumStashRarityDefault") ?: "rare"
                RarityAPIProvider.get().getRarityById(rarity).weight
            }
        }

    fun setMinimumStashRarity(
        uuid: UUID,
        weight: Int,
    ) {
        minimumStashRarity[uuid] = weight
        upsert(uuid, "minimum_stash_rarity") { stmt, index -> stmt.setInt(index, weight) }
    }

    fun refreshAndGetPercentage(
        force: Boolean,
        immediate: Boolean,
    ): CompletableFuture<Long> {
        // check if enough time has elapsed since last refresh
        if (!force && lastRefreshTime > System.currentTimeMillis() - 1000) {
            return CompletableFuture.completedFuture(totalBoostPercentage)
        }
        lastRefreshTime = System.currentTimeMillis()

        removeOld()

        if (immediate) {
            boostTimes = readBoosts()
            updateBoostPercentage()
            return CompletableFuture.completedFuture(totalBoostPercentage)
        }
        return CompletableFuture
            .runAsync { boostTimes = readBoosts() }
            .thenApplyAsync {
                updateBoostPercentage()
                totalBoostPercentage
            }
    }

    private fun updateBoostPercentage() {
        totalBoostPercentage = boostTimes.sumOf { it.percentage }
    }

    fun addBoost(
        startTime: Long,
        duration: Long,
        percentage: Long,
    ): CompletableFuture<Void> {
        boostTimes = boostTimes + BoostData(startTime, duration, percentage)
        return CompletableFuture.runAsync {
            try {
                ensureBoostTable()
                openConnection().prepareStatement("INSERT INTO `$tableName` (StartTime, Duration, Percentage) VALUES (?, ?, ?)").use { stmt ->
                    stmt.setLong(1, startTime)
                    stmt.setLong(2, duration)
                    stmt.setLong(3, percentage)
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                MMLuck.instance.slF4JLogger.error("Failed to add boost", e)
            }
        }
    }

    /** Deletes expired boosts from the database and announces them. */
    private fun removeOld() {
        Bukkit.getScheduler().runTaskAsynchronously(
            MMLuck.instance,
            Runnable {
                try {
                    val now = System.currentTimeMillis()
                    for (boost in readBoosts().filter { it.isExpired(now) }) {
                        openConnection().prepareStatement("DELETE FROM `$tableName` WHERE StartTime = ?").use { stmt ->
                            stmt.setLong(1, boost.startTime)
                            stmt.executeUpdate()
                        }
                        val percentage = if (boost.percentage >= 0) "+${boost.percentage}" else boost.percentage.toString()
                        Bukkit.getScheduler().runTask(
                            MMLuck.instance,
                            Runnable { Messages.broadcast("boost.expired", Messages.text("percentage", percentage)) },
                        )
                    }
                } catch (e: Exception) {
                    MMLuck.instance.slF4JLogger.error("Failed to remove expired boosts", e)
                }
            },
        )
    }

    private fun readBoosts(): List<BoostData> =
        try {
            ensureBoostTable()
            openConnection().createStatement().use { statement ->
                statement.executeQuery("SELECT StartTime, Duration, Percentage FROM `$tableName`").use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(BoostData(rs.getLong(1), rs.getLong(2), rs.getLong(3)))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            MMLuck.instance.slF4JLogger.error("Failed to read boosts", e)
            emptyList()
        }

    private fun ensureBoostTable() {
        openConnection().createStatement().use {
            it.executeUpdate("CREATE TABLE IF NOT EXISTS `$tableName` (`StartTime` BIGINT UNSIGNED, `Duration` BIGINT UNSIGNED, `Percentage` BIGINT UNSIGNED)")
        }
    }

    private fun selectBoolean(
        uuid: UUID,
        column: String,
    ): Boolean = select(uuid, column) { it.getBoolean(column) } ?: false

    private fun <T : Any> select(
        uuid: UUID,
        column: String,
        getter: (ResultSet) -> T,
    ): T? =
        openConnection().prepareStatement("SELECT `$column` FROM `$playersTableName` WHERE `id` = ?").use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.executeQuery().use { rs -> if (rs.next()) getter(rs) else null }
        }

    private fun upsert(
        uuid: UUID,
        column: String,
        setter: (PreparedStatement, Int) -> Unit,
    ) {
        openConnection()
            .prepareStatement("INSERT INTO `$playersTableName` (`id`, `$column`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `$column` = ?")
            .use { stmt ->
                stmt.setString(1, uuid.toString())
                setter(stmt, 2)
                setter(stmt, 3)
                stmt.executeUpdate()
            }
    }

    @Synchronized
    fun openConnection(): Connection {
        val config = MMLuck.instance.config
        tableName = config.getString("table") ?: "MMLuck"
        playersTableName = config.getString("players-table") ?: "mmluck_players"
        connection?.let { if (!it.isClosed) return it }

        val host = config.getString("host")
        val port = config.getInt("port")
        val database = config.getString("database")
        Class.forName("com.mysql.cj.jdbc.Driver")
        return DriverManager
            .getConnection("jdbc:mysql://$host:$port/$database", config.getString("username"), config.getString("password"))
            .also { connection = it }
    }

    @Synchronized
    fun close() {
        connection?.close()
        connection = null
    }
}
