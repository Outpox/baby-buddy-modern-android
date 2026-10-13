package com.example.data.repository

import com.example.data.model.BreastSide

/** A running breastfeed as stored in a Baby Buddy timer's name, so every phone can show and continue it. */
internal data class FeedSnapshot(
    val feedingServerId: Long?,
    val side: BreastSide,
    val paused: Boolean,
    val leftSeconds: Int, // as of changedAtMillis
    val rightSeconds: Int,
    val firstSide: BreastSide?,
    val startMillis: Long,
    val pausedAtMillis: Long,
    val lastActiveMillis: Long,
    val changedAtMillis: Long
)

/** A sleep being timed, as stored in a Baby Buddy timer's name. */
internal data class SleepSnapshot(
    val startMillis: Long,
    val changedAtMillis: Long
)

/**
 * Baby Buddy timers only have a name and a start, so the live state goes in the name after a readable part:
 * "Breastfeeding · Left #bb1 k=feed;f=123;a=L;p=0;l=192;r=300;fs=L;ss=…;pa=…;la=…;t=…" (times in epoch seconds).
 * Timers without the "#bb1 " marker (e.g. made in the web UI) are ignored.
 */
internal object SharedTimers {
    private const val MARK = "#bb1 "

    fun encodeFeed(s: FeedSnapshot): String {
        val readable = "Breastfeeding · ${sideName(s.side)}${if (s.paused) " (paused)" else ""}"
        val fields = listOfNotNull(
            "k=feed",
            s.feedingServerId?.let { "f=$it" },
            "a=${s.side.name.first()}",
            "p=${if (s.paused) 1 else 0}",
            "l=${s.leftSeconds}",
            "r=${s.rightSeconds}",
            s.firstSide?.let { "fs=${it.name.first()}" },
            "ss=${s.startMillis / 1000}",
            "pa=${s.pausedAtMillis / 1000}",
            "la=${s.lastActiveMillis / 1000}",
            "t=${s.changedAtMillis}" // millis: orders changes made within the same second
        )
        return "$readable $MARK${fields.joinToString(";")}"
    }

    fun encodeSleep(s: SleepSnapshot): String = "Sleep $MARK" + listOf("k=sleep", "s=${s.startMillis / 1000}", "t=${s.changedAtMillis}").joinToString(";")

    fun decodeFeed(name: String?): FeedSnapshot? {
        val f = fields(name) ?: return null
        if (f["k"] != "feed") return null
        return FeedSnapshot(
            feedingServerId = f["f"]?.toLongOrNull(),
            side = side(f["a"]) ?: return null,
            paused = f["p"] == "1",
            leftSeconds = f["l"]?.toIntOrNull() ?: 0,
            rightSeconds = f["r"]?.toIntOrNull() ?: 0,
            firstSide = side(f["fs"]),
            startMillis = (f["ss"]?.toLongOrNull() ?: return null) * 1000,
            pausedAtMillis = (f["pa"]?.toLongOrNull() ?: 0) * 1000,
            lastActiveMillis = (f["la"]?.toLongOrNull() ?: 0) * 1000,
            changedAtMillis = f["t"]?.toLongOrNull() ?: return null
        )
    }

    fun decodeSleep(name: String?): SleepSnapshot? {
        val f = fields(name) ?: return null
        if (f["k"] != "sleep") return null
        return SleepSnapshot(
            startMillis = (f["s"]?.toLongOrNull() ?: return null) * 1000,
            changedAtMillis = f["t"]?.toLongOrNull() ?: return null
        )
    }

    private fun fields(name: String?): Map<String, String>? {
        val at = name?.indexOf(MARK) ?: -1
        if (name == null || at < 0) return null
        return name.substring(at + MARK.length).trim().split(';').mapNotNull { pair ->
            val kv = pair.split('=', limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()
    }

    private fun side(code: String?): BreastSide? = when (code) {
        "L" -> BreastSide.LEFT
        "R" -> BreastSide.RIGHT
        else -> null
    }

    private fun sideName(side: BreastSide) = if (side == BreastSide.LEFT) "Left" else "Right"
}
