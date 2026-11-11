package com.github.mori01231.mmluck.i18n

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.StyleBuilderApplicable
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.Reader
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.ceil

/**
 * Per-language messages loaded from flat JSON files (`lang/<locale>.json`).
 *
 * Values are MiniMessage strings. Bundled languages are written to the data folder on first start,
 * and any JSON file placed in the `lang` folder there overrides (or adds) keys for that language.
 */
object Messages {
    private const val FALLBACK = "ja_jp"
    private val BUNDLED = listOf("ja_jp", "en_us")
    private val gson = Gson()
    private val mapType: Type = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
    private val miniMessage = MiniMessage.miniMessage()

    @Volatile
    private var languages: Map<String, Map<String, String>> = emptyMap()

    @Volatile
    private var defaultLanguage = FALLBACK

    fun load(plugin: JavaPlugin) {
        val loaded = HashMap<String, Map<String, String>>()
        for (lang in BUNDLED) {
            val path = "lang/$lang.json"
            plugin.getResource(path)?.reader(Charsets.UTF_8)?.use { loaded[lang] = read(it) }
            if (!File(plugin.dataFolder, path).exists()) {
                plugin.saveResource(path, false)
            }
        }
        File(plugin.dataFolder, "lang").listFiles()?.filter { it.isFile && it.extension == "json" }?.forEach { file ->
            val lang = file.nameWithoutExtension.lowercase(Locale.ROOT)
            try {
                loaded[lang] = loaded[lang].orEmpty() + file.reader(Charsets.UTF_8).use { read(it) }
            } catch (e: Exception) {
                plugin.slF4JLogger.warn("Failed to load language file {}", file, e)
            }
        }
        languages = loaded
        defaultLanguage = plugin.config.getString("language")?.lowercase(Locale.ROOT)?.takeIf { it in loaded } ?: FALLBACK
    }

    private fun read(reader: Reader): Map<String, String> = gson.fromJson<Map<String, String>>(reader, mapType) ?: emptyMap()

    fun language(sender: CommandSender?): String = if (sender is Player) language(sender.locale()) else defaultLanguage

    fun language(locale: Locale): String {
        val tag = locale.toString().lowercase(Locale.ROOT)
        if (tag in languages) return tag
        val prefix = locale.language.lowercase(Locale.ROOT) + "_"
        return languages.keys.firstOrNull { it.startsWith(prefix) } ?: defaultLanguage
    }

    fun raw(
        language: String,
        key: String,
    ): String =
        languages[language]?.get(key)
            ?: languages[defaultLanguage]?.get(key)
            ?: languages[FALLBACK]?.get(key)
            ?: key

    fun get(
        sender: CommandSender?,
        key: String,
        vararg resolvers: TagResolver,
    ): Component = miniMessage.deserialize(raw(language(sender), key), *resolvers)

    /** Splits a multi-line message (lines separated by `\n`) into one component per line. */
    fun lines(
        sender: CommandSender?,
        key: String,
        vararg resolvers: TagResolver,
    ): List<Component> = raw(language(sender), key).split('\n').map { miniMessage.deserialize(it, *resolvers) }

    fun send(
        sender: CommandSender,
        key: String,
        vararg resolvers: TagResolver,
    ) = sender.sendMessage(get(sender, key, *resolvers))

    fun actionBar(
        player: Player,
        key: String,
        vararg resolvers: TagResolver,
    ) = player.sendActionBar(get(player, key, *resolvers))

    /** Sends the message to every online player (each in their own language) and the console. */
    fun broadcast(
        key: String,
        vararg resolvers: TagResolver,
    ) {
        Bukkit.getOnlinePlayers().forEach { send(it, key, *resolvers) }
        send(Bukkit.getConsoleSender(), key, *resolvers)
    }

    fun text(
        name: String,
        value: Any,
    ): TagResolver = Placeholder.unparsed(name, value.toString())

    fun component(
        name: String,
        value: ComponentLike,
    ): TagResolver = Placeholder.component(name, value)

    fun style(
        name: String,
        vararg style: StyleBuilderApplicable,
    ): TagResolver = Placeholder.styling(name, *style)

    /** Formats a large number using the language's digit grouping (e.g. 万/億 for Japanese, K/M for English). */
    fun friendlyNumber(
        sender: CommandSender?,
        number: BigInteger,
    ): String {
        val lang = language(sender)
        val digits = raw(lang, "number.group_digits").toIntOrNull()?.takeIf { it > 0 } ?: 4
        val suffixes = raw(lang, "number.suffixes").split(',')
        val groups = ceil(number.toString(10).length / digits.toDouble()).toInt().coerceAtLeast(1)
        val index = (groups - 1).coerceAtMost(suffixes.size - 1)
        val value = BigDecimal(number).divide(BigDecimal.TEN.pow(digits * index), 2, RoundingMode.HALF_EVEN)
        return DecimalFormat("0.00").format(value) + suffixes[index]
    }
}
