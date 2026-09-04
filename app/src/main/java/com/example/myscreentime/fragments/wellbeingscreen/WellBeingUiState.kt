package com.example.myscreentime.fragments.wellbeingscreen

sealed interface WellBeingUiState {
    data class Success(
        val liveActivity: String,
        val walkingMs: Long,
        val walkingUpstairsMs: Long,
        val walkingDownstairsMs: Long,
        val sittingMs: Long,
        val standingMs: Long,
        val layingMs: Long,
        val weeklyActivity: List<WeeklyActivity>
    ) : WellBeingUiState
}

data class WeeklyActivity(
    val label: String,
    val walkingMs: Long = 0,
    val walkingUpstairsMs: Long = 0,
    val walkingDownstairsMs: Long = 0,
    val sittingMs: Long = 0,
    val standingMs: Long = 0,
    val layingMs: Long = 0
)
