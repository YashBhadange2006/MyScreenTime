package com.example.myscreentime.fragments.dashboardscreen.insights

import android.content.Context
import android.util.Log
import com.example.myscreentime.BuildConfig
import com.example.myscreentime.fragments.permissionscreen.getSortedUsedApps
import com.example.myscreentime.fragments.permissionscreen.getTodayScreenTime
import com.example.myscreentime.roomdb.ActivityDataEntity
import com.example.myscreentime.roomdb.AppRoomDatabase
import com.example.myscreentime.roomdb.AppUsageEntity
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardInsightService(
    private val context: Context,
    private val database: AppRoomDatabase
) {

    suspend fun getLatestInsight(): String {
        return withContext(Dispatchers.IO) {
            val dao = database.usageDao()
            
            // Prioritize Today's real-time data to match the UI and current context
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val totalUsageMs = getTodayScreenTime(context)
            val appUsageStats = getSortedUsedApps(context)
            
            // Map UsageStats AppUsageEntry to Room AppUsageEntity for logic compatibility
            val appUsageRows = appUsageStats.map { 
                AppUsageEntity(it.packageName, todayDate, it.totalTimeInForeground, it.lastTimeUsed)
            }
            
            val activityData = dao.getActivityDataForDate(todayDate)

            if (appUsageRows.isEmpty() || totalUsageMs <= 0L) {
                return@withContext "Insights will appear after you use some apps today."
            }

            val localFallback = buildLocalInsight(appUsageRows, totalUsageMs, activityData)
            val apiKey = BuildConfig.GROQ_API_KEY.trim()

            if (apiKey.isEmpty()) {
                return@withContext "$localFallback\n\nAdd GROQ_API_KEY to local.properties to enable Groq insights."
            }


            fetchGroqInsight(
                apiKey = apiKey,
                latestDate = todayDate,
                totalUsageMs = totalUsageMs,
                appUsageRows = appUsageRows,
                activityData = activityData,
                fallback = localFallback
            )
        }
    }

    private fun buildLocalInsight(
        appUsageRows: List<AppUsageEntity>,
        totalUsageMs: Long,
        activityData: ActivityDataEntity?
    ): String {
        val topApp = appUsageRows.first()
        val appName = resolveAppName(topApp.packageName)
        val percent = ((topApp.totalTimeInForeground * 100) / totalUsageMs).coerceAtMost(100L)
        val topMinutes = topApp.totalTimeInForeground / (1000 * 60)

        var insight = "You spent about $percent% of your saved screen time on $appName (${topMinutes}m)."
        
        activityData?.let {
            val totalWalkingMs = it.walkingMs + it.walkingUpstairsMs + it.walkingDownstairsMs
            val walkingMins = totalWalkingMs / (1000 * 60)
            Log.d("GroqInsight", "Total Walking Ms: $totalWalkingMs, Mins: $walkingMins")
            if (walkingMins > 0) {
                insight += " You also walked for $walkingMins minutes today. Balance is key!"
            }
        }
        
        return insight
    }

    private fun fetchGroqInsight(
        apiKey: String,
        latestDate: String,
        totalUsageMs: Long,
        appUsageRows: List<AppUsageEntity>,
        activityData: ActivityDataEntity?,
        fallback: String
    ): String {
        val TAG = "GroqInsight"
        return try {
            val connection = URL("https://api.groq.com/openai/v1/chat/completions")
                .openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.doOutput = true

            val topApps = appUsageRows.take(5).joinToString(", ") {
                "${resolveAppName(it.packageName)} (${formatMinutes(it.totalTimeInForeground)})"
            }

            val activityContext = if (activityData != null) {
                """
                Physical Activity:
                - Walking: ${formatMinutes(activityData.walkingMs + activityData.walkingUpstairsMs + activityData.walkingDownstairsMs)}
                - Static Activity (Sitting/Standing/Laying): ${formatMinutes(activityData.sittingMs + activityData.standingMs + activityData.layingMs)}
                """.trimIndent()
            } else {
                "Physical Activity: Data not available."
            }

            val requestBody = JSONObject().apply {
                put("model", "openai/gpt-oss-20b")
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", """
                                You are a Senior Wellness Architect. Analyze digital habits vs. physical movement.
                                You must respond ONLY with a JSON object in this format:
                                {
                                  "observation": "A smart, data-driven observation about their day.",
                                  "action": "One high-impact, specific wellness tip."
                                }
                                Keep it professional, encouraging, and concise.
                            """.trimIndent())
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", """
                                Data for $latestDate:
                                - Total Screen: ${formatMinutes(totalUsageMs)}
                                - Top Apps: $topApps
                                $activityContext
                            """.trimIndent())
                        })
                    }
                )
                put("temperature", 0.6)
                put("max_tokens", 250)
            }

            Log.d(TAG, "Requesting advanced insight...")
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = connection.responseCode
            val responseText = BufferedReader(
                if (responseCode in 200..299) {
                    connection.inputStream.reader()
                } else {
                    connection.errorStream?.reader() ?: connection.inputStream.reader()
                }
            ).use { it.readText() }

            if (responseCode !in 200..299) {
                Log.e(TAG, "API Error ($responseCode): $responseText")
                return fallback
            }

            val responseJson = JSONObject(responseText)
            val content = responseJson.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()

            if (content.isNullOrEmpty()) return fallback

            // Parse the JSON output from AI
            try {
                // The AI might sometimes wrap JSON in code blocks, strip them if present
                val cleanedContent = content.removePrefix("```json").removeSuffix("```").trim()
                val json = JSONObject(cleanedContent)
                val obs = json.getString("observation")
                val act = json.getString("action")
                
                "<b>Observation:</b> $obs<br><br><b>Pro-Tip:</b> $act"
            } catch (e: Exception) {
                Log.e(TAG, "JSON Parse Error: $content", e)
                content // Return raw if JSON parsing fails
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch Error", e)
            fallback
        }
    }

    private fun resolveAppName(packageName: String): String {
        // Strip process suffix (e.g., :remote)
        val cleanPackageName = packageName.substringBefore(':')
        
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(cleanPackageName, 0)).toString()
        } catch (_: Exception) {
            val parts = cleanPackageName.split('.')
            val candidate = parts.lastOrNull { it !in setOf("android", "google", "apps", "main", "core") }
                ?: parts.lastOrNull()
                ?: cleanPackageName
            candidate.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }

    private fun formatMinutes(milliseconds: Long): String {
        val totalMinutes = milliseconds / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
