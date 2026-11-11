package com.infinifolder.theme

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class SkinArtworkTest {
    private fun archive(files: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip -> files.forEach { (name, data) -> zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry() } }
    }.toByteArray()
    @Test fun arbitraryImageFormatsUseTheSameLogicalSpriteRole() {
        for (name in listOf("main.iff", "main.svg", "main.png", "main.weird")) {
            val parsed = WinampSkinParser.parseSkinFromStream(archive(mapOf(name to byteArrayOf(1,2,3))).inputStream(), "test")!!
            assertEquals(listOf(name), SkinArtwork(parsed.artwork).filenames("main.bmp"))
        }
    }
    @Test fun classicBmpAndArchiveOrderRemainCompatible() {
        val artwork = mapOf("main.png" to byteArrayOf(1), "main.bmp" to byteArrayOf(2))
        assertEquals(listOf("main.bmp"), SkinArtwork(artwork).filenames("main"))
        assertEquals(listOf("main.bmp"), SkinArtwork(artwork.entries.reversed().associate { it.toPair() }).filenames("main"))
    }
    @Test fun manifestSupportsNamedLayersAndNonRoleFilenames() {
        val manifest = """{"images":{"main":["panel.dat","logo.iff"]}}""".toByteArray()
        val parsed = WinampSkinParser.parseSkinFromStream(archive(mapOf("theme.json" to manifest,"panel.dat" to byteArrayOf(1),"logo.iff" to byteArrayOf(2))).inputStream(), "test")!!
        assertEquals(listOf("panel.dat","logo.iff"), SkinArtwork(parsed.artwork).filenames("main.bmp"))
    }
    @Test fun missingInvalidOrExcessiveLayersRejectTheArchive() {
        for (json in listOf("""{"images":{"main":"missing.png"}}""", """{"images":{"main":[]}}""", """{"images":{"unknown":"main.bmp"}}""", """{"images":{"main":["main.bmp","main.bmp"]}}""")) {
            assertNull(WinampSkinParser.parseSkinFromStream(archive(mapOf("theme.json" to json.toByteArray(), "main.bmp" to byteArrayOf(1))).inputStream(), "test"))
        }
    }
    @Test fun metadataFilesAreNeverTreatedAsImageSheets() {
        assertFalse(SkinArtwork.isImageEntry("pledit.txt"))
        assertEquals(emptyList<String>(),SkinArtwork(mapOf("pledit.txt" to byteArrayOf(1))).filenames("pledit.bmp"))
    }
    @Test fun manifestDisplayNameReplacesTheArchiveFilename() {
        val manifest = """{"name":"Infinimedia Default","images":{"main":"main.iff"}}""".toByteArray()
        val parsed = WinampSkinParser.parseSkinFromStream(archive(mapOf("theme.json" to manifest, "main.iff" to byteArrayOf(1))).inputStream(), "old name")!!
        assertEquals("Infinimedia Default", parsed.name)
        assertEquals("Infinimedia Default", parsed.themeColors.name)
    }
    @Test fun duplicateArchiveBasenamesAreRejected() {
        assertNull(WinampSkinParser.parseSkinFromStream(archive(mapOf("first/main.bmp" to byteArrayOf(1),"second/MAIN.BMP" to byteArrayOf(2))).inputStream(), "test"))
    }
}
