package be.marche.gstock.core

/**
 * A QR code produced by the Gstock web application.
 *
 * The encoded payload is a compact, stable identifier — never a URL or a JSON blob —
 * in the form `GSTOCK:<TYPE>:<id>`, e.g. `GSTOCK:W:7` (worker) or `GSTOCK:T:24` (tool).
 * The app validates the type locally and lets the server resolve the id to the
 * authoritative record via the scan endpoints.
 */
sealed interface GstockCode {
    val id: Long

    data class Worker(override val id: Long) : GstockCode
    data class Tool(override val id: Long) : GstockCode
}

object GstockQr {

    private const val PREFIX = "GSTOCK"

    /** Parse a raw scanned value, or return null if it is not a recognised Gstock code. */
    fun parse(raw: String): GstockCode? {
        val parts = raw.trim().split(":")
        if (parts.size != 3 || !parts[0].equals(PREFIX, ignoreCase = true)) return null
        val id = parts[2].toLongOrNull() ?: return null
        return when (parts[1].uppercase()) {
            "W" -> GstockCode.Worker(id)
            "T" -> GstockCode.Tool(id)
            else -> null
        }
    }
}
