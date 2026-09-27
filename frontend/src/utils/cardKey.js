/**
 * 卡密派生值工具模块
 *
 * ⚠ 重要：本模块的函数**不是**后端加密卡密的复刻，两者结果不同，不可互相替代。
 *
 * 后端 `CustomCardObfuscator.generateEncryptedKey` 的算法是：
 *   Base64( SHA-256( cardKey ) )   —— 结果存放在 `Card.encryptedKey` 字段，
 * 并通过接口（如 /cards/admin/all、/cards/apikey/{id}）以 `encryptedKey` 返回。
 * 前端展示或导出「加密卡密」时应直接使用后端返回值。
 *
 * 本模块提供的是**纯前端派生值**，仅用于不希望暴露明文卡密的展示场景
 * （例如自定义混淆展示），其结果与数据库中的 encryptedKey 无关，
 * 不可用于任何服务端比对、查重或校验逻辑。
 */

/**
 * 计算卡密的本地混淆展示值（与后端 encryptedKey 不同，仅供前端展示）。
 * 算法：URL编码 -> 字符反转 -> Base64 -> 字符替换
 *
 * @param {string} rawKey - 原始卡密
 * @returns {string} 混淆后的字符串，失败时返回原值
 */
export function obfuscateCardKey(rawKey) {
    if (!rawKey) return rawKey
    try {
        const encoded = encodeURIComponent(rawKey)
        const reversed = encoded.split('').reverse().join('')
        const base64 = btoa(reversed)
        return base64.replace(/e/g, '*').replace(/U/g, '-')
    } catch (e) {
        // btoa 对非 Latin-1 字符会抛错；此处降级返回原值由调用方决定如何处理
        console.error('obfuscateCardKey failed:', e)
        return rawKey
    }
}
