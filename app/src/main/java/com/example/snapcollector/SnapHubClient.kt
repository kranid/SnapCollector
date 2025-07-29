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

class SnapHubClient {
    private val TAG: String = "snapcollector"
    private val rootUrl = "http://212.34.131.52:8080/snaphub"
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json{encodeDefaults =false})
        }
    }

    suspend fun saveSnap(snap: List<SnapNode>, screenInfo: ScreenInfo): HttpResponse = withContext(Dispatchers.IO)  {
        val url = "$rootUrl/add"
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
        val url = "$rootUrl/add"
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

        val url = "$rootUrl/get/$name"
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

}