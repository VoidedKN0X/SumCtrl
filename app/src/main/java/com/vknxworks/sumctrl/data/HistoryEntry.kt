package com.vknxworks.sumctrl.data

import kotlinx.serialization.Serializable

/**
 * One finished sum: the keys typed, the exact result (as a plain decimal string) and an optional note.
 * Files from before notes existed have no "note" field and read as no note.
 */
@Serializable
data class HistoryEntry(
    val id: Long,
    val time: Long,
    val tokens: List<String>,
    val result: String,
    val note: String? = null,
)
