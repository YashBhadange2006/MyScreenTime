package com.example.myscreentime.fragments.wellbeingscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
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
