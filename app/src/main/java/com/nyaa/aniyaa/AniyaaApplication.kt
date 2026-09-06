package com.nyaa.aniyaa

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.db.migrateFromLegacy
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.data.repository.BookmarkRepository
import com.nyaa.aniyaa.data.repository.SavedSearchRepository
import com.nyaa.aniyaa.data.repository.SearchHistoryRepository
import com.nyaa.aniyaa.data.repository.NyaaRepository
import com.nyaa.aniyaa.ui.lock.AppLockController
import com.nyaa.aniyaa.work.SavedSearchWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AniyaaApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var prefs: AppPreferences
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var bookmarkRepository: BookmarkRepository
        private set
    lateinit var historyRepository: SearchHistoryRepository
        private set
    lateinit var savedSearchRepository: SavedSearchRepository
        private set
    lateinit var nyaaRepository: NyaaRepository
        private set
    val lockController = AppLockController()

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = AppPreferences(this)
        prefs.applyToSiteConfig()
        AppHttpClient.configure(cacheDir)
        database = AppDatabase.get(this)
        bookmarkRepository = BookmarkRepository(database)
        historyRepository = SearchHistoryRepository(database)
        savedSearchRepository = SavedSearchRepository(database)
        nyaaRepository = NyaaRepository()
        applicationScope.launch {
            database.migrateFromLegacy(this@AniyaaApplication, prefs)
        }
        SavedSearchWorker.enqueue(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (prefs.lockEnabled) {
                    lockController.lock()
                }
            }
        })
    }

    companion object {
        lateinit var instance: AniyaaApplication
            private set
    }
}
