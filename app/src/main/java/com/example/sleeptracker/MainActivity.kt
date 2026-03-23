package com.example.sleeptracker

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sleeptracker.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isTracking = false
    private var startTime: Long = 0
    private var targetBedtimeHour: Int = -1
    private var targetBedtimeMinute: Int = -1

    private val PREFS_NAME = "SleepTrackerPrefs"
    private val KEY_TOTAL_SLEEP = "TotalSleep"
    private val KEY_SESSION_COUNT = "SessionCount"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateSummary()

        binding.btnStart.setOnClickListener {
            startTracking()
        }

        binding.btnStop.setOnClickListener {
            stopTracking()
        }

        binding.btnReset.setOnClickListener {
            resetTracking()
        }

        binding.btnSettings.setOnClickListener {
            showTimePickerDialog()
        }

        binding.chronometer.onChronometerTickListener = Chronometer.OnChronometerTickListener {
            val elapsedMillis = SystemClock.elapsedRealtime() - it.base
            updateSleepStage(elapsedMillis)
        }
    }

    private fun startTracking() {
        if (!isTracking) {
            binding.chronometer.base = SystemClock.elapsedRealtime()
            binding.chronometer.start()
            startTime = System.currentTimeMillis()
            isTracking = true
            binding.btnStart.isEnabled = false
            binding.btnStop.isEnabled = true
            binding.tvSleepStage.text = "Sleep Stage: Light Sleep"
        }
    }

    private fun stopTracking() {
        if (isTracking) {
            binding.chronometer.stop()
            val elapsedMillis = SystemClock.elapsedRealtime() - binding.chronometer.base
            isTracking = false
            binding.btnStart.isEnabled = true
            binding.btnStop.isEnabled = false

            saveSleepData(elapsedMillis)
            updateSummary()
            checkGoal(elapsedMillis)
        }
    }

    private fun resetTracking() {
        binding.chronometer.stop()
        binding.chronometer.base = SystemClock.elapsedRealtime()
        isTracking = false
        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
        binding.tvSleepStage.text = "Sleep Stage: -"
        binding.tvGoalStatus.text = ""
    }

    private fun updateSleepStage(elapsedMillis: Long) {
        val hours = elapsedMillis / (1000 * 60 * 60.0)
        if (hours >= 1.5) {
            binding.tvSleepStage.text = "Sleep Stage: Deep Sleep"
        } else {
            binding.tvSleepStage.text = "Sleep Stage: Light Sleep"
        }
    }

    private fun saveSleepData(durationMillis: Long) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalSleep = prefs.getLong(KEY_TOTAL_SLEEP, 0) + durationMillis
        val count = prefs.getInt(KEY_SESSION_COUNT, 0) + 1

        val editor = prefs.edit()
        editor.putLong(KEY_TOTAL_SLEEP, totalSleep)
        editor.putInt(KEY_SESSION_COUNT, count)
        editor.apply()
    }

    private fun updateSummary() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalSleep = prefs.getLong(KEY_TOTAL_SLEEP, 0)
        val count = prefs.getInt(KEY_SESSION_COUNT, 0)

        if (count > 0) {
            val avgMillis = totalSleep / count
            val hours = avgMillis / (1000 * 60 * 60)
            val minutes = (avgMillis % (1000 * 60 * 60)) / (1000 * 60)
            binding.tvSummary.text = "Avg. Sleep: ${hours}h ${minutes}m"
        } else {
            binding.tvSummary.text = "Avg. Sleep: 0h 0m"
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            targetBedtimeHour = h
            targetBedtimeMinute = m
            Toast.makeText(this, "Target Bedtime set to $h:$m", Toast.LENGTH_SHORT).show()
        }, hour, minute, true).show()
    }

    private fun checkGoal(elapsedMillis: Long) {
        if (targetBedtimeHour == -1) return

        // Simple goal logic: assume target is 8 hours of sleep for demonstration
        val goalMillis = 8 * 60 * 60 * 1000L
        val diff = elapsedMillis - goalMillis
        val diffHours = Math.abs(diff) / (1000 * 60 * 60.0)

        if (diff >= 0) {
            binding.tvGoalStatus.text = "Goal met! You slept ${String.format("%.1f", diffHours)}h more than 8h goal."
        } else {
            binding.tvGoalStatus.text = "Goal not met. You slept ${String.format("%.1f", diffHours)}h less than 8h goal."
        }
    }
}
