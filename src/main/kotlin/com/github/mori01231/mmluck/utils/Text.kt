package com.github.mori01231.mmluck.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Conversion helpers between legacy (§ / &) formatted strings and Adventure components.
 */
object Text {
    private val legacySection =
        LegacyComponentSerializer
            .builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build()
    private const val FORMAT_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx"

    /** Equivalent of the old `ChatColor.translateAlternateColorCodes('&', text)`. */
    fun translateAmpersand(text: String): String {
        val chars = text.toCharArray()
        for (i in 0 until chars.size - 1) {
            if (chars[i] == '&' && FORMAT_CODES.indexOf(chars[i + 1]) >= 0) {
                chars[i] = LegacyComponentSerializer.SECTION_CHAR
                chars[i + 1] = chars[i + 1].lowercaseChar()
            }
        }
        return String(chars)
    }

    /** Parses a string that may contain both `&` and `§` formatting codes. */
    fun fromAmpersand(text: String): Component = fromLegacy(translateAmpersand(text))

    /** Parses a string that contains `§` formatting codes. */
    fun fromLegacy(text: String): Component = legacySection.deserialize(text)

    fun toLegacy(component: Component): String = legacySection.serialize(component)

    /** Item names and lore are italic by default; disable it unless explicitly set. */
    fun item(component: Component): Component = component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
}
