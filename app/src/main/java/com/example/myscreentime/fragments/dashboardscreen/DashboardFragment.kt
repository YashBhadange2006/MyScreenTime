package com.example.myscreentime.fragments.dashboardscreen

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.myscreentime.R
import com.example.myscreentime.fragments.permissionscreen.getLastUsedApp
import com.example.myscreentime.fragments.permissionscreen.getMostUsedApp
import com.example.myscreentime.fragments.permissionscreen.getTodayScreenTime

class DashboardFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val totalTime = getTodayScreenTime(requireContext())
        val tvTotalScreenTime = view.findViewById<TextView>(R.id.total_screen_textview)
        tvTotalScreenTime.text = formatTime(totalTime)

        val mostUsed = getMostUsedApp(requireContext())
        val lastUsed = getLastUsedApp(requireContext())

        val app_name_most = mostUsed?.packageName?.let(::resolveAppName)
        val app_name_last = lastUsed?.packageName?.let(::resolveAppName)

        val mostUsedCard = view.findViewById<View>(R.id.most_used_app_card)
        val mostUsedIcon = mostUsedCard.findViewById<ImageView>(R.id.iv_app_icon)
        val mostUsedTitle = mostUsedCard.findViewById<TextView>(R.id.text_above_app_name)
        val mostUsedName = mostUsedCard.findViewById<TextView>(R.id.tv_app_name)

        val lastUsedCard = view.findViewById<View>(R.id.last_used_app_card)
        val lastUsedIcon = lastUsedCard.findViewById<ImageView>(R.id.iv_app_icon)
        val lastUsedTitle = lastUsedCard.findViewById<TextView>(R.id.text_above_app_name)
        val lastUsedName = lastUsedCard.findViewById<TextView>(R.id.tv_app_name)

        mostUsedTitle.text = "Most Used App"
        mostUsedName.text = app_name_most ?: "No app data"
        bindAppIcon(mostUsedIcon, mostUsed?.packageName)

        lastUsedTitle.text = "Last Used App"
        lastUsedName.text = app_name_last ?: "No app data"
        bindAppIcon(lastUsedIcon, lastUsed?.packageName)
    }


    fun formatTime(milliseconds : Long): String {
        val hours = milliseconds/(1000*60*60)
        val minutes = (milliseconds/(1000*60)) % 60

        return "${hours}h ${minutes}m"
    }

    private fun resolveAppName(packageName: String): String {
        val pm = requireContext().packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.').replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }

    private fun resolveAppIcon(packageName: String): Drawable? {
        val pm = requireContext().packageManager
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun bindAppIcon(imageView: ImageView, packageName: String?) {
        val appIcon = packageName?.let(::resolveAppIcon)
        if (appIcon != null) {
            imageView.setImageDrawable(appIcon)
            imageView.imageTintList = null
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher_round)
            imageView.imageTintList = null
        }
    }
}
