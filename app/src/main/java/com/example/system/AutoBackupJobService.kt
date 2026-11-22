package com.example.system

import android.app.job.JobParameters
import android.app.job.JobService
import com.example.data.firebase.CloudBackupRepository
import com.example.data.local.RRDatabase
import com.example.data.local.UserPreferencesRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AutoBackupJobService : JobService() {
    private var job: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        val user = Firebase.auth.currentUser ?: return false
        if (user.uid.isBlank()) return false

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = RRDatabase.getInstance(applicationContext)
                val cloudRepo = CloudBackupRepository(applicationContext)
                val prefsRepo = UserPreferencesRepository(applicationContext)
                val result = cloudRepo.backupAll(
                    reminders = db.reminderDao().getAllRemindersOnce(),
                    alarms = db.alarmDao().getAllAlarmsOnce(),
                    events = db.calendarEventDao().getAllEventsOnce()
                )
                if (result.isSuccess) {
                    prefsRepo.setLastBackupMillis(System.currentTimeMillis())
                }
                jobFinished(params, false)
            } catch (_: Exception) {
                jobFinished(params, true)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        job?.cancel()
        return true
    }
}
