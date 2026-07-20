package com.example.myscreentime.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myscreentime.R
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val insight_toggle = view.findViewById<SwitchMaterial>(R.id.ai_insights_switch)
        val sharedPref =
            requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        insight_toggle.isChecked = sharedPref.getBoolean("ai_insights_enabled", false)

        insight_toggle.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("ai_insights_enabled", isChecked).apply()
        }
    }
}