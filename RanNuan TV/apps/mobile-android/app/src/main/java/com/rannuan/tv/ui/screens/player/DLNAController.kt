package com.rannuan.tv.ui.screens.player

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

// ── 数据模型 ──

/** 发现的 DLNA 设备 */
data class DLNADevice(
    val uuid: String,
    val friendlyName: String,
    val manufacturer: String = "",
    val location: String,          // 设备描述 XML 地址
    val controlUrl: String = "",   // AVTransport 控制端点
    val eventUrl: String = ""
) {
    val displayName: String get() = friendlyName.ifBlank { manufacturer.ifBlank { "未知设备" } }
}

/** DLNA 投屏状态 */
enum class DLNACastState {
    IDLE,           // 未投屏
    CONNECTING,     // 连接设备中
    PLAYING,        // 投屏播放中
    PAUSED,         // 投屏已暂停
    STOPPED,        // 投屏已停止
    ERROR           // 出错
}

/** 当前投屏会话 */
data class DLNACastSession(
    val device: DLNADevice,
    var state: DLNACastState = DLNACastState.CONNECTING,
    var position: Long = 0L,
    var duration: Long = 0L
)

// ── DLNA 控制器 ──

/**
 * 轻量级 DLNA 投屏控制器。
 *
 * 协议说明：
 * - SSDP (Simple Service Discovery Protocol): 通过 UDP 多播发现局域网内 DLNA 设备
 * - AVTransport: UPnP 服务，控制播放器加载/播放/暂停/停止/Seek
 * - 使用 OkHttp 发 SOAP 请求，不引入额外 DLNA 库
 *
 * 国内主流电视品牌（小米/海信/TCL/创维/长虹/索尼/三星/LG）均支持 DLNA/UPnP。
 */
class DLNAController {

    companion object {
        private const val TAG = "DLNAController"
        private const val SSDP_MULTICAST_HOST = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SSDP_TIMEOUT_MS = 4000L
        private const val HTTP_TIMEOUT_S = 8L

        // AVTransport SOAP 模板
        private const val SOAP_ENV =
            """<?xml version="1.0" encoding="utf-8" standalone="yes"?>
<s:Envelope s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
 xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
  <s:Body>%s</s:Body>
</s:Envelope>"""

        private const val AVT_NS = "urn:schemas-upnp-org:service:AVTransport:1"
        private const val RC_NS  = "urn:schemas-upnp-org:service:RenderingControl:1"

        private val soapActionHeaders = mapOf(
            "SetAVTransportURI"  to "\"$AVT_NS#SetAVTransportURI\"",
            "Play"               to "\"$AVT_NS#Play\"",
            "Pause"              to "\"$AVT_NS#Pause\"",
            "Stop"               to "\"$AVT_NS#Stop\"",
            "Seek"               to "\"$AVT_NS#Seek\"",
            "GetPositionInfo"    to "\"$AVT_NS#GetPositionInfo\"",
            "GetTransportInfo"   to "\"$AVT_NS#GetTransportInfo\"",
            "SetVolume"          to "\"$RC_NS#SetVolume\"",
            "GetVolume"          to "\"$RC_NS#GetVolume\""
        )
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // ── 设备发现 ──

    /**
     * 通过 SSDP 多播发现局域网内 DLNA 设备（Android TV / 智能电视等）。
     * 超时约 4 秒后返回已发现的设备列表。
     */
    suspend fun discoverDevices(): List<DLNADevice> = withContext(Dispatchers.IO) {
        val rawDevices = mutableListOf<DLNADevice>()
        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                soTimeout = SSDP_TIMEOUT_MS.toInt()
                bind(InetSocketAddress(0))
            }

            // 构造 M-SEARCH 请求
            val mSearch = buildString {
                append("M-SEARCH * HTTP/1.1\r\n")
                append("HOST: $SSDP_MULTICAST_HOST:$SSDP_PORT\r\n")
                append("MAN: \"ssdp:discover\"\r\n")
                append("MX: 3\r\n")
                append("ST: $AVT_NS\r\n")         // 只搜 AVTransport 服务
                append("USER-AGENT: RanNuan-TV-Android/1.0\r\n")
                append("\r\n")
            }
            val requestBytes = mSearch.toByteArray(Charsets.UTF_8)
            val group = InetAddress.getByName(SSDP_MULTICAST_HOST)
            val packet = DatagramPacket(requestBytes, requestBytes.size, InetSocketAddress(group, SSDP_PORT))
            socket.send(packet)

            // 接收响应（循环直到超时）
            val buffer = ByteArray(4096)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < SSDP_TIMEOUT_MS) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    val response = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                    parseSSDPResponse(response)?.let { rawDevices.add(it) }
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSDP 发现失败: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }

        Log.d(TAG, "SSDP 发现 ${rawDevices.size} 个设备，正在获取详情...")

        // 并行获取设备描述 XML，解析 friendlyName 和 AVTransport controlURL
        val devices = rawDevices.map { dev ->
            coroutineScope { async { fetchDeviceDetails(dev) } }
        }.awaitAll().filterNotNull()

        Log.d(TAG, "最终可用 DLNA 设备: ${devices.size} 个")
        devices
    }

    private fun parseSSDPResponse(response: String): DLNADevice? {
        val headers = response.lines().associate { line ->
            val colon = line.indexOf(':')
            if (colon > 0) line.substring(0, colon).trim().uppercase() to line.substring(colon + 1).trim()
            else "" to ""
        }
        val location = headers["LOCATION"] ?: return null
        val usn = headers["USN"] ?: ""
        val uuid = usn.removePrefix("uuid:").substringBefore("::")
        return DLNADevice(uuid = uuid, friendlyName = "", location = location)
    }

    private suspend fun fetchDeviceDetails(device: DLNADevice): DLNADevice? {
        return try {
            val request = Request.Builder().url(device.location).get().build()
            val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
            val xml = response.body?.string() ?: return null
            parseDeviceDescription(xml, device)
        } catch (e: Exception) {
            Log.w(TAG, "获取设备描述失败 ${device.location}: ${e.message}")
            null
        }
    }

    private fun parseDeviceDescription(xml: String, device: DLNADevice): DLNADevice? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var friendlyName = device.friendlyName
            var manufacturer = device.manufacturer
            var controlUrl = device.controlUrl
            var eventUrl = device.eventUrl
            var inAvTransport = false
            var tagName = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        tagName = parser.name
                        when (tagName) {
                            "friendlyName" -> friendlyName = parser.nextText().trim()
                            "manufacturer" -> manufacturer = parser.nextText().trim()
                            "serviceType" -> {
                                val st = parser.nextText().trim()
                                inAvTransport = st.contains("AVTransport")
                            }
                            "controlURL" -> if (inAvTransport) controlUrl = parser.nextText().trim()
                            "eventSubURL" -> if (inAvTransport) eventUrl = parser.nextText().trim()
                        }
                    }
                }
                eventType = parser.next()
            }

            if (controlUrl.isBlank()) return null

            // 补全相对路径
            val baseUrl = device.location.substringBeforeLast("/")
            if (!controlUrl.startsWith("http")) {
                controlUrl = if (controlUrl.startsWith("/")) "$baseUrl$controlUrl" else "$baseUrl/$controlUrl"
            }
            if (eventUrl.isNotBlank() && !eventUrl.startsWith("http")) {
                eventUrl = if (eventUrl.startsWith("/")) "$baseUrl$eventUrl" else "$baseUrl/$eventUrl"
            }

            return device.copy(
                friendlyName = friendlyName,
                manufacturer = manufacturer,
                controlUrl = controlUrl,
                eventUrl = eventUrl
            )
        } catch (e: Exception) {
            Log.w(TAG, "解析设备描述 XML 失败: ${e.message}")
            return null
        }
    }

    // ── AVTransport 控制 ──

    /** 设置播放 URI：将视频地址推送到电视 */
    suspend fun setAVTransportURI(
        device: DLNADevice,
        videoUrl: String,
        title: String = "",
        metadataXml: String = ""
    ): Boolean {
        val body = SOAP_ENV.format(
            """<u:SetAVTransportURI xmlns:u="$AVT_NS">
  <InstanceID>0</InstanceID>
  <CurrentURI>${videoUrl.escapeXml()}</CurrentURI>
  <CurrentURIMetaData>${metadataXml.escapeXml()}</CurrentURIMetaData>
</u:SetAVTransportURI>"""
        )
        return soapCall(device.controlUrl, "SetAVTransportURI", body)
    }

    /** 播放 */
    suspend fun play(device: DLNADevice): Boolean {
        val body = SOAP_ENV.format(
            """<u:Play xmlns:u="$AVT_NS">
  <InstanceID>0</InstanceID>
  <Speed>1</Speed>
</u:Play>"""
        )
        return soapCall(device.controlUrl, "Play", body)
    }

    /** 暂停 */
    suspend fun pause(device: DLNADevice): Boolean {
        val body = SOAP_ENV.format(
            """<u:Pause xmlns:u="$AVT_NS">
  <InstanceID>0</InstanceID>
</u:Pause>"""
        )
        return soapCall(device.controlUrl, "Pause", body)
    }

    /** 停止 */
    suspend fun stop(device: DLNADevice): Boolean {
        val body = SOAP_ENV.format(
            """<u:Stop xmlns:u="$AVT_NS">
  <InstanceID>0</InstanceID>
</u:Stop>"""
        )
        return soapCall(device.controlUrl, "Stop", body)
    }

    /** 跳转到指定位置（秒） */
    suspend fun seek(device: DLNADevice, positionSeconds: Long): Boolean {
        val timeStr = formatDlnaTime(positionSeconds)
        val body = SOAP_ENV.format(
            """<u:Seek xmlns:u="$AVT_NS">
  <InstanceID>0</InstanceID>
  <Unit>REL_TIME</Unit>
  <Target>$timeStr</Target>
</u:Seek>"""
        )
        return soapCall(device.controlUrl, "Seek", body)
    }

    /** 获取播放位置信息，返回 (positionSeconds, durationSeconds) */
    suspend fun getPositionInfo(device: DLNADevice): Pair<Long, Long>? {
        val body = SOAP_ENV.format(
            """<u:GetPositionInfo xmlns:u="$AVT_NS">
  <InstanceID>0</InstanceID>
</u:GetPositionInfo>"""
        )
        val response = soapCallRaw(device.controlUrl, "GetPositionInfo", body) ?: return null
        return parsePositionInfo(response)
    }

    /** 获取传输状态 */
    suspend fun getTransportState(device: DLNADevice): String? {
        val body = SOAP_ENV.format(
            """<u:GetTransportInfo xmlns:u="$AVT_NS">
  <InstanceID>0</InstanceID>
</u:GetTransportInfo>"""
        )
        val response = soapCallRaw(device.controlUrl, "GetTransportInfo", body) ?: return null
        return response.substringAfter("<CurrentTransportState>")
            .substringBefore("</CurrentTransportState>")
            .ifBlank { null }
    }

    /** 设置音量 (0-100) */
    suspend fun setVolume(device: DLNADevice, volume: Int): Boolean {
        val body = SOAP_ENV.format(
            """<u:SetVolume xmlns:u="$RC_NS">
  <InstanceID>0</InstanceID>
  <Channel>Master</Channel>
  <DesiredVolume>$volume</DesiredVolume>
</u:SetVolume>"""
        )
        // RenderingControl 控制 URL 通常和 AVTransport 在同一 base
        val rcUrl = device.controlUrl.replace("AVTransport", "RenderingControl")
        return soapCall(rcUrl, "SetVolume", body)
    }

    // ── 内部 SOAP 通信 ──

    private suspend fun soapCall(
        controlUrl: String,
        action: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val soapAction = soapActionHeaders[action] ?: "\"$AVT_NS#$action\""
            val request = Request.Builder()
                .url(controlUrl)
                .header("SOAPACTION", soapAction)
                .header("Content-Type", "text/xml; charset=utf-8")
                .post(body.toRequestBody("text/xml".toMediaType()))
                .build()
            val response = httpClient.newCall(request).execute()
            response.close()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "SOAP $action 失败: ${e.message}")
            false
        }
    }

    private suspend fun soapCallRaw(
        controlUrl: String,
        action: String,
        body: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val soapAction = soapActionHeaders[action] ?: "\"$AVT_NS#$action\""
            val request = Request.Builder()
                .url(controlUrl)
                .header("SOAPACTION", soapAction)
                .header("Content-Type", "text/xml; charset=utf-8")
                .post(body.toRequestBody("text/xml".toMediaType()))
                .build()
            val response = httpClient.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            Log.e(TAG, "SOAP $action (raw) 失败: ${e.message}")
            null
        }
    }

    private fun parsePositionInfo(xml: String): Pair<Long, Long>? {
        val relTime = xml.substringAfter("<RelTime>").substringBefore("</RelTime>")
        val trackDuration = xml.substringAfter("<TrackDuration>").substringBefore("</TrackDuration>")
        return try {
            parseDlnaTime(relTime) to parseDlnaTime(trackDuration)
        } catch (_: Exception) { null }
    }

    // ── 工具函数 ──

    private fun formatDlnaTime(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun parseDlnaTime(time: String): Long {
        val parts = time.split(":")
        return if (parts.size == 3) {
            parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
        } else 0L
    }

    private fun String.escapeXml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
