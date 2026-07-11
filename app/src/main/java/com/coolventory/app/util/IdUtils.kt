package com.coolventory.app.util

import java.util.UUID

/** Locally-generated unique identifiers. No network, no device identifiers. */
object IdUtils {
    fun newId(prefix: String = "id"): String = "${prefix}_${UUID.randomUUID()}"
}
