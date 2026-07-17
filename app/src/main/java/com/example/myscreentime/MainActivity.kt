package com.example.myscreentime

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.navigation.ui.setupWithNavController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.myscreentime.fragments.permissionscreen.hasUsageStatsPermission
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController
        bottomNavigation = findViewById(R.id.bottomNavigation)
        val navHostView = findViewById<View>(R.id.nav_host_fragment)

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
            setStartDestination(
                if (hasUsagePermission()) {
                    R.id.dashboardFragment
                } else {
                    R.id.permissionFragment
                }
            )
        }
        navController.setGraph(navGraph, null)
        bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigation.isVisible = destination.id != R.id.permissionFragment
        }

        ViewCompat.setOnApplyWindowInsetsListener(navHostView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()

        val navController = navHostFragment.navController
        val hasPermission = hasUsagePermission()
        val currentDestinationId = navController.currentDestination?.id

        if (!hasPermission && currentDestinationId != R.id.permissionFragment) {
            navController.navigate(R.id.permissionFragment, null, clearBackStackOptions())
        } else if (hasPermission && currentDestinationId == R.id.permissionFragment) {
            navController.navigate(R.id.dashboardFragment, null, clearBackStackOptions())
        }
    }

    private fun hasUsagePermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasUsageStatsPermission(this)
    }

    private fun clearBackStackOptions(): NavOptions {
        return NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.nav_graph, true)
            .build()
    }
}
