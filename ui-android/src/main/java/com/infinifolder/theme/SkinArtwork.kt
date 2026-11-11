package com.infinifolder.theme

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Logical Winamp sprite names are independent of encoded image formats. */
class SkinArtwork(private val entries: Map<String, ByteArray>) {
    val layers: Map<String, List<String>> = entries["theme.json"]?.let { bytes ->
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val images = root.optJSONObject("images") ?: throw IllegalArgumentException("theme.json needs an images object")
        require(images.length() <= IMAGE_ROLES.size)
        buildMap {
            for (key in images.keys()) {
                val role = role(key)
                require(role in IMAGE_ROLES && role !in this) { "Unknown or duplicate skin image role" }
                val value = images.get(key)
                val names = when (value) {
                    is String -> listOf(normalize(value))
                    is JSONArray -> {
                        require(value.length() in 1..4) { "A skin image needs one to four layers" }
                        (0 until value.length()).map { normalize(value.getString(it)) }
                    }
                    else -> throw IllegalArgumentException("Skin image must be a filename or layer array")
                }
                require(names.isNotEmpty() && names.distinct().size == names.size && names.all { it in this@SkinArtwork.entries && it != "theme.json" }) { "Missing or duplicate skin layer" }
                put(role, names)
            }
        }
    } ?: emptyMap()

    fun filenames(logicalName: String): List<String> {
        val role = role(logicalName)
        layers[role]?.let { return it }
        if (role !in IMAGE_ROLES) return emptyList()
        // Preserve classic skins and avoid relying on ZIP entry order for alternatives.
        return entries.keys.filter { isImageEntry(it) && role(it) == role }.sortedWith(compareBy<String> { if (it == "$role.bmp") 0 else 1 }.thenBy { it }).take(1)
    }
    fun images(logicalName: String): List<Pair<String, ByteArray>> = filenames(logicalName).map { it to entries.getValue(it) }
    val referencedFiles: Set<String> get() = layers.values.flatten().toSet()

    companion object {
        val IMAGE_ROLES = setOf("main", "gen", "cbuttons", "titlebar", "posbar", "volume", "shufrep", "text", "numbers", "monoster", "playpaus", "pledit", "balance", "eqmain", "eq_ex", "nums_ex", "video", "genex", "albumlist", "amarok", "mikro", "mb")
        fun normalize(name: String): String = name.replace('\\', '/').substringAfterLast('/').lowercase(Locale.ROOT)
        fun role(name: String): String = normalize(name).substringBeforeLast('.', normalize(name))
        fun isImageEntry(name: String): Boolean = normalize(name) !in setOf("pledit.txt", "albumlist.txt", "viscolor.txt", "theme.json") && role(name) in IMAGE_ROLES
    }
}
