package com.infinifolder.viewer

import org.junit.Assert.*
import org.junit.Test

class ExternalDefaultRulePolicyTest {
    private fun replaces(scope: String, savedScope: String, savedKey: String, savedMime: String = "audio/mpeg") =
        ExternalDefaultRulePolicy.replaces(scope, if (scope == "type") "audio/mpeg" else "mp3",
            "audio/mpeg", "current-file", savedScope, savedKey, savedMime)

    @Test fun typeChoiceReplacesSeededExtensionChoice() {
        assertTrue(replaces("type", "extension", "mp3"))
        assertTrue(replaces("type", "extension", "mpga"))
    }
    @Test fun unrelatedTypesAndOtherFilesRemainUnchanged() {
        assertFalse(replaces("type", "extension", "flac", "audio/flac"))
        assertFalse(replaces("type", "file", "another-file"))
    }
    @Test fun broaderChoiceReplacesCurrentFileOverride() {
        assertTrue(replaces("type", "file", "current-file"))
        assertTrue(replaces("extension", "file", "current-file"))
    }
    @Test fun narrowerChoicePreservesOtherScopes() {
        assertFalse(replaces("extension", "type", "audio/mpeg"))
        assertFalse(replaces("file", "extension", "mp3"))
        assertTrue(replaces("extension", "extension", "mp3"))
    }
}
