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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.myscreentime.R
import com.example.myscreentime.fragments.dashboardscreen.insights.DashboardInsightService
import com.example.myscreentime.fragments.permissionscreen.AppUsageEntry
import com.example.myscreentime.fragments.permissionscreen.getLastUsedApp
import com.example.myscreentime.fragments.permissionscreen.getMostUsedApp
import com.example.myscreentime.fragments.permissionscreen.getSortedUsedApps
import com.example.myscreentime.fragments.permissionscreen.getTodayScreenTime
import com.example.myscreentime.roomdb.AppRoomDatabase
import kotlinx.coroutines.launch

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
        val insightCard = view.findViewById<View>(R.id.insight_card)
        val insightBody = insightCard.findViewById<TextView>(R.id.insight_body)

        mostUsedTitle.text = "Most Used App"
        mostUsedName.text = app_name_most ?: "No app data"
        bindAppIcon(mostUsedIcon, mostUsed?.packageName)

        lastUsedTitle.text = "Last Used App"
        lastUsedName.text = app_name_last ?: "No app data"
        bindAppIcon(lastUsedIcon, lastUsed?.packageName)

        val appList = view.findViewById<RecyclerView>(R.id.app_list)
        appList.layoutManager = LinearLayoutManager(requireContext())
        appList.setHasFixedSize(true)
        appList.isNestedScrollingEnabled = false
        (appList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        appList.adapter = AppAdapter(buildUsageItems())

        insightBody.text = "Insights will appear after the first daily sync stores a full day of usage."
        viewLifecycleOwner.lifecycleScope.launch {
            val insightService = DashboardInsightService(
                context = requireContext(),
                database = AppRoomDatabase.getInstance(requireContext())
            )
            insightBody.text = insightService.getLatestInsight()
        }
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
            pm.getLaunchIntentForPackage(packageName)
                ?.resolveActivityInfo(pm, PackageManager.MATCH_DEFAULT_ONLY)
                ?.loadLabel(pm)
                ?.toString()
                ?: packageName.substringAfterLast('.').replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
        }
    }

    private fun resolveAppIcon(packageName: String): Drawable? {
        val pm = requireContext().packageManager
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            pm.getLaunchIntentForPackage(packageName)
                ?.resolveActivityInfo(pm, PackageManager.MATCH_DEFAULT_ONLY)
                ?.loadIcon(pm)
        }
    }

    private fun bindAppIcon(imageView: ImageView, packageName: String?) {
        val appIcon = packageName?.let(::resolveAppIcon)
        if (appIcon != null) {
            imageView.setImageDrawable(appIcon)
            imageView.imageTintList = null
        } else {
            imageView.setImageResource(R.drawable.ic_app_fallback)
            imageView.imageTintList = null
        }
    }

    private fun buildUsageItems(): List<RowItem> {
        return getSortedUsedApps(requireContext()).map { usageEntry ->
            RowItem(
                appIcon = resolveAppIcon(usageEntry.packageName),
                appName = resolveAppName(usageEntry.packageName),
                usageTime = formatUsageLabel(usageEntry)
            )
        }
    }

    private fun formatUsageLabel(usageEntry: AppUsageEntry): String {
        return formatTime(usageEntry.totalTimeInForeground)
    }
}
