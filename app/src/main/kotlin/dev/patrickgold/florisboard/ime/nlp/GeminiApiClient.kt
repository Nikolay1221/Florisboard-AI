package dev.patrickgold.florisboard.ime.nlp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import dev.patrickgold.florisboard.app.FlorisPreferenceStore

enum class AiStyle {
    FORMAL,
    FRIENDLY,
    GRAMMAR,
    FUNNY
}

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private val prefs by FlorisPreferenceStore
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"

    suspend fun rewriteText(text: String, prompt: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        return@withContext makeRequest(prompt + "\n\nИсходный текст: " + text, null, null)
    }

    suspend fun extractTextFromImage(base64Image: String, mimeType: String): String? = withContext(Dispatchers.IO) {
        val prompt = "Extract all text from this image and generate a suitable reply based on the conversation context. Reply ONLY with the final text."
        return@withContext makeRequest(prompt, base64Image, mimeType)
    }

    private fun makeRequest(prompt: String, base64Image: String?, mimeType: String?): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(ENDPOINT)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-goog-api-key", prefs.ai.apiKey.get())
            connection.doOutput = true

            // Build JSON body
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))

            if (base64Image != null && mimeType != null) {
                val inlineData = JSONObject()
                inlineData.put("mime_type", mimeType)
                inlineData.put("data", base64Image)
                partsArray.put(JSONObject().put("inline_data", inlineData))
            }

            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)

            val requestBody = JSONObject()
            requestBody.put("contents", contentsArray)

            // Write body
            val writer = OutputStreamWriter(connection.outputStream, Charsets.UTF_8)
            writer.write(requestBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                val responseString = reader.use { it.readText() }
                
                val responseJson = JSONObject(responseString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text != null && text.isNotBlank()) return text.trim()
                        return "ERROR: ИИ вернул пустой текст"
                    }
                    return "ERROR: Нет поля parts в ответе"
                }
                return "ERROR: Нет кандидатов в ответе"
            } else {
                val errorStream = connection.errorStream ?: connection.inputStream
                val errorReader = BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8))
                val errorString = errorReader.use { it.readText() }
                Log.e(TAG, "API Error: $responseCode - $errorString")
                return "ERROR: HTTP $responseCode - $errorString"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call", e)
            return "ERROR: Exception - ${e.message}"
        } finally {
            connection?.disconnect()
        }
    }
}
