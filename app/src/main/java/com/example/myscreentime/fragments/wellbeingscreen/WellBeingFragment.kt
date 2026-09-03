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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.example.myscreentime.R

class WellBeingFragment : Fragment() {

    private var classifier: ActivityClassifier? = null

    private var currentLabel = mutableStateOf("Waiting for data...")
    private var currentConfidence = mutableStateOf(0f)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wellbeing, container, false)

        val composeView = view.findViewById<ComposeView>(R.id.wellBeingComposeView)
        composeView.setContent {
            MaterialTheme {
                WellBeingScreen(currentLabel.value, currentConfidence.value)
            }
        }

        classifier = ActivityClassifier(requireContext()) { label, confidence ->
            requireActivity().runOnUiThread {
                currentLabel.value = label
                currentConfidence.value = confidence
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        classifier?.start()
    }

    override fun onPause() {
        super.onPause()
        classifier?.stop()
    }
}

@Composable
fun WellBeingScreen(label: String, confidence: Float) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Detected activity:")
        Text(text = label)
        Text(text = "Confidence: ${"%.2f".format(confidence * 100)}%")
    }
}