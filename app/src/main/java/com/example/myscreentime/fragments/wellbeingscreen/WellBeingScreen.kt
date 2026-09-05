package com.example.myscreentime.fragments.wellbeingscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreentime.ui.components.*

@Composable
fun WellBeingScreen(uiState: WellBeingUiState) {
    val state = uiState as? WellBeingUiState.Success ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FE))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        LiveActivityCard(state.liveActivity)
        
        Spacer(modifier = Modifier.height(24.dp))
        ActivityBalanceCard(state)
        
        Spacer(modifier = Modifier.height(24.dp))
        ActivityTrendsSection(state.weeklyActivity)
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun LiveActivityCard(activity: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Live Activity",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activity,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun ActivityBalanceCard(state: WellBeingUiState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Activity Balance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Today's multi-axis posture & movement breakdown",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            val radarDataList = listOf(
                RadarData("Walking", state.walkingMs.toFloat(), 0f),
                RadarData("W.Up", state.walkingUpstairsMs.toFloat(), 0f),
                RadarData("W.Down", state.walkingDownstairsMs.toFloat(), 0f),
                RadarData("Sitting", state.sittingMs.toFloat(), 0f),
                RadarData("Standing", state.standingMs.toFloat(), 0f),
                RadarData("Laying", state.layingMs.toFloat(), 0f)
            )
            val maxVal = radarDataList.maxOf { it.value }.coerceAtLeast(60000f)
            val finalRadarData = radarDataList.map { it.copy(maxValue = maxVal) }

            RadarChart(
                data = finalRadarData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(vertical = 16.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Today's activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Recorded totals", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TodayActivityGrid(state)
        }
    }
}

@Composable
fun TodayActivityGrid(state: WellBeingUiState.Success) {
    Column {
        ActivityRow("Walking", state.walkingMs, "Standing", state.standingMs)
        Spacer(modifier = Modifier.height(12.dp))
        ActivityRow("Walking upstairs", state.walkingUpstairsMs, "Walking downstairs", state.walkingDownstairsMs)
        Spacer(modifier = Modifier.height(12.dp))
        ActivityRow("Sitting", state.sittingMs, "Laying", state.layingMs)
    }
}

@Composable
fun ActivityRow(
    label1: String, ms1: Long,
    label2: String, ms2: Long
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ActivityItem(label = label1, ms = ms1, modifier = Modifier.weight(1f), valueColor = Color.Black)
        Spacer(modifier = Modifier.width(16.dp))
        ActivityItem(label = label2, ms = ms2, modifier = Modifier.weight(1f), valueColor = Color.Black)
    }
}

@Composable
fun ActivityItem(label: String, ms: Long, modifier: Modifier = Modifier, valueColor: Color = Color.Black) {
    Row(
        modifier = modifier
            .background(Color(0xFFF8F9FE), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(
            text = "${ms / 60_000} min",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun ActivityTrendsSection(weeklyActivity: List<WeeklyActivity>) {
    var selectedActivity by remember { mutableStateOf("Walking") }
    val activities = listOf("Walking", "Sitting", "Standing", "Laying")

    Column {
        Text(
            text = "Activity Trends",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Historical view of your activities (Last 7 days)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(activities) { activity ->
                FilterChip(
                    selected = selectedActivity == activity,
                    onClick = { selectedActivity = activity },
                    label = { Text(activity) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        val chartData = when (selectedActivity) {
            "Sitting" -> weeklyActivity.map { BarData(it.label, it.sittingMs / 60000f) }
            "Standing" -> weeklyActivity.map { BarData(it.label, it.standingMs / 60000f) }
            "Laying" -> weeklyActivity.map { BarData(it.label, it.layingMs / 60000f) }
            else -> weeklyActivity.map { BarData(it.label, it.walkingMs / 60000f) }
        }

        val gradientColors = when (selectedActivity) {
            "Sitting" -> listOf(Color(0xFF0288D1), Color(0xFF26C6DA))
            "Standing" -> listOf(Color(0xFFF57C00), Color(0xFFFFB74D))
            "Laying" -> listOf(Color(0xFF388E3C), Color(0xFF81C784))
            else -> listOf(Color(0xFF8E24AA), Color(0xFFE91E63))
        }

        ActivityBarChartCard(
            title = selectedActivity,
            data = chartData,
            gradientColors = gradientColors,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WellBeingScreenPreview() {
    val mockData = WellBeingUiState.Success(
        liveActivity = "Sitting",
        walkingMs = 5 * 60_000L,
        walkingUpstairsMs = 108 * 60_000L,
        walkingDownstairsMs = 425 * 60_000L,
        sittingMs = 412 * 60_000L,
        standingMs = 0 * 60_000L,
        layingMs = 187 * 60_000L,
        weeklyActivity = listOf(
            WeeklyActivity("Sun", walkingMs = 0, sittingMs = 0, standingMs = 0, layingMs = 0),
            WeeklyActivity("Mon", walkingMs = 0, sittingMs = 0, standingMs = 0, layingMs = 0),
            WeeklyActivity("Tue", walkingMs = 0, sittingMs = 0, standingMs = 0, layingMs = 0),
            WeeklyActivity("Wed", walkingMs = 0, sittingMs = 0, standingMs = 0, layingMs = 0),
            WeeklyActivity("Thu", walkingMs = 0, sittingMs = 52 * 60000, standingMs = 0, layingMs = 5 * 60000),
            WeeklyActivity("Fri", walkingMs = 43 * 60000, sittingMs = 409 * 60000, standingMs = 43 * 60000, layingMs = 145 * 60000),
            WeeklyActivity("Sat", walkingMs = 5 * 60000, sittingMs = 412 * 60000, standingMs = 0, layingMs = 187 * 60000)
        )
    )
    MaterialTheme {
        WellBeingScreen(uiState = mockData)
    }
}
