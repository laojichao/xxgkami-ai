package com.xxgkami.shared.api

import com.xxgkami.shared.model.*

/**
 * 钱包相关API
 * 封装钱包信息查询、交易记录查询等接口
 *
 * 说明：不封装 /wallet/recharge —— 该接口后端已限定 ADMIN，
 * 移动端无自助充值入口，保留只会产生必 403 的死代码。
 *
 * @param client HTTP客户端实例
 */
class WalletApi(private val client: ApiClient) {
    // 使用全局共享的 JSON 解析器，保持配置一致性
    private val json = ApiProvider.json

    /** 获取用户钱包信息 */
    suspend fun getWallet(): ApiResponse<Wallet> {
        val response = client.get("/wallet")
        return json.decodeFromString(ApiResponse.serializer(Wallet.serializer()), response)
    }

    /**
     * 获取交易记录列表。
     *
     * 后端 GET /wallet/transactions 返回 Page<WalletTransaction> 分页结构
     * （{content:[...], totalPages, ...}），先按 PageResponse 解析再取 content；
     * 未带分页参数时后端默认返回第 0 页（20 条）。
     */
    suspend fun getTransactions(): ApiResponse<List<WalletTransaction>> {
        val response = client.get("/wallet/transactions")
        val page = json.decodeFromString(ApiResponse.serializer(pageSerializer(WalletTransaction.serializer())), response)
        return page.unwrapPage()
    }
}
