package com.xxgkami.shared.model

import kotlinx.serialization.Serializable

/**
 * 通用API响应包装类
 * 所有API接口统一返回此格式，通过泛型 [T] 支持不同数据类型
 *
 * @param T 响应数据的类型
 * @property success 请求是否成功
 * @property message 响应消息（通常用于错误提示）
 * @property data 响应数据，可能为null
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)
