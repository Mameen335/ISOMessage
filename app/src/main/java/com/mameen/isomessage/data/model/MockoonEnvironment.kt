package com.mameen.isomessage.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for parsing the Mockoon environment JSON file.
 * File: app/src/main/assets/ISO8583_POS_Payment_Host.json
 *
 * Mockoon is an open-source API mock tool. Its environment JSON defines:
 * - Server configuration (port, hostname, latency)
 * - Route definitions (endpoints, methods, responses)
 * - Response rules (conditions to select different responses)
 *
 * Parsing this file gives the Developer Tools screen a live view of
 * which endpoints the app communicates with and what to expect.
 */
data class MockoonEnvironment(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("port") val port: Int = 5000,
    @SerializedName("hostname") val hostname: String = "0.0.0.0",
    @SerializedName("latency") val latency: Int = 0,
    @SerializedName("endpointPrefix") val endpointPrefix: String = "",
    @SerializedName("headers") val headers: List<MockoonHeader> = emptyList(),
    @SerializedName("routes") val routes: List<MockoonRoute> = emptyList(),
    @SerializedName("proxyMode") val proxyMode: Boolean = false,
    @SerializedName("tlsOptions") val tlsOptions: TlsOptions? = null
) {
    val fullBaseUrl: String get() = "http://$hostname:$port/"
}

data class MockoonHeader(
    @SerializedName("key") val key: String,
    @SerializedName("value") val value: String
)

data class MockoonRoute(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("type") val type: String = "http",
    @SerializedName("method") val method: String = "post",
    @SerializedName("endpoint") val endpoint: String = "",
    @SerializedName("documentation") val documentation: String = "",
    @SerializedName("responses") val responses: List<MockoonResponse> = emptyList(),
    @SerializedName("responseMode") val responseMode: String? = null
) {
    val fullPath: String get() = "/$endpoint"
    val httpMethod: String get() = method.uppercase()

    /** Extract first sample request body from documentation string */
    val sampleRequest: String?
        get() = extractJsonBlock(documentation, "SAMPLE REQUEST")

    /** Extract first sample response body from the first MockoonResponse */
    val sampleResponse: String?
        get() = responses.firstOrNull()?.body

    private fun extractJsonBlock(text: String, marker: String): String? {
        val startIdx = text.indexOf(marker)
        if (startIdx < 0) return null
        val jsonStart = text.indexOf('{', startIdx)
        if (jsonStart < 0) return null
        var depth = 0
        var jsonEnd = jsonStart
        for (i in jsonStart until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) { jsonEnd = i; break }
                }
            }
        }
        return if (jsonEnd > jsonStart) text.substring(jsonStart, jsonEnd + 1) else null
    }
}

data class MockoonResponse(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("statusCode") val statusCode: Int = 200,
    @SerializedName("latency") val latency: Int = 0,
    @SerializedName("label") val label: String = "",
    @SerializedName("rules") val rules: List<MockoonRule> = emptyList(),
    @SerializedName("headers") val headers: List<MockoonHeader> = emptyList()
)

data class MockoonRule(
    @SerializedName("target") val target: String = "",
    @SerializedName("modifier") val modifier: String = "",
    @SerializedName("value") val value: String = "",
    @SerializedName("operator") val operator: String = "equals",
    @SerializedName("invert") val invert: Boolean = false
)

data class TlsOptions(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("type") val type: String = "CERT"
)
