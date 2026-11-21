package com.infinifolder.viewer

/** A type-wide choice supersedes matching MIME rules and stale rules for this file's extension. */
internal object ExternalDefaultRulePolicy {
    fun replaces(scope: String, key: String, mime: String, sourceKey: String?, currentExtension: String?,
        savedScope: String, savedKey: String, savedMime: String): Boolean =
        (savedScope == scope && savedKey == key) ||
            (scope != "file" && savedScope == "file" && savedKey == sourceKey) ||
            (scope == "type" && savedScope == "extension" &&
                (savedMime == mime || savedKey == currentExtension))
}
