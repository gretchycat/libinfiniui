package com.infinifolder.viewer

/** A saved app choice can be launched explicitly when installed but omitted by MIME-filter queries. */
internal object SavedDefaultLaunchPolicy {
    fun includeExplicit(savedComponent: String?, compatibleComponents: Set<String>, installedAndEnabled: Boolean): Boolean =
        savedComponent != null && savedComponent !in compatibleComponents && installedAndEnabled
}
