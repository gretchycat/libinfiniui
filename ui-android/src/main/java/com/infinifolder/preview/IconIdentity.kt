package com.infinifolder.preview

import com.infinifolder.mime.MimeRegistry
import com.infinifolder.storage.FileMagicDetector
import com.infinifolder.storage.FileMagicResult

/** Identity decisions do not decide how a file is drawn or whether a container can be browsed. */
data class IconIdentity(val mimeType: String, val associated: Boolean, val ambiguous: Boolean,
    val magicMime: String? = null)
class IconIdentityResolver(private val registry: IconRegistry) {
    fun resolve(name: String, supplied: String?, detect: () -> String?): IconIdentity {
        val candidates = registry.candidates(name)
        val stated = MimeRegistry.normalize(supplied)
        val candidate = candidates.singleOrNull()?.takeUnless(MimeRegistry::isUnspecified)
        val detected = try { detect()?.let(MimeRegistry::normalize) }
        catch (_: Exception) {
            // A denied/offline source cannot invalidate an intentional association.
            if (candidate != null) return IconIdentity(candidate, true, false)
            null
        }
        // Extensions can have both a preferred display identity and a parser
        // MIME alias. Use the extension's registered identity when the library
        // catalog declares the detected parser MIME for that same suffix.
        if (detected != null && parserDeclares(name, detected)) {
            val extensionMime = MimeRegistry.normalize(MimeRegistry.guess(name))
            if (extensionMime in candidates) return IconIdentity(extensionMime, true, false, detected)
        }
        if (candidate != null) {
            AudioIconFormats.extensionCandidateAfterInconclusiveProbe(candidate, detected)
                ?.let { return IconIdentity(it, true, false) }
            // A ZIP signature confirms the package container, not its inner
            // document type. Keep the extension's registered package identity.
            if (detected == "application/zip" && MimeRegistry.packageFormat(name, candidate) != null) {
                return IconIdentity(candidate, true, false, detected)
            }
            // Some parsers publish a useful MIME alias for an extension rather
            // than the host's display identity (for example PostScript for EPS).
            // Preserve the extension identity when a runtime handler explicitly
            // declares that alias for this suffix.
            if (detected != null && parserDeclares(name, detected)) {
                return IconIdentity(candidate, true, false, detected)
            }
            val validated = FileMagicDetector.validatedExtensionMime(name, candidate,
                detected?.let { FileMagicResult(it, "") })
            return IconIdentity(validated, validated == candidate, false, detected)
        }

        // Generic or conflicting extension associations need a content identity;
        // no signature and no text evidence means generic binary.
        val packageMime = stated.takeIf { MimeRegistry.packageFormat(name, it) != null }
        val identity = if (detected == "application/zip" && packageMime != null) packageMime
            else detected ?: if (candidates.isEmpty()) stated else "application/octet-stream"
        return IconIdentity(identity, false, candidates.size > 1, detected)
    }

    private fun parserDeclares(fileName: String, mimeType: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isBlank()) return false
        val mime = MimeRegistry.normalize(mimeType)
        val parserOperations = setOf(
            io.github.gretchycat.infinistream.Operations.IDENTIFY,
            io.github.gretchycat.infinistream.Operations.DECODE,
            io.github.gretchycat.infinistream.Operations.THUMBNAIL,
            io.github.gretchycat.infinistream.Operations.VIEW,
            io.github.gretchycat.infinistream.Operations.PLAY,
        )
        return io.github.gretchycat.infinistream.RuntimeCapabilities.installed().descriptors().any { handler ->
            handler.operations.any(parserOperations::contains) && handler.formats.any { format ->
                extension in format.extensions.map(String::lowercase) &&
                    format.mimeTypes.any { MimeRegistry.normalize(it) == mime }
            }
        }
    }
}
