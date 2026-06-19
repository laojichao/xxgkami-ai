package com.xxgkami.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxgkami.shared.api.ApiProvider
import com.xxgkami.shared.api.WalletApi
import com.xxgkami.shared.model.Wallet
import com.xxgkami.shared.model.WalletTransaction
import com.xxgkami.android.util.ErrorMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * 钱包业务ViewModel
 * 管理用户钱包余额、充值操作、交易记录的加载和状态
 * 通过 StateFlow 暴露钱包数据给 UI 层
 */
class WalletViewModel : ViewModel() {
    // API客户端和钱包API实例
    private val apiClient = ApiProvider.apiClient
    private val walletApi = WalletApi(apiClient)

    // 钱包信息（余额、累计充值、累计消费）
    private val _wallet = MutableStateFlow<Wallet?>(null)
    val wallet: StateFlow<Wallet?> = _wallet

    // 交易记录列表
    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions

    // 加载状态（使用计数器支持并发操作，如同时加载钱包和交易记录）
    // 说明：采用计数器方案而非布尔值，是为了支持多个并发加载操作（如同时加载钱包和交易记录），
    // 只有所有操作都完成后才隐藏加载指示器。暂不统一为布尔值方案。
    private val _loadingCount = MutableStateFlow(0)
    val isLoading: StateFlow<Boolean> = _loadingCount.map { it > 0 }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 数据缓存标记：避免页面切换时重复请求
    private var walletLoaded = false
    private var transactionsLoaded = false

    /**
     * 加载用户钱包信息
     * @param forceRefresh 是否强制刷新，忽略缓存
     * 成功时更新 [_wallet]，失败时更新 [_error]
     */
    fun loadWallet(forceRefresh: Boolean = false) {
        if (walletLoaded && !forceRefresh) return
        viewModelScope.launch {
            _loadingCount.update { it + 1 }
            _error.value = null
            try {
                val response = walletApi.getWallet()
                _wallet.value = response.data
                walletLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _loadingCount.update { it - 1 }
            }
        }
    }

    /**
     * 钱包充值
     * @param amount 充值金额（字符串格式）
     */
    fun recharge(amount: String) {
        // 使用统一的校验函数，确保 UI 与 ViewModel 校验逻辑一致
        val validation = validateRechargeAmount(amount)
        if (validation != null) {
            _error.value = validation
            return
        }
        viewModelScope.launch {
            _loadingCount.update { it + 1 }
            _error.value = null
            try {
                val response = walletApi.recharge(amount)
                _wallet.value = response.data
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _loadingCount.update { it - 1 }
            }
        }
    }

    /**
     * 加载交易记录列表
     * @param forceRefresh 是否强制刷新，忽略缓存
     * 成功时更新 [_transactions]，失败时更新 [_error]
     */
    fun loadTransactions(forceRefresh: Boolean = false) {
        if (transactionsLoaded && !forceRefresh) return
        viewModelScope.launch {
            _loadingCount.update { it + 1 }
            _error.value = null
            try {
                val response = walletApi.getTransactions()
                _transactions.value = response.data ?: emptyList()
                transactionsLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.mapError(e)
            } finally {
                _loadingCount.update { it - 1 }
            }
        }
    }

    /**
     * 重置所有缓存标记
     * 在用户登出时调用，确保下次登录后重新加载数据
     */
    fun resetCache() {
        walletLoaded = false
        transactionsLoaded = false
        _wallet.value = null
        _transactions.value = emptyList()
        _error.value = null
    }

    companion object {
        /** 单次充值金额上限（元） */
        const val MAX_RECHARGE_AMOUNT = 100000.0

        /** 最大小数位数 */
        const val MAX_DECIMAL_PLACES = 2

        /**
         * 统一的充值金额校验函数，供 ViewModel 和 UI 共用，确保校验逻辑一致。
         *
         * 使用 BigDecimal 严格解析金额，避免 Double 解析的精度问题。
         *
         * @param amount 充值金额字符串
         * @return 校验失败返回错误信息，校验通过返回 null
         */
        fun validateRechargeAmount(amount: String): String? {
            // 使用 BigDecimal 严格解析，避免 Double 解析的精度问题（如 "0.1" + "0.2" != "0.3"）
            val parsedAmount = try {
                BigDecimal(amount.trim())
            } catch (e: NumberFormatException) {
                return "请输入有效的充值金额"
            }
            // 校验金额为正数
            if (parsedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return "请输入有效的充值金额"
            }
            // 校验金额上限
            if (parsedAmount > BigDecimal.valueOf(MAX_RECHARGE_AMOUNT)) {
                return "单次充值金额不能超过${MAX_RECHARGE_AMOUNT.toInt()}元"
            }
            // 校验小数位数
            val scale = parsedAmount.stripTrailingZeros().scale()
            if (scale > MAX_DECIMAL_PLACES) {
                return "充值金额最多支持${MAX_DECIMAL_PLACES}位小数"
            }
            return null
        }
    }
}
