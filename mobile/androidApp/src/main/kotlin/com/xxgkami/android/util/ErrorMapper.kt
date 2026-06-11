package com.xxgkami.android.util

import android.util.Log
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 统一错误映射工具
 * 将技术性异常转换为用户友好的中文提示信息
 */
object ErrorMapper {
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
            is java.io.IOException -> "网络异常，请检查网络连接"
            else -> {
                Log.e("ErrorMapper", "Unhandled exception: ${e.javaClass.simpleName}: ${e.message}")
                "操作失败，请稍后重试"
            }
        }
    }
}
