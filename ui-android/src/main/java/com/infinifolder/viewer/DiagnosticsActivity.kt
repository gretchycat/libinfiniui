package com.infinifolder.viewer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.*
import com.infinifolder.storage.DiagnosticLog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** The folder picker can grant access to a Termux project/build directory through its document provider. */
class DiagnosticsActivity : ViewerActivity(), DebugDumpObserver {
    override fun onDebugDumpFinished() { refresh() }
    private lateinit var output: TextView
    private lateinit var folderStatus: TextView
    private var refreshGeneration = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagnosticLog.init(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,24) }
        layout.addView(TextView(this).apply { text = "Diagnostics log"; textSize = 24f })
        layout.addView(Button(this).apply {
            text = "Choose or change debug dump folder"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }, 903)
            }
        })
        layout.addView(Button(this).apply { text = "Offload debug information"; setOnClickListener { DebugTools.offload(this@DiagnosticsActivity) } })
        layout.addView(Button(this).apply { text = "Refresh log"; setOnClickListener { refresh() } })
        folderStatus = TextView(this).apply { textSize = 14f; setPadding(0, 12, 0, 12) }
        layout.addView(folderStatus)
        output = TextView(this).apply { textSize = 12f; setTextIsSelectable(true) }
        layout.addView(ScrollView(this).apply { addView(output) }, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(layout)
        refresh()
    }
    private fun refresh() {
        val generation = ++refreshGeneration
        Thread {
        val destination = com.infinifolder.storage.DebugDumpDestination.get(this)
        val provider = com.infinifolder.storage.ViewerStorage.get(this).provider
        val folderName = destination?.let { ref ->
            (runCatching { provider.stat(ref) }.getOrNull() as? com.infinifolder.model.ProviderResult.Success)?.value?.metadata?.name
                ?: ref.documentId
        }
        val status = if (destination == null) "Debug dump folder: not set. Choose a folder to save crash and diagnostic dumps."
            else "Debug dump folder: ${folderName ?: destination.documentId}"
        val text = "Debug dump location: ${destination?.documentId ?: "not set"}\n" +
            (DiagnosticLog.lastExportError?.let { "Last write failed: $it\n" } ?: "") + DiagnosticLog.snapshot(this).takeLast(64 * 1024)
        runOnUiThread { if (!isFinishing && !isDestroyed && generation == refreshGeneration) {
            folderStatus.text = status
            output.text = text
        } }
        }.start()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 903 || resultCode != RESULT_OK) return
        val tree = data?.data ?: return
        val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        Thread {
            val result = runCatching {
                if (flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0) {
                    error("The selected folder did not grant write access")
                }
                if (data.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == 0) {
                    error("The selected folder does not support persistent access")
                }
                contentResolver.takePersistableUriPermission(tree, flags)
                val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                val provider = com.infinifolder.storage.ViewerStorage.get(this).provider
                val directory = provider.refForUri(parent) ?: error("Folder is not supported")
                val name = "infinifolder-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".log"
                val file = DocumentsContract.createDocument(contentResolver, parent, "text/plain", name) ?: error("Could not create log file")
                DiagnosticLog.setDestination(this, file)
                DiagnosticLog.writeNow(this).getOrThrow()
                com.infinifolder.storage.DebugDumpDestination.set(this, directory)
            }
            runOnUiThread {
                Toast.makeText(this, if (result.isSuccess) "Debug dump folder set; a test log was written there" else
                    "Could not set debug dump folder: ${result.exceptionOrNull()?.message ?: "selection failed"}", Toast.LENGTH_LONG).show()
                refresh()
                if (result.isSuccess) DebugTools.onActivityResumed(this)
            }
        }.start()
    }
}
