package com.example.data.scheduler

import android.content.Context
import android.content.SharedPreferences
import com.example.service.CrawlerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CronScheduleState(
    val isEnabled: Boolean = false,
    val intervalHours: Int = 6,
    val nextRunFormatted: String = "DISABLED",
    val isCronRunning: Boolean = false
)

class CronSchedulerManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cron_scheduler_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var cronJob: Job? = null

    private val _scheduleState = MutableStateFlow(CronScheduleState())
    val scheduleState: StateFlow<CronScheduleState> = _scheduleState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val enabled = prefs.getBoolean("cron_enabled", false)
        val interval = prefs.getInt("cron_interval_hours", 6)
        val nextRun = prefs.getString("cron_next_run", "DISABLED") ?: "DISABLED"

        _scheduleState.value = CronScheduleState(
            isEnabled = enabled,
            intervalHours = interval,
            nextRunFormatted = if (enabled) nextRun else "DISABLED"
        )

        if (enabled) {
            startCronLoop()
        }
    }

    fun setCronSchedule(enabled: Boolean, intervalHours: Int) {
        prefs.edit()
            .putBoolean("cron_enabled", enabled)
            .putInt("cron_interval_hours", intervalHours)
            .apply()

        if (!enabled) {
            cronJob?.cancel()
            cronJob = null
            prefs.edit().putString("cron_next_run", "DISABLED").apply()
            _scheduleState.value = CronScheduleState(isEnabled = false, intervalHours = intervalHours, nextRunFormatted = "DISABLED")
        } else {
            val nextRunTime = System.currentTimeMillis() + (intervalHours * 3600 * 1000L)
            val formatStr = java.text.SimpleDateFormat("HH:mm, dd MMM", java.util.Locale.US).format(java.util.Date(nextRunTime))
            prefs.edit().putString("cron_next_run", formatStr).apply()

            _scheduleState.value = CronScheduleState(
                isEnabled = true,
                intervalHours = intervalHours,
                nextRunFormatted = "NEXT RUN: $formatStr"
            )
            startCronLoop()
        }
    }

    private fun startCronLoop() {
        cronJob?.cancel()
        cronJob = scope.launch {
            while (_scheduleState.value.isEnabled) {
                val intervalMs = _scheduleState.value.intervalHours * 3600 * 1000L
                delay(intervalMs)

                if (_scheduleState.value.isEnabled) {
                    _scheduleState.value = _scheduleState.value.copy(isCronRunning = true)
                    
                    // Trigger foreground service crawl cycle
                    CrawlerService.startService(context)

                    // Allow crawl to run for 30 minutes, then update next run
                    delay(30 * 60 * 1000L)

                    val nextRunTime = System.currentTimeMillis() + intervalMs
                    val formatStr = java.text.SimpleDateFormat("HH:mm, dd MMM", java.util.Locale.US).format(java.util.Date(nextRunTime))
                    prefs.edit().putString("cron_next_run", formatStr).apply()

                    _scheduleState.value = _scheduleState.value.copy(
                        isCronRunning = false,
                        nextRunFormatted = "NEXT RUN: $formatStr"
                    )
                }
            }
        }
    }
}
