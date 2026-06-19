/**
 * 卡密混淆工具模块
 * 与后端 CustomCardObfuscator 保持一致的混淆算法
 * 供 ApiManagePage、KeysManagePage 等组件统一导入使用，避免重复代码
 */
import logger from './logger'

/**
 * 卡密混淆函数（与后端 CustomCardObfuscator 保持一致）
 * 算法：URL编码 -> 反转 -> Base64 -> 替换字符
 * @param {string} rawKey - 原始卡密
 * @returns {string} 混淆后的卡密，失败时返回原值
 */
export function obfuscateCardKey(rawKey) {
    if (!rawKey) return rawKey
    try {
        const encoded = encodeURIComponent(rawKey)
        const reversed = encoded.split('').reverse().join('')
        const base64 = btoa(reversed)
        return base64.replace(/e/g, '*').replace(/U/g, '-')
    } catch (e) {
        logger.error('Obfuscation failed:', e)
        return rawKey
    }
}
