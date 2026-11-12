package com.infinifolder.preview

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IconIdentityPolicyTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun associationsControlDisplayIdentityWithoutProbingKnownPackages() {
        val registry=IconRegistry.at(temp.root);val resolver=IconIdentityResolver(registry)
        for(name in listOf("book.docx","book.epub","program.apk","notes.txt")) {
            val id=resolver.resolve(name,"application/zip") {error("Known association must not be replaced by ZIP magic")}
            assertEquals(registry.candidates(name).single(),id.mimeType);assertTrue(id.associated)
        }
    }
    @Test fun noExtensionUnknownAndConflictingAssociationsNeedDetection() {
        val registry=IconRegistry.at(temp.root);val resolver=IconIdentityResolver(registry)
        var probes=0
        assertEquals("image/png",resolver.resolve("no-extension","application/octet-stream") {probes++;"image/png"}.mimeType)
        assertEquals("application/zip",resolver.resolve("archive.unknown","application/octet-stream") {probes++;"application/zip"}.mimeType)
        registry.save(registry.mime("application/first").copy(extensions=setOf("conflict")))
        registry.save(registry.mime("application/second").copy(extensions=setOf("conflict")))
        assertTrue(resolver.resolve("a.conflict","") {probes++;"text/plain"}.ambiguous)
        assertEquals(3,probes)
    }
    @Test fun overrideAndThumbnailOrderingMatchesCheckboxMeaning() {
        val base=IconRegistry.MimeRecord("image/png",setOf("png"),"image-png",overrideIcon="uri_icons/icon.png",isImage=true,overrideImageIcon=true)
        assertEquals(IconPolicy.Step.MIME_OVERRIDE,IconPolicy.steps(base).first())
        assertEquals(listOf(IconPolicy.Step.THUMBNAIL,IconPolicy.Step.MIME_OVERRIDE),IconPolicy.steps(base.copy(overrideImageIcon=false)).take(2))
        assertFalse(IconPolicy.steps(base.copy(isImage=false,overrideImageIcon=false)).contains(IconPolicy.Step.THUMBNAIL))
    }
    @Test fun explicitMappingIsNotReplacedByContentAndPackageMimeSurvivesZipMagic() {
        val registry=IconRegistry.at(temp.root);val resolver=IconIdentityResolver(registry)
        registry.save(registry.mime("application/pdf").copy(extensions=setOf("intentional")))
        assertEquals("application/pdf",resolver.resolve("book.intentional","") {error("No icon magic override")}.mimeType)
        val docx="application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        assertEquals(docx,resolver.resolve("no-extension",docx) {"application/zip"}.mimeType)
    }
    @Test fun inconclusiveAudioProbeKeepsExtensionIdentityButExplicitFailureBecomesBinary() {
        val registry=IconRegistry.at(temp.root);val resolver=IconIdentityResolver(registry)
        val candidate=registry.candidates("recording.mp3").single()
        assertEquals("audio/mpeg",candidate)
        assertEquals(candidate,resolver.resolve("recording.mp3","application/octet-stream") {null}.mimeType)
        assertEquals("application/octet-stream",resolver.resolve("broken.mp3",candidate) {"application/octet-stream"}.mimeType)
        assertEquals("application/pdf",resolver.resolve("renamed.mp3",candidate) {"application/pdf"}.mimeType)
        assertNull(AudioIconFormats.extensionCandidateAfterInconclusiveProbe(candidate,null,0L))
    }
}
