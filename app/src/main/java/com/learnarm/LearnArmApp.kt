package com.learnarm

import android.app.Application
import android.util.Log
import com.learnarm.core.data.di.ApplicationScope
import com.learnarm.core.data.seed.AlphabetSeeder
import com.learnarm.core.data.seed.PhrasesSeeder
import com.learnarm.core.data.sync.LessonsSyncer
import com.learnarm.core.data.sync.SyncOutcome
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LearnArmApp : Application() {

    @Inject lateinit var alphabetSeeder: AlphabetSeeder
    @Inject lateinit var phrasesSeeder: PhrasesSeeder
    @Inject lateinit var lessonsSyncer: LessonsSyncer
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            alphabetSeeder.seedIfNeeded()
            phrasesSeeder.seedIfNeeded()
            val outcome = runCatching { lessonsSyncer.syncIfNeeded() }
                .getOrElse { error ->
                    Log.w(TAG, "Lesson sync failed", error)
                    SyncOutcome.NetworkSkipped
                }
            Log.i(TAG, "Lesson sync outcome: $outcome")
        }
    }

    private companion object {
        const val TAG = "LearnArmApp"
    }
}
