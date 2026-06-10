/**
 * 统一日志工具
 * 开发环境输出到控制台，生产环境静默（可扩展为错误上报）
 */

const isDev = import.meta.env.DEV

export const logger = {
  error(message, ...args) {
    if (isDev) {
      console.error(`[ERROR] ${message}`, ...args)
    }
    // 生产环境可扩展：发送到错误监控服务
  },

  warn(message, ...args) {
    if (isDev) {
      console.warn(`[WARN] ${message}`, ...args)
    }
  },

  info(message, ...args) {
    if (isDev) {
      console.info(`[INFO] ${message}`, ...args)
    }
  },

  debug(message, ...args) {
    if (isDev) {
      console.debug(`[DEBUG] ${message}`, ...args)
    }
  }
}

export default logger
