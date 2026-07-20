package com.example.myscreentime.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.myscreentime.R
import com.example.myscreentime.databinding.FragmentSettingsBinding
import com.example.myscreentime.worker.GoalTrackingWorker

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding?= null
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (isGranted) {
            saveGoalAlertPreference(true)
            binding.dailyGoalAlertSwitch.isChecked = true
        } else {
            binding.dailyGoalAlertSwitch.isChecked = false
            saveGoalAlertPreference(false)
            Toast.makeText(context, "Permission denied. Notifications disabled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        // Use float to support 0.5 steps (30 min increments)
        val savedGoalValue = sharedPref.getFloat("screen_time_goal", 4f)
        binding.screenTimeGoalSlider.value = savedGoalValue
        binding.setLimitTime.text = formatSliderValue(savedGoalValue)

        binding.screenTimeGoalSlider.addOnChangeListener { _, value, _ ->
            binding.setLimitTime.text = formatSliderValue(value)
        }

        binding.btnSetGoal.setOnClickListener {
            val currentVal = binding.screenTimeGoalSlider.value
            sharedPref.edit().putFloat("screen_time_goal", currentVal).apply()
            
            // Reset notified date so they get notified for the new goal today if already exceeded
            sharedPref.edit().remove("last_goal_notified_date").apply()

            GoalTrackingWorker.runOnce(requireContext())
            
            Toast.makeText(context, getString(R.string.goal_set_success, formatSliderValue(currentVal)), Toast.LENGTH_SHORT).show()
        }

        val insightToggle = binding.aiInsightsSwitch
        insightToggle.isChecked = sharedPref.getBoolean("ai_insights_enabled", false)

        insightToggle.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("ai_insights_enabled", isChecked).apply()
        }

        val goalAlertToggle = binding.dailyGoalAlertSwitch
        // Default to false as requested
        goalAlertToggle.isChecked = sharedPref.getBoolean("daily_goal_alert_enabled", false)
        
        goalAlertToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkNotificationPermission()
            } else {
                saveGoalAlertPreference(false)
            }
        }
    }

    private fun formatSliderValue(value: Float): String {
        val hours = value.toInt()
        val minutes = ((value - hours) * 60).toInt()
        return if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h 00m"
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                saveGoalAlertPreference(true)
            }
        } else {
            saveGoalAlertPreference(true)
        }
    }

    private fun saveGoalAlertPreference(enabled: Boolean) {
        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("daily_goal_alert_enabled", enabled).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
