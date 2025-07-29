package com.example.snaper

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

class GeminiClient(private val apiKey: String) {
    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout){
            requestTimeoutMillis=300_000
            connectTimeoutMillis=60_000
            socketTimeoutMillis=300_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private val tag = "GeminiClient"


    private suspend fun sendBitmapWithText(bitmap: Bitmap, prompt: String): Result<String?> =
        withContext(Dispatchers.IO) {
            try {
                val base64Image = bitmap.toBase64Jpeg()
                val payload = GeminiRequest(
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0F
                    ),
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(text = prompt),
                                GeminiRequest.Part(
                                    inlineData = GeminiRequest.InlineData(
                                        mimeType = "image/jpeg",
                                        data = base64Image
                                    )
                                )
                            )
                        )
                    )
                )

                val response =
                    httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-05-20:generateContent?key=$apiKey") {
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                            append(HttpHeaders.Accept, ContentType.Application.Json)
                        }
                        setBody(payload)
                    }

                val rawText = response.bodyAsText()
                Log.d(tag, rawText)
                if (response.status != HttpStatusCode.OK) {
                    return@withContext Result.failure(Exception(response.status.description))
                }
                // парсим ответ
                val geminiResponse = response.body<GeminiResponse>()

                // извлекаем текст
                val text = extractTextsFromResponse(geminiResponse)
                Result.success(text)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    // Основная функция - вызывает sendBitmapWithText и парсит результат
    suspend fun checkSnapshot(bitmap: Bitmap, prompt: String): Result<List<SnapIssue>> {
        return runCatching {
            val responseResult = sendBitmapWithText(bitmap, prompt)
            val jsonString = responseResult.getOrThrow() ?: throw Exception("Пустой ответ")
            // парсинг строки JSON в список
            Json.decodeFromString<List<SnapIssue>>(jsonString)
        }
    }

    private fun extractTextsFromResponse(response: GeminiResponse): String? =
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

}

private fun Bitmap.toBase64Jpeg(): String {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}


@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig
) {
    @Serializable
    data class GenerationConfig(
        val responseMimeType: String,
        val temperature:Float
    )

    @Serializable
    data class Content(
        val parts: List<Part>
    )

    @Serializable
    data class Part(
        val text: String? = null,
        @SerialName("inline_data")
        val inlineData: InlineData? = null
    )

    @Serializable
    data class InlineData(
        @SerialName("mime_type")
        val mimeType: String,
        val data: String
    )
}

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
) {
    @Serializable
    data class Candidate(
        val content: Content? = null
    ) {
        @Serializable
        data class Content(
            val parts: List<TextPart> = emptyList()
        )
    }

    @Serializable
    data class TextPart(val text: String = "")
}


