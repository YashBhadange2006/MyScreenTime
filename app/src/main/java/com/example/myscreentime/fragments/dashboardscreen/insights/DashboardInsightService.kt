package com.example.myscreentime.fragments.dashboardscreen.insights

import android.content.Context
import com.example.myscreentime.BuildConfig
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

class DashboardInsightService(
    private val context: Context,
    private val database: AppRoomDatabase
) {

    suspend fun getLatestInsight(): String {
        return withContext(Dispatchers.IO) {
            val dao = database.usageDao()
            val latestDate = dao.getLatestSavedDate()
                ?: return@withContext "Insights will appear after the first daily sync saves a full day of usage."

            val totalUsage = dao.getTotalUsageForDate(latestDate)
                ?: return@withContext "Insights will appear after the first daily sync saves a full day of usage."

            val appUsageRows = dao.getUsageRowsForDate(latestDate)
            if (appUsageRows.isEmpty() || totalUsage.totalCombinedTime <= 0L) {
                return@withContext "No saved usage summary is available yet for $latestDate."
            }

            val localFallback = buildLocalInsight(appUsageRows, totalUsage.totalCombinedTime)
            val apiKey = BuildConfig.GROQ_API_KEY.trim()

            if (apiKey.isEmpty()) {
                return@withContext "$localFallback\n\nAdd GROQ_API_KEY to local.properties to enable Groq insights."
            }

            fetchGroqInsight(
                apiKey = apiKey,
                latestDate = latestDate,
                totalUsageMs = totalUsage.totalCombinedTime,
                appUsageRows = appUsageRows,
                fallback = localFallback
            )
        }
    }

    private fun buildLocalInsight(appUsageRows: List<AppUsageEntity>, totalUsageMs: Long): String {
        val topApp = appUsageRows.first()
        val appName = resolveAppName(topApp.packageName)
        val percent = ((topApp.totalTimeInForeground * 100) / totalUsageMs).coerceAtMost(100L)
        val topMinutes = topApp.totalTimeInForeground / (1000 * 60)

        return "You spent about $percent% of your saved screen time on $appName. That was roughly $topMinutes minutes, so it may be the best app to watch first."
    }

    private fun fetchGroqInsight(
        apiKey: String,
        latestDate: String,
        totalUsageMs: Long,
        appUsageRows: List<AppUsageEntity>,
        fallback: String
    ): String {
        return try {
            val connection = URL("https://api.groq.com/openai/v1/chat/completions")
                .openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val topApps = appUsageRows.take(5).joinToString("\n") {
                "${resolveAppName(it.packageName)}: ${formatMinutes(it.totalTimeInForeground)}"
            }

            val prompt = """
                Date: $latestDate
                Total screen time: ${formatMinutes(totalUsageMs)}
                Top apps:
                $topApps

                Give one short helpful insight to reduce screen time in 2 sentences max. Be specific and practical.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("model", "llama-3.1-8b-instant")
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", prompt)
                    )
                )
                put("temperature", 0.4)
                put("max_completion_tokens", 120)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseText = BufferedReader(
                if (connection.responseCode in 200..299) {
                    connection.inputStream.reader()
                } else {
                    connection.errorStream?.reader() ?: connection.inputStream.reader()
                }
            ).use { it.readText() }

            val responseJson = JSONObject(responseText)
            responseJson.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { char ->
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
