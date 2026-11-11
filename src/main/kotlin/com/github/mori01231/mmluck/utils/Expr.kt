package com.github.mori01231.mmluck.utils

import com.github.mori01231.mmluck.MMLuck
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.variables.VariableRegistry
import org.bukkit.BanEntry
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import xyz.acrylicstyle.util.expression.CompileData
import xyz.acrylicstyle.util.expression.ExpressionParser
import xyz.acrylicstyle.util.expression.RuntimeData
import xyz.acrylicstyle.util.expression.instruction.DummyInstTypeInfo
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.pow

/**
 * Script evaluator for `${...}` expressions in item names and lore.
 *
 * Instance methods are exposed to scripts through the `expr` variable.
 */
class Expr private constructor() {
    fun round(d: Double): Long = Math.round(d)

    fun round1(d: Double): Double = round(d, 1)

    fun round2(d: Double): Double = round(d, 2)

    fun round3(d: Double): Double = round(d, 3)

    fun round4(d: Double): Double = round(d, 4)

    fun round5(d: Double): Double = round(d, 5)

    fun round(
        d: Double,
        points: Int,
    ): Double {
        val factor = 10.0.pow(points)
        return Math.round(d * factor) / factor
    }

    fun randomDouble(
        min: Double,
        max: Double,
    ): Double = Math.random() * (max - min) + min

    fun secureRandomDouble(
        min: Double,
        max: Double,
    ): Double = SECURE_RANDOM.nextDouble() * (max - min) + min

    fun randomDouble(
        min: Int,
        max: Int,
    ): Double = Math.random() * (max - min) + min

    fun secureRandomDouble(
        min: Int,
        max: Int,
    ): Double = SECURE_RANDOM.nextDouble() * (max - min) + min

    fun randomLong(
        min: Long,
        max: Long,
    ): Long = Math.round(randomDouble(min.toDouble(), max.toDouble()))

    fun secureRandomLong(
        min: Long,
        max: Long,
    ): Long = Math.round(secureRandomDouble(min.toDouble(), max.toDouble()))

    fun randomInt(
        min: Int,
        max: Int,
    ): Int = Math.toIntExact(Math.round(randomDouble(min, max)))

    fun secureRandomInt(
        min: Int,
        max: Int,
    ): Int = Math.toIntExact(Math.round(secureRandomDouble(min, max)))

    fun padZero(
        number: Int,
        length: Int,
    ): String = number.toString().padStart(length, '0')

    fun comma(number: Int): String = "%,d".format(number)

    companion object {
        @JvmField
        val INSTANCE = Expr()

        private val DISALLOWED_RETURN_TYPES: Set<Class<*>> =
            setOf(
                Void.TYPE,
                ConsoleCommandSender::class.java,
                Class::class.java,
                Field::class.java,
                Method::class.java,
                Constructor::class.java,
                BanEntry::class.java,
            )
        private val SECURE_RANDOM = SecureRandom()
        private val SCRIPT_PATTERN = Regex("""\$\{(.+)}""")
        private val VARIABLES = listOf("player", "expr", "globalVariable", "casterVariable")

        @JvmStatic
        fun longToHex(l: Long): String = l.toString(16)

        /**
         * Replaces every `${...}` in [line] with its evaluated value.
         *
         * @return the replaced line, or null if the line does not contain any script
         */
        fun evalAndReplace(
            player: Player,
            line: String,
            description: String,
        ): String? {
            if (!SCRIPT_PATTERN.containsMatchIn(line)) return null
            return SCRIPT_PATTERN.replace(line) { match ->
                try {
                    eval(player, match.groupValues[1]).toString()
                } catch (e: Exception) {
                    MMLuck.instance.slF4JLogger.error("Error evaluating script (description: {})\nLine: {}", description, line, e)
                    "<ERROR>"
                }
            }
        }

        private fun compileData(playerType: Class<*>): CompileData =
            CompileData
                .builder()
                .allowPrivate(true)
                .addVariable("player", playerType)
                .addVariable("expr", Expr::class.java)
                .addVariable("globalVariable", VariableRegistry::class.java)
                .addVariable("casterVariable", VariableRegistry::class.java)
                .build()

        fun getSuggestions(
            sender: Any,
            src: String,
        ): List<String> = getSuggestionsPartial(VARIABLES, compileData(sender.javaClass), src)

        fun getSuggestionsFull(
            variables: List<String>,
            compileData: CompileData,
            input: String,
        ): List<String> {
            if (!input.contains('.')) return variables
            val prefix = input.substring(0, input.lastIndexOf('.'))
            val instruction = ExpressionParser.compile(prefix, compileData).lastOrNull()
            if (instruction is DummyInstTypeInfo) {
                return getTokens(instruction.clazz).map { "$prefix.$it" }
            }
            return emptyList()
        }

        fun getSuggestionsPartial(
            variables: List<String>,
            compileData: CompileData,
            input: String,
        ): List<String> {
            if (!input.contains('.')) return variables.filter { it.startsWith(input) }
            val token = input.substring(input.lastIndexOf('.') + 1).lowercase(Locale.ROOT)
            val last = input.split(" ").last()
            val instruction = ExpressionParser.compile(input.substring(0, input.lastIndexOf('.')), compileData).lastOrNull()
            if (instruction is DummyInstTypeInfo) {
                return getTokens(instruction.clazz)
                    .filter { it.lowercase(Locale.ROOT).startsWith(token) }
                    .map { last.substring(0, last.lastIndexOf('.') + 1) + it }
            }
            return emptyList()
        }

        private fun getTokens(type: Class<*>): List<String> {
            val tokens = ArrayList<String>()
            for (clazz in getSupers(type)) {
                clazz.declaredFields.mapTo(tokens) { it.name }
                for (method in clazz.declaredMethods) {
                    if (method.returnType in DISALLOWED_RETURN_TYPES) continue
                    val name = method.name
                    if (name.length >= 4 && name.startsWith("get") && name[3].isUpperCase() && method.parameterCount == 0) {
                        tokens.add(name.substring(3, 4).lowercase(Locale.ROOT) + name.substring(4))
                    }
                    if (method.parameterCount == 0) {
                        tokens.add("$name()")
                        tokens.add(name)
                    } else {
                        tokens.add("$name(")
                    }
                }
            }
            tokens.add("?as(")
            return tokens.distinct()
        }

        private fun getSupers(type: Class<*>): Set<Class<*>> {
            val set = LinkedHashSet<Class<*>>()
            set.add(type)
            type.superclass?.let {
                set.add(it)
                set.addAll(getSupers(it))
            }
            set.addAll(type.interfaces)
            for (anInterface in type.interfaces) {
                set.addAll(getSupers(anInterface))
            }
            return set
        }

        fun eval(
            player: Player,
            src: String,
        ): Any? {
            val instructionSet = ExpressionParser.compile(src, compileData(player.javaClass))
            instructionSet.forEach { instruction ->
                if (instruction is DummyInstTypeInfo && instruction.clazz in DISALLOWED_RETURN_TYPES) {
                    throw IllegalArgumentException("Disallowed return type: ${instruction.clazz}")
                }
            }
            val mythic = MythicBukkit.inst()
            return instructionSet.execute(
                RuntimeData
                    .builder()
                    .allowPrivate(true)
                    .addVariable("player", player)
                    .addVariable("expr", INSTANCE)
                    .addVariable("globalVariable", mythic.variableManager.globalRegistry)
                    .addVariable("casterVariable", mythic.playerManager.getProfile(player).variables)
                    .build(),
            )
        }
    }
}
