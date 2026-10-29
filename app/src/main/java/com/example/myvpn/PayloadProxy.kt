package com.example.myvpn

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

class PayloadProxy(
    private val localPort: Int,
    private val wsUrl: String,
    private val payloadText: String,
    private val sniHost: String
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        isRunning = true
        Thread {
            try {
                serverSocket = ServerSocket(localPort)
                Log.d("PayloadProxy", "Local Proxy started on port \$localPort")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                Log.e("PayloadProxy", "Proxy server error", e)
            }
        }.start()
    }

    private fun handleClient(clientSocket: Socket) {
        Thread {
            try {
                val formattedPayload = payloadText
                    .replace("[crlf]", "
")
                    .replace("[lf]", "
")
                    .replace("[cr]", "")

                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build()

                val requestBuilder = Request.Builder().url(wsUrl)
                if (sniHost.isNotEmpty()) {
                    requestBuilder.addHeader("Host", sniHost)
                }
                
                // Attach custom Payload headers
                formattedPayload.lines().forEach { line ->
                    if (line.contains(":")) {
                        val parts = line.split(":", limit = 2)
                        requestBuilder.addHeader(parts[0].trim(), parts[1].trim())
                    }
                }

                val request = requestBuilder.build()
                val inputStream = clientSocket.getInputStream()
                val outputStream = clientSocket.getOutputStream()

                val webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        try {
                            outputStream.write(bytes.toByteArray())
                            outputStream.flush()
                        } catch (e: Exception) {
                            Log.e("PayloadProxy", "Error writing to socket", e)
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e("PayloadProxy", "WebSocket failure", t)
                        clientSocket.close()
                    }
                })

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    webSocket.send(ByteString.of(buffer, 0, bytesRead))
                }

            } catch (e: Exception) {
                Log.e("PayloadProxy", "Client handler error", e)
            } finally {
                clientSocket.close()
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("PayloadProxy", "Error stopping proxy", e)
        }
    }
}