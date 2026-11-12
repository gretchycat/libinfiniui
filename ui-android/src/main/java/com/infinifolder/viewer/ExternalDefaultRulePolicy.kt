package com.infinifolder.viewer

/** An explicit type-wide choice supersedes extension defaults for that same MIME type. */
internal object ExternalDefaultRulePolicy {
    fun replaces(scope: String, key: String, mime: String, sourceKey: String?,
        savedScope: String, savedKey: String, savedMime: String): Boolean =
        (savedScope == scope && savedKey == key) ||
            (scope != "file" && savedScope == "file" && savedKey == sourceKey) ||
            (scope == "type" && savedScope == "extension" && savedMime == mime)
}
