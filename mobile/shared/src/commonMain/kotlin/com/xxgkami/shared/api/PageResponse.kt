package com.xxgkami.shared.api

import com.xxgkami.shared.model.ApiResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * Spring Data Page 分页响应包装
 *
 * 后端分页接口（如 GET /orders、GET /wallet/transactions）返回
 * `{content:[...], totalPages, totalElements, pageable, sort, ...}` 结构，
 * 本地仅消费 [content] 字段，其余字段依赖 ignoreUnknownKeys 忽略。
 *
 * @param T 列表元素类型
 * @property content 当前页数据列表
 */
@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList()
)

/**
 * 为具体元素类型构造 PageResponse&lt;T&gt; 的序列化器。
 *
 * kotlinx.serialization 的泛型 @Serializable 类需显式传入元素序列化器：
 * `PageResponse.serializer(elementSerializer)`。
 */
fun <T> pageSerializer(elementSerializer: KSerializer<T>): KSerializer<PageResponse<T>> =
    PageResponse.serializer(elementSerializer)

/**
 * 将 Page 分页响应解包为普通 List 响应，供上层按列表消费。
 */
fun <T> ApiResponse<PageResponse<T>>.unwrapPage(): ApiResponse<List<T>> =
    ApiResponse(success, message, data?.content)
