package com.infinifolder.storage

import android.content.Context
import android.net.Uri
import com.infinifolder.model.*
import io.github.gretchycat.infinistream.ByteSource

/** The host may install its own provider. Consumer apps default to granted Android URIs. */
class ViewerStorage private constructor(context: Context) {
    val provider: StorageProvider = factory?.invoke(context.applicationContext)
        ?: AndroidStorageProvider(context.applicationContext)
    interface ReadConsistency {
        fun <T> withFreshReads(work: () -> T): T
        fun invalidateReadCaches()
    }
    companion object {
        @Volatile var readConsistency: ReadConsistency = object : ReadConsistency {
            override fun <T> withFreshReads(work: () -> T): T = work()
            override fun invalidateReadCaches() {}
        }
        @Volatile var factory: ((Context) -> StorageProvider)? = null
        @Volatile var streamingSource: ((Context, Uri) -> ByteSource?)? = null
        @Volatile private var instance: ViewerStorage? = null
        fun get(context: Context): ViewerStorage = instance ?: synchronized(this) {
            instance ?: ViewerStorage(context).also { instance = it }
        }
    }
}
