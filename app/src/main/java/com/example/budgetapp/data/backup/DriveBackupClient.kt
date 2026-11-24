package com.example.budgetapp.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

/** Drive answered with an HTTP error. A 401 means the token is stale. */
class DriveHttpException(val code: Int, message: String) : IOException("Drive HTTP $code: $message")

/** The backup file as Drive sees it. */
data class RemoteBackup(val fileId: String, val modifiedAt: Long)

/**
 * Talks to the Drive v3 REST API directly. The app needs only four calls, which keeps
 * the large google-api-client library out. Everything lives in the hidden
 * `appDataFolder`, in a single file.
 */
class DriveBackupClient {

    suspend fun find(token: String): RemoteBackup? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val body = request(
            token, "GET",
            "$API/files?spaces=appDataFolder&q=$q&fields=files(id,modifiedTime)&orderBy=modifiedTime%20desc",
        )
        val files = json.parseToJsonElement(body).jsonObject["files"]?.jsonArray.orEmpty()
        files.firstOrNull()?.jsonObject?.toRemote()
    }

    suspend fun download(token: String, fileId: String): String = withContext(Dispatchers.IO) {
        request(token, "GET", "$API/files/$fileId?alt=media")
    }

    /** Creates the backup file, or overwrites the existing one. */
    suspend fun upload(token: String, contents: String, existing: RemoteBackup?): RemoteBackup =
        withContext(Dispatchers.IO) {
            val body = if (existing == null) {
                val boundary = "budget-backup-boundary"
                val multipart = buildString {
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append("""{"name":"$FILE_NAME","parents":["appDataFolder"]}""")
                    append("\r\n--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(contents)
                    append("\r\n--$boundary--\r\n")
                }
                request(
                    token, "POST",
                    "$UPLOAD/files?uploadType=multipart&fields=id,modifiedTime",
                    payload = multipart,
                    contentType = "multipart/related; boundary=$boundary",
                )
            } else {
                // HttpURLConnection has no PATCH; Google APIs honour the override header.
                request(
                    token, "POST",
                    "$UPLOAD/files/${existing.fileId}?uploadType=media&fields=id,modifiedTime",
                    payload = contents,
                    contentType = "application/json; charset=UTF-8",
                    methodOverride = "PATCH",
                )
            }
            json.parseToJsonElement(body).jsonObject.toRemote()
        }

    private fun JsonObject.toRemote() = RemoteBackup(
        fileId = getValue("id").jsonPrimitive.content,
        modifiedAt = this["modifiedTime"]?.jsonPrimitive?.content
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: System.currentTimeMillis(),
    )

    private fun request(
        token: String,
        method: String,
        url: String,
        payload: String? = null,
        contentType: String? = null,
        methodOverride: String? = null,
    ): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            methodOverride?.let { conn.setRequestProperty("X-HTTP-Method-Override", it) }
            if (payload != null) {
                conn.doOutput = true
                contentType?.let { conn.setRequestProperty("Content-Type", it) }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw DriveHttpException(code, err.take(300))
            }
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val FILE_NAME = "budget-backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
