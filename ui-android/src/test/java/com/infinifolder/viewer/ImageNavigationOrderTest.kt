package com.infinifolder.viewer

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageNavigationOrderTest {
    @get:Rule val temp = TemporaryFolder()
    private val items = listOf(ImageNavigationItem("content://opaque/third", "z.svgz", "image/svg+xml"),
        ImageNavigationItem("archive://source?path=b.ora", "b.ora", "image/openraster"),
        ImageNavigationItem("content://opaque/first", "a.kra", "image/x-krita"))
    @Test fun snapshotPreservesDisplayOrderWithoutSortingNamesOrUris() {
        val token = ImageNavigationOrder.write(temp.root, items)!!
        assertEquals(items, ImageNavigationOrder.read(temp.root, token))
        assertEquals(items.reversed(), ImageNavigationOrder.read(temp.root, ImageNavigationOrder.write(temp.root, items.reversed())!!))
    }
    @Test fun traversalMissingAndTruncatedSnapshotsUseFallback() {
        assertNull(ImageNavigationOrder.read(temp.root, "../outside"))
        assertNull(ImageNavigationOrder.read(temp.root, "00000000-0000-0000-0000-000000000000"))
        val token = ImageNavigationOrder.write(temp.root, items)!!
        File(temp.root, "$token.bin").writeBytes(byteArrayOf(1,2,3))
        assertNull(ImageNavigationOrder.read(temp.root, token))
    }
    @Test fun oversizedOrEmptySnapshotDoesNotLeavePartialFiles() {
        assertNull(ImageNavigationOrder.write(temp.root, emptyList()))
        assertNull(ImageNavigationOrder.write(temp.root, listOf(ImageNavigationItem("x".repeat(100000),"large", "image/png"))))
        assertTrue(temp.root.listFiles().orEmpty().isEmpty())
    }
    @Test fun oldSnapshotsAreBoundedAndCurrentSnapshotRemainsReadable() {
        repeat(35) { ImageNavigationOrder.write(temp.root, items) }
        val token = ImageNavigationOrder.write(temp.root, items)!!
        assertTrue(temp.root.listFiles().orEmpty().size <= 32)
        assertEquals(items, ImageNavigationOrder.read(temp.root, token))
    }
}
