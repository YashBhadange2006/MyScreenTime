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
import androidx.fragment.app.viewModels
import com.example.myscreentime.R

class WellBeingFragment : Fragment() {

    private val viewModel: WellBeingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wellbeing, container, false)

        val composeView = view.findViewById<ComposeView>(R.id.wellBeingComposeView)
        composeView.setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsState()
                WellBeingScreen(uiState)
            }
        }

        return view
    }

}

@Composable
fun WellBeingScreen(uiState: WellBeingUiState) {
    val state = uiState as? WellBeingUiState.Success ?: return

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Live activity: ${state.liveActivity}")
        Text(text = "Today's activity")
        Text(text = "Walking: ${formatDuration(state.walkingMs)}")
        Text(text = "Walking upstairs: ${formatDuration(state.walkingUpstairsMs)}")
        Text(text = "Walking downstairs: ${formatDuration(state.walkingDownstairsMs)}")
        Text(text = "Sitting: ${formatDuration(state.sittingMs)}")
        Text(text = "Standing: ${formatDuration(state.standingMs)}")
        Text(text = "Laying: ${formatDuration(state.layingMs)}")
    }
}

private fun formatDuration(durationMs: Long): String =
    "${durationMs / 60_000} min"
