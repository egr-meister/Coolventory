package com.coolventory.app

import android.app.Application
import com.coolventory.app.data.CoolventoryRepository
import com.coolventory.app.data.coolventoryDataStore

/**
 * Application class. Owns the single repository instance (no DI framework needed). Lightweight —
 * no background work, services, networking, or analytics are started here.
 */
class CoolventoryApplication : Application() {

    val repository: CoolventoryRepository by lazy {
        CoolventoryRepository(coolventoryDataStore)
    }
}
