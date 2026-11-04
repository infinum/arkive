package com.infinum.arkive.composeutils.uitree

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Hand-written JSON for [UiTree]: composeUtils sits on the consumer's runtime classpath, so it
 * takes no serialization dependency. Pretty-printed (2-space indent, `"key": value`) so agents
 * can grep it — the skills grep for `"overflow": true`. Null fields and empty lists are omitted.
 */
internal object UiTreeJson {
    private const val INDENT = "  "
    private const val BOUNDS_DECIMALS = 2
    private const val DENSITY_DECIMALS = 4
    private const val FIRST_PRINTABLE_CHAR = 0x20

    fun write(tree: UiTree): String {
        val out = StringBuilder()
        writeValue(out, tree.toJsonMap(), depth = 0)
        return out.append('\n').toString()
    }

    fun formatNumber(value: Float, decimals: Int = BOUNDS_DECIMALS): String =
        BigDecimal(value.toDouble())
            .setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    private fun UiTree.toJsonMap(): Map<String, Any?> = linkedMapOf(
        "schemaVersion" to UiTree.SCHEMA_VERSION,
        "id" to id,
        "density" to RawNumber(formatNumber(density, DENSITY_DECIMALS)),
        "size" to listOf(width, height),
        "root" to root.toJsonMap(),
    )

    private fun UiTreeNode.toJsonMap(): Map<String, Any?> = linkedMapOf(
        "composable" to composable,
        "callers" to callers.ifEmpty { null },
        "source" to source,
        "view" to view,
        "id" to viewId,
        "bounds" to listOf(bounds.x, bounds.y, bounds.width, bounds.height),
        "role" to role,
        "text" to text,
        "editableText" to editableText,
        "contentDescription" to contentDescription,
        "testTag" to testTag,
        "actions" to actions.ifEmpty { null },
        "state" to state?.toJsonMap(),
        "textStyle" to textStyle?.toJsonMap(),
        "children" to children.ifEmpty { null }?.map { it.toJsonMap() },
    )

    private fun UiState.toJsonMap(): Map<String, Any?> = linkedMapOf(
        "enabled" to enabled,
        "selected" to selected,
        "toggle" to toggle,
        "focused" to focused,
    )

    private fun UiTextStyle.toJsonMap(): Map<String, Any?> = linkedMapOf(
        "fontSize" to fontSize,
        "fontWeight" to fontWeight,
        "lineHeight" to lineHeight,
        "color" to color,
        "maxLines" to maxLines,
        "overflow" to overflow,
    )

    private fun writeValue(out: StringBuilder, value: Any?, depth: Int) {
        when (value) {
            is Map<*, *> -> writeObject(out, value, depth)
            is List<*> -> writeArray(out, value, depth)
            is String -> writeString(out, value)
            is Float -> out.append(formatNumber(value))
            is RawNumber -> out.append(value.text)
            is Int, is Boolean -> out.append(value.toString())
            else -> error("Arkive: unsupported ui tree JSON value $value")
        }
    }

    private fun writeObject(out: StringBuilder, map: Map<*, *>, depth: Int) {
        val entries = map.entries.filter { it.value != null }
        out.append("{\n")
        entries.forEachIndexed { index, entry ->
            out.append(INDENT.repeat(depth + 1))
            writeString(out, entry.key.toString())
            out.append(": ")
            writeValue(out, entry.value, depth + 1)
            out.append(if (index < entries.lastIndex) ",\n" else "\n")
        }
        out.append(INDENT.repeat(depth)).append('}')
    }

    private fun writeArray(out: StringBuilder, list: List<*>, depth: Int) {
        if (list.none { it is Map<*, *> || it is List<*> }) {
            out.append('[')
            list.forEachIndexed { index, item ->
                if (index > 0) out.append(", ")
                writeValue(out, item, depth)
            }
            out.append(']')
        } else {
            out.append("[\n")
            list.forEachIndexed { index, item ->
                out.append(INDENT.repeat(depth + 1))
                writeValue(out, item, depth + 1)
                out.append(if (index < list.lastIndex) ",\n" else "\n")
            }
            out.append(INDENT.repeat(depth)).append(']')
        }
    }

    private fun writeString(out: StringBuilder, value: String) {
        out.append('"')
        value.forEach { char ->
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char.code < FIRST_PRINTABLE_CHAR -> out.append("\\u%04x".format(char.code))
                else -> out.append(char)
            }
        }
        out.append('"')
    }

    /** A number already formatted with a non-default precision (density keeps 4 decimals). */
    private data class RawNumber(val text: String)
}
