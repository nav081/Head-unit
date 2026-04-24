package com.example.blegps.debug

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object DebugRuntimeLogger {
    private const val ENDPOINT = "http://127.0.0.1:7758/ingest/13de0381-c918-4d21-bcc5-b9c221ebfeaa"
    private const val SESSION_ID = "efe250"

    fun log(runId: String, hypothesisId: String, location: String, message: String, data: String) {
        val payload = """{"sessionId":"$SESSION_ID","runId":"$runId","hypothesisId":"$hypothesisId","location":"$location","message":"${esc(message)}","data":$data,"timestamp":${System.currentTimeMillis()}}"""
        Thread {
            runCatching {
                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Debug-Session-Id", SESSION_ID)
                    connectTimeout = 800
                    readTimeout = 800
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                conn.responseCode
                conn.disconnect()
            }
        }.start()
    }

    private fun esc(input: String): String = input.replace("\\", "\\\\").replace("\"", "\\\"")
}
