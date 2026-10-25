/**
 * 安全解析后端返回的时间字符串。
 *
 * 后端 Jackson 配置为 `yyyy-MM-dd HH:mm:ss`（空格分隔），该格式在
 * Safari/iOS WebView 上 new Date() 解析返回 Invalid Date（ECMAScript
 * 规范仅保证 ISO 8601 的 "T" 分隔格式）。所有消费后端时间字段的地方
 * 必须经由本工具解析，禁止直接 new Date(后端字符串)。
 *
 * @param {string|null|undefined} value 后端时间字符串（yyyy-MM-dd HH:mm:ss 或 ISO）
 * @returns {Date} 解析成功返回 Date 对象；空值/非法输入返回 Invalid Date（与
 *                原生行为一致，调用方原有的判空逻辑不变）
 */
export function parseBackendDate(value) {
  if (!value) return new Date(NaN)
  // 空格分隔 → "T" 分隔（兼容 Safari），其余原样交给原生解析
  return new Date(String(value).replace(' ', 'T'))
}
