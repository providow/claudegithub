package com.gcashagent.tracker.core.domain.model

/** A GCash number (wallet) operated by the agent. */
data class GCashNumber(
    val id: Long = 0,
    val alias: String,
    val phoneNumber: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** "0991 209 3084" — grouped for readability. */
    val formattedNumber: String
        get() = if (phoneNumber.length == 11) {
            "${phoneNumber.substring(0, 4)} ${phoneNumber.substring(4, 7)} ${phoneNumber.substring(7)}"
        } else {
            phoneNumber
        }
}
