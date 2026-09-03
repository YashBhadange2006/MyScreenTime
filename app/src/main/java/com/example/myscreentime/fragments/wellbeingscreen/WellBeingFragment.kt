package com.example.myscreentime.fragments.wellbeingscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.example.myscreentime.roomdb.AppRoomDatabase
import com.example.myscreentime.roomdb.ActivityDataEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.myscreentime.R

class WellBeingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wellbeing, container, false)

        val composeView = view.findViewById<ComposeView>(R.id.wellBeingComposeView)
        composeView.setContent {
            MaterialTheme {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val liveActivity by ActivityClassificationService.latestActivity.collectAsState()
                val activityData by AppRoomDatabase.getInstance(requireContext()).usageDao()
                    .observeActivityDataForDate(date)
                    .collectAsState(initial = null)
                WellBeingScreen(liveActivity, activityData)
            }
        }

        return view
    }

}

@Composable
fun WellBeingScreen(liveActivity: String, activityData: ActivityDataEntity?) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Live activity: $liveActivity")
        Text(text = "Today's activity")
        if (activityData == null) {
            Text(text = "Collecting activity data…")
        } else {
            Text(text = "Walking: ${formatDuration(activityData.walkingMs)}")
            Text(text = "Walking upstairs: ${formatDuration(activityData.walkingUpstairsMs)}")
            Text(text = "Walking downstairs: ${formatDuration(activityData.walkingDownstairsMs)}")
            Text(text = "Sitting: ${formatDuration(activityData.sittingMs)}")
            Text(text = "Standing: ${formatDuration(activityData.standingMs)}")
            Text(text = "Laying: ${formatDuration(activityData.layingMs)}")
        }
    }
}

private fun formatDuration(durationMs: Long): String =
    "${durationMs / 60_000} min"
