package com.coolventory.app.data

import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration used for all persisted collections.
 *
 * - ignoreUnknownKeys: forward/backward compatible when fields are removed.
 * - encodeDefaults: newer fields are written so re-reads are stable.
 * - isLenient: tolerant of minor malformations.
 * - coerceInputValues: unknown enum values / nulls fall back to declared defaults instead of throwing.
 */
object CoolventoryJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
}
