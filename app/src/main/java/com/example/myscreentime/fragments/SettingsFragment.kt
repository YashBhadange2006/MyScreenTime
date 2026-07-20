package com.example.myscreentime.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myscreentime.R
import com.example.myscreentime.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding?= null
    private val binding get() = _binding!!

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
        val savedGoalValue = sharedPref.getInt("screen_time_goal", 4).toFloat()
        binding.screenTimeGoalSlider.value = savedGoalValue
        binding.setLimitTime.text = getString(R.string.screen_time_goal_format, savedGoalValue.toInt())

        // Save the new value dynamically whenever the user moves the slider
        binding.screenTimeGoalSlider.addOnChangeListener { _, value, fromUser ->

            val hours = value.toInt()
            val minutes = ((value - hours) * 60).toInt()

            binding.setLimitTime.text = "${hours}h ${String.format("%02dm", minutes)}"
            if (fromUser) {
                sharedPref.edit().putFloat("screen_time_goal", value).apply()
            }
        }

        val insightToggle = binding.aiInsightsSwitch
        insightToggle.isChecked = sharedPref.getBoolean("ai_insights_enabled", false)

        insightToggle.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("ai_insights_enabled", isChecked).apply()
        }

        val goalAlertToggle = binding.dailyGoalAlertSwitch
        goalAlertToggle.isChecked = sharedPref.getBoolean("daily_goal_alert_enabled", true)
        goalAlertToggle.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("daily_goal_alert_enabled", isChecked).apply()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
