package com.xxgkami.android.util

import android.util.Log
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 统一错误映射工具
 * 将技术性异常转换为用户友好的中文提示信息
 */
object ErrorMapper {
    /**
     * 用于解析 HTTP 错误响应体中的 message 字段
     */
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 将异常映射为用户友好的错误提示
     * @param e 捕获的异常
     * @return 用户友好的错误提示字符串
     */
    fun mapError(e: Throwable): String {
        // CancellationException 必须优先匹配并向上传播，不能被吞掉
        if (e is kotlinx.coroutines.CancellationException) throw e
        return when (e) {
            is ConnectException -> "网络连接失败，请检查网络设置"
            is SocketTimeoutException -> "请求超时，请稍后重试"
            is UnknownHostException -> "无法连接服务器，请检查网络"
            is SerializationException -> "数据解析错误，请稍后重试"
            is ClientRequestException -> mapClientRequestException(e)
            is java.io.IOException -> "网络异常，请检查网络连接"
            else -> {
                Log.e("ErrorMapper", "Unhandled exception: ${e.javaClass.simpleName}: ${e.message}")
                "操作失败，请稍后重试"
            }
        }
    }

    /**
     * 解析 ClientRequestException，尝试从响应体提取服务端返回的 message 字段。
     * 若解析失败，则根据 HTTP 状态码给出默认提示。
     */
    private fun mapClientRequestException(e: ClientRequestException): String {
        // 先尝试从响应体解析服务端返回的 message 字段
        val serverMessage = try {
            val bodyText = e.response.bodyAsText()
            val parsed = json.parseToJsonElement(bodyText).jsonObject
            parsed["message"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
        if (!serverMessage.isNullOrBlank()) return serverMessage

        // 解析失败时，根据 HTTP 状态码给出默认提示
        return when (e.response.status.value) {
            401 -> "登录已过期，请重新登录"
            403 -> "没有权限执行此操作"
            404 -> "请求的资源不存在"
            in 400..499 -> "请求参数错误，请检查后重试"
            in 500..599 -> "服务器异常，请稍后重试"
            else -> "请求失败，请稍后重试"
        }
    }
}
