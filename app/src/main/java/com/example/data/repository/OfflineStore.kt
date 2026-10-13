package com.example.data.repository

import com.example.data.model.DiaperEvent
import com.example.data.model.FeedingEvent
import com.example.data.model.NoteEvent
import com.example.data.model.PumpingEvent
import com.example.data.model.SleepEvent
import com.example.data.model.TemperatureEvent
import com.example.data.model.TummyTimeEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * A write to the Baby Buddy server, kept as plain data so the queue survives the app being killed.
 * [body] holds the request fields without "child", which is added when sending.
 */
data class QueuedWrite(
    val endpoint: String, // "feedings" | "changes" | "tummy-times" | "pumping" | "sleep" | ...
    val action: String, // ACTION_CREATE | ACTION_UPDATE | ACTION_DELETE
    val localId: Long,
    val serverId: Long? = null,
    val body: Map<String, Any?> = emptyMap(),
    val attempts: Int = 0
) {
    companion object {
        const val ACTION_CREATE = "create"
        const val ACTION_UPDATE = "update"
        const val ACTION_DELETE = "delete"
    }
}

/** Today's entries as last shown, so they (including unsent ones) are visible offline after a restart. */
data class TodayCache(
    val day: String, // yyyy-MM-dd, device-local
    val feedings: List<FeedingEvent> = emptyList(),
    val diapers: List<DiaperEvent> = emptyList(),
    val tummyTimes: List<TummyTimeEvent> = emptyList(),
    val pumpings: List<PumpingEvent> = emptyList(),
    val temperatures: List<TemperatureEvent> = emptyList(),
    val notes: List<NoteEvent> = emptyList(),
    val sleeps: List<SleepEvent> = emptyList()
)

/** JSON files under filesDir/offline. Writes go to a temp file first so a crash can't leave half a file. */
internal class OfflineStore(private val dir: File) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val queueAdapter = moshi.adapter<List<QueuedWrite>>(
        Types.newParameterizedType(List::class.java, QueuedWrite::class.java)
    )
    private val cacheAdapter = moshi.adapter(TodayCache::class.java)

    private val queueFile get() = File(dir, "pending_writes.json")
    private val cacheFile get() = File(dir, "today.json")

    fun loadQueue(): List<QueuedWrite> = runCatching { queueAdapter.fromJson(queueFile.readText()) }.getOrNull().orEmpty()

    fun saveQueue(queue: List<QueuedWrite>) = write(queueFile, queueAdapter.toJson(queue))

    fun loadCache(): TodayCache? = runCatching { cacheAdapter.fromJson(cacheFile.readText()) }.getOrNull()

    fun saveCache(cache: TodayCache) = write(cacheFile, cacheAdapter.toJson(cache))

    fun clear() {
        queueFile.delete()
        cacheFile.delete()
    }

    @Synchronized
    private fun write(file: File, json: String) {
        dir.mkdirs()
        val tmp = File(dir, file.name + ".tmp")
        tmp.writeText(json)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
