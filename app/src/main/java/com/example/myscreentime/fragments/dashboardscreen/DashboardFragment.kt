package com.example.myscreentime.fragments.dashboardscreen

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.myscreentime.R
import com.example.myscreentime.fragments.dashboardscreen.insights.DashboardInsightService
import com.example.myscreentime.roomdb.AppRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by activityViewModels()

    private lateinit var insightBody: TextView
    private lateinit var insightService: DashboardInsightService
    private lateinit var tvPercentComp: TextView
    private lateinit var tvTotalScreenTime: TextView
    private lateinit var mostUsedIcon: ImageView
    private lateinit var mostUsedName: TextView
    private lateinit var lastUsedIcon: ImageView
    private lateinit var lastUsedName: TextView
    private lateinit var appList: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalScreenTime = view.findViewById(R.id.total_screen_textview)
        tvPercentComp = view.findViewById(R.id.percent_comp_textview)
        val mostUsedCard = view.findViewById<View>(R.id.most_used_app_card)
        mostUsedIcon = mostUsedCard.findViewById(R.id.iv_app_icon)
        val mostUsedTitle = mostUsedCard.findViewById<TextView>(R.id.text_above_app_name)
        mostUsedName = mostUsedCard.findViewById(R.id.tv_app_name)
        val lastUsedCard = view.findViewById<View>(R.id.last_used_app_card)
        lastUsedIcon = lastUsedCard.findViewById(R.id.iv_app_icon)
        val lastUsedTitle = lastUsedCard.findViewById<TextView>(R.id.text_above_app_name)
        lastUsedName = lastUsedCard.findViewById(R.id.tv_app_name)
        val insightCard = view.findViewById<View>(R.id.insight_card)
        insightBody = insightCard.findViewById(R.id.insight_body)
        appList = view.findViewById(R.id.app_list)
        
        appList.layoutManager = LinearLayoutManager(requireContext())
        appList.setHasFixedSize(true)
        appList.isNestedScrollingEnabled = false
        (appList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        mostUsedTitle.text = "Most Used App"
        lastUsedTitle.text = "Last Used App"

        insightService = DashboardInsightService(
            context = requireContext(),
            database = AppRoomDatabase.getInstance(requireContext())
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { data ->
                if (data == null) {
                    showSkeletonUi()
                } else {
                    bindData(data)
                }
            }
        }

        viewModel.loadDataIfNeeded()
    }

    private fun showSkeletonUi() {
        showSkeletonText(tvTotalScreenTime)
        showSkeletonText(mostUsedName)
        showSkeletonIcon(mostUsedIcon)
        showSkeletonText(lastUsedName)
        showSkeletonIcon(lastUsedIcon)
        appList.adapter = AppAdapter(emptyList())
        insightBody.text = "Insights will appear after the first daily sync stores a full day of usage."
    }

    private fun bindData(data: DashboardData) {
        tvTotalScreenTime.text = data.totalTime.let { formatTime(it) }
        tvTotalScreenTime.background = null
        
        tvPercentComp.text = data.percentText
        tvPercentComp.setTextColor(
            if (data.isMoreThanYesterday) ContextCompat.getColor(requireContext(), R.color.red_500)
            else ContextCompat.getColor(requireContext(), R.color.green_500)
        )

        mostUsedName.text = data.mostUsedName ?: "No app data"
        mostUsedName.background = null
        loadIconAsync(mostUsedIcon, data.mostUsedPackage)
        
        lastUsedName.text = data.lastUsedName ?: "No app data"
        lastUsedName.background = null
        loadIconAsync(lastUsedIcon, data.lastUsedPackage)
        
        appList.adapter = AppAdapter(data.usageItems)
    }

    private fun loadIconAsync(imageView: ImageView, packageName: String?) {
        imageView.setImageResource(R.drawable.ic_app_fallback)
        if (packageName == null) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            val icon = withContext(Dispatchers.IO) {
                try {
                    requireContext().packageManager.getApplicationIcon(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            if (icon != null) {
                imageView.setImageDrawable(icon)
                imageView.imageTintList = null
            }
        }
    }

    override fun onResume() {
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

    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }

    private fun showSkeletonText(textView: TextView) {
        textView.text = ""
        textView.minWidth = resources.getDimensionPixelSize(R.dimen.dashboard_skeleton_text_width)
        textView.minHeight = resources.getDimensionPixelSize(R.dimen.dashboard_skeleton_text_height)
        textView.background = ContextCompat.getDrawable(requireContext(), R.drawable.skeleton_bar)
    }

    private fun showSkeletonIcon(imageView: ImageView) {
        imageView.setImageDrawable(null)
        imageView.imageTintList = null
    }
}
