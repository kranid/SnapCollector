package com.example.snapcollector

import android.content.res.Resources.NotFoundException
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.encodeToString

class SnapHubClient {
    private val TAG: String = "snapcollector"
    private val rootUrl = "http://212.34.131.52:8080"
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json{encodeDefaults =false})
        }
    }

    suspend fun saveSnap(snap: List<SnapNode>, screenInfo: ScreenInfo): HttpResponse = withContext(Dispatchers.IO)  {
        val url = "$rootUrl/snaphub/add"
        return@withContext client.post(url) {
            contentType(ContentType.Application.Json)
            ContentType
            setBody(snap)
            headers {
                append("name", screenInfo.Name)
                append("packagename", screenInfo.PackageName)
            }
        }
    }

    suspend fun saveReport(report: List<SnapIssue>, screenInfo: ScreenInfo): HttpResponse =
        withContext(Dispatchers.IO) {
        val url = "$rootUrl/snaphub/add"
       return@withContext  client.post(url) {
            contentType(ContentType.Application.Json)
            ContentType
            setBody(report)
            headers {
                append("name", "report_${screenInfo.Name}")
                append("packagename", screenInfo.PackageName)
            }
        }
    }


    suspend fun getSnap(name: String): List<SnapNode> {

        val url = "$rootUrl/snaphub/get/$name"
        Log.i(TAG, "url: $url")
        val resp = client.get(url) {
            contentType(ContentType.Application.Json)
        }

        if (resp.status != HttpStatusCode.OK) {
            if (resp.status == HttpStatusCode.NotFound) {
                throw NotFoundException("SnapShot  is not found")
            }
            throw Exception("Error ${resp.status}")
        }
        Log.i(TAG, "response: ${resp.body<List<SnapNode>>()} ")
        return resp.body<List<SnapNode>>()
    }

        suspend fun saveData(
        screenshot: ByteArray,
        originalSnapNodes: List<SnapNode>,
        editedSnapNodes: List<SnapNode>,
        technicalChanges: List<SnapChange>,
        humanReadableIssues: List<SnapIssue>,
        packageName: String,
        activityName: String,
        deviceModel: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "saveData called for package: $packageName, activity: $activityName")
        val url = "$rootUrl/snapshots/add"

        val response = client.post(url) {
            setBody(MultiPartFormDataContent(formData {
                append("package_name", packageName)
                append("activity_name", activityName)
                append("device_model", deviceModel)

                append("original_snapshot", Json.encodeToString(originalSnapNodes).toByteArray(), Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.ContentDisposition, "filename=\"original.json\"")
                })
                append("expected_snapshot", Json.encodeToString(editedSnapNodes).toByteArray(), Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.ContentDisposition, "filename=\"expected.json\"")
                })
                append("tech_report", Json.encodeToString(technicalChanges).toByteArray(), Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.ContentDisposition, "filename=\"technical_report.json\"")
                })
                append("human_report", Json.encodeToString(humanReadableIssues).toByteArray(), Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.ContentDisposition, "filename=\"human_report.json\"")
                })
                append("screenshot", screenshot, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"screenshot.jpg\"")
                })
            }))
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Server error: ${response.status.value} ${response.status.description}")
        }
    }

}
