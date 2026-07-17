package com.example.myscreentime.fragments.permissionscreen

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.myscreentime.R

class PermissionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_permission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val permissionBtn = view.findViewById<Button>(R.id.permission_btn)
        val cardContent = view.findViewById<FrameLayout>(R.id.card_content)
        layoutInflater.inflate(
            R.layout.permission_card_content,
            cardContent,
            true
        )

        permissionBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (hasUsageStatsPermission(requireContext())) {
                    navigateToDashboard()
                } else {
                    requestUsageStatsPermission(requireContext())
                }
            } else {
                navigateToDashboard()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndNavigate()
    }

    private fun checkPermissionAndNavigate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (hasUsageStatsPermission(requireContext())) {
                // Logs for testing
                getAppUsageStats(requireContext())
                navigateToDashboard()
            }
        } else {
            navigateToDashboard()
        }
    }

    private fun navigateToDashboard() {
        findNavController().navigate(
            R.id.dashboardFragment,
            null,
            NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_graph, true)
                .build()
        )
    }
}
