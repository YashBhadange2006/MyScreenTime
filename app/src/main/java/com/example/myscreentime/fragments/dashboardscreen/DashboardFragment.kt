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
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private lateinit var insightBody: TextView
    private lateinit var insightService: DashboardInsightService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTotalScreenTime = view.findViewById<TextView>(R.id.total_screen_textview)
        val mostUsedCard = view.findViewById<View>(R.id.most_used_app_card)
        val mostUsedIcon = mostUsedCard.findViewById<ImageView>(R.id.iv_app_icon)
        val mostUsedTitle = mostUsedCard.findViewById<TextView>(R.id.text_above_app_name)
        val mostUsedName = mostUsedCard.findViewById<TextView>(R.id.tv_app_name)
        val lastUsedCard = view.findViewById<View>(R.id.last_used_app_card)
        val lastUsedIcon = lastUsedCard.findViewById<ImageView>(R.id.iv_app_icon)
        val lastUsedTitle = lastUsedCard.findViewById<TextView>(R.id.text_above_app_name)
        val lastUsedName = lastUsedCard.findViewById<TextView>(R.id.tv_app_name)
        val insightCard = view.findViewById<View>(R.id.insight_card)
        insightBody = insightCard.findViewById<TextView>(R.id.insight_body)
        val appList = view.findViewById<RecyclerView>(R.id.app_list)
        appList.layoutManager = LinearLayoutManager(requireContext())
        appList.setHasFixedSize(true)
        appList.isNestedScrollingEnabled = false
        (appList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        showSkeletonText(tvTotalScreenTime)
        mostUsedTitle.text = "Most Used App"
        showSkeletonText(mostUsedName)
        showSkeletonIcon(mostUsedIcon)
        lastUsedTitle.text = "Last Used App"
        showSkeletonText(lastUsedName)
        showSkeletonIcon(lastUsedIcon)
        appList.adapter = AppAdapter(emptyList())

        insightService = DashboardInsightService(
            context = requireContext(),
            database = AppRoomDatabase.getInstance(requireContext())
        )

        insightBody.text = "Insights will appear after the first daily sync stores a full day of usage."
        viewLifecycleOwner.lifecycleScope.launch {
            val dashboardData = withContext(Dispatchers.IO) {
                buildDashboardData()
            }

            tvTotalScreenTime.text = formatTime(dashboardData.totalTime)
            tvTotalScreenTime.background = null
            mostUsedName.text = dashboardData.mostUsedName ?: "No app data"
            mostUsedName.background = null
            bindAppIcon(mostUsedIcon, dashboardData.mostUsedPackage)
            lastUsedName.text = dashboardData.lastUsedName ?: "No app data"
            lastUsedName.background = null
            bindAppIcon(lastUsedIcon, dashboardData.lastUsedPackage)
            appList.adapter = AppAdapter(dashboardData.usageItems)
        }
    }

    override fun onResume(){
        super.onResume()

        val sharedPref = requireContext().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val isAiInsightsEnabled = sharedPref.getBoolean("ai_insights_enabled", false)

        if (isAiInsightsEnabled) {
                viewLifecycleOwner.lifecycleScope.launch {
                insightBody.text = insightService.getLatestInsight()
            }
        } else {
            insightBody.text = "Turn on the AI insights option"
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

    private fun showSkeletonText(textView: TextView) {
        textView.text = ""
        textView.minWidth = resources.getDimensionPixelSize(R.dimen.dashboard_skeleton_text_width)
        textView.minHeight = resources.getDimensionPixelSize(R.dimen.dashboard_skeleton_text_height)
        textView.background = requireContext().getDrawable(R.drawable.skeleton_bar)
    }

    private fun showSkeletonIcon(imageView: ImageView) {
        imageView.setImageDrawable(null)
        imageView.imageTintList = null
    }

    private fun buildDashboardData(): DashboardData {
        val totalTime = getTodayScreenTime(requireContext())
        val mostUsed = getMostUsedApp(requireContext())
        val lastUsed = getLastUsedApp(requireContext())

        return DashboardData(
            totalTime = totalTime,
            mostUsedPackage = mostUsed?.packageName,
            mostUsedName = mostUsed?.packageName?.let(::resolveAppName),
            lastUsedPackage = lastUsed?.packageName,
            lastUsedName = lastUsed?.packageName?.let(::resolveAppName),
            usageItems = buildUsageItems()
        )
    }
}

private data class DashboardData(
    val totalTime: Long,
    val mostUsedPackage: String?,
    val mostUsedName: String?,
    val lastUsedPackage: String?,
    val lastUsedName: String?,
    val usageItems: List<RowItem>
)
