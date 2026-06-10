# 代码审查与修复 - 设计文档

**日期**: 2026-06-10
**状态**: 已完成初步审查，30 项修复待提交
**范围**: 后端(Spring Boot) + 前端(Vue 3) + 移动端(KMP)

---

## 1. 背景与目标

对 xxgkami-ai 全栈卡密验证系统进行全面代码审查，识别安全漏洞、代码质量问题、性能瓶颈，并按优先级修复。

## 2. 已完成的修复（30 项）

### 2.1 后端安全修复（14 项）

| # | 严重度 | 问题 | 修复方案 |
|---|--------|------|----------|
| 1 | 🔴严重 | 用户枚举：登录返回不同错误信息 | 统一返回"用户名或密码错误" |
| 2 | 🔴严重 | Order.cardKeys 无 @JsonIgnore，API 泄露明文卡密 | 添加 @JsonIgnore |
| 3 | 🔴严重 | recharge 缺少悲观锁，并发充值竞态 | 添加 PESSIMISTIC_WRITE 锁 |
| 4 | 🔴严重 | OnlineUserController 从请求体获取用户名 | 改用 Authentication 对象 |
| 5 | 🟠高 | TOTP 接口无限流 | 纳入 RateLimitFilter |
| 6 | 🟠高 | getUserInfo 未登录返回 200 | 改为 401 |
| 7 | 🟠高 | SettingsController 敏感字段脱敏不全 | 改为关键字匹配模式 |
| 8 | 🟡中 | UserService 错误消息回显用户名 | 移除用户名 |
| 9 | 🟡中 | GlobalExceptionHandler 缺少 405/415 | 添加异常处理器 |
| 10 | 🟡中 | SecurityConfig 未保护 online 接口 | 添加 authenticated() 规则 |
| 11 | 🟡中 | refreshToken N+1 查询 | 复用已查询对象 |
| 12 | 🟡中 | WalletController.getTransactions 无分页 | 添加分页参数 |
| 13 | 🟡中 | MonitorController 硬编码版本号 | 外部化为配置项 |
| 14 | 🟡中 | BackupService 硬编码备份目录 | 外部化为配置项 |

### 2.2 前端修复（8 项）

| # | 严重度 | 问题 | 修复方案 |
|---|--------|------|----------|
| 15 | 🔴严重 | AES 密钥硬编码 | 从 Token 动态派生（SHA256） |
| 16 | 🟠高 | xlsx 库 CVE-2023-30533 | 升级到 ^0.20.0 |
| 17 | 🟠高 | mockData.js 包含真实 API 密钥 | 替换为占位符 |
| 18 | 🟡中 | escapeHtml 缺少单引号转义 | 补全转义 |
| 19 | 🟡中 | getAllUsers size=9999 | 降为 500 |
| 20 | 🟡中 | OrdersManagePage size=9999 | 降为 200 |
| 21 | 🟡中 | xlsx 静态导入 ~400KB | 改为动态 import() |
| 22 | 🟡中 | DDL_AUTO 默认 update | 改为 validate |

### 2.3 移动端修复（8 项）

| # | 严重度 | 问题 | 修复方案 |
|---|--------|------|----------|
| 23 | 🔴严重 | EncryptedSharedPreferences 部分设备崩溃 | try-catch 降级 |
| 24 | 🟠高 | logout() 先清理 token 导致服务端登出失败 | 调换顺序 |
| 25 | 🟠高 | httpClient 为 public | 改为 private |
| 26 | 🟡中 | token/refreshToken 无线程安全保护 | 添加 @Volatile |
| 27 | 🟡中 | 4 个 API 类各自创建 Json 实例 | 统一使用 ApiProvider.json |
| 28 | 🟡中 | Theme 不支持暗色模式 | 添加 DarkColorScheme |
| 29 | 🟡中 | ApiProvider 缺少共享 Json 实例 | 添加 json 字段 |
| 30 | 🟢低 | 缺少暗色模式颜色定义 | 添加 Dark 配色常量 |

## 3. 剩余待修复问题

### 3.1 后端剩余问题

| # | 严重度 | 问题 | 建议方案 | 工作量 |
|---|--------|------|----------|--------|
| R1 | 🟠高 | JWT token 明文存储在数据库 | 存储 hash | 中 |
| R2 | 🟠高 | bind token 内存存储，重启丢失 | 迁移到 Redis 或数据库 | 中 |
| R3 | 🟡中 | MD5 用于支付签名 | 受限于第三方协议，需确认 | 小 |
| R4 | 🟡中 | ECC 私钥明文存储在文件系统 | 加密存储或 HSM | 中 |
| R5 | 🟡中 | Lombok @Data 在 JPA 实体上 | 改用 @Getter/@Setter | 大 |
| R6 | 🟡中 | 重复端点（payment、settings、maintenance） | 合并或废弃 | 小 |
| R7 | 🟢低 | @Async 邮件发送失败用户无感知 | 添加重试机制 | 中 |

### 3.2 前端剩余问题

| # | 严重度 | 问题 | 建议方案 | 工作量 |
|---|--------|------|----------|--------|
| R8 | 🟠高 | JWT 存 localStorage，XSS 可窃取 | 改用 httpOnly cookie | 大 |
| R9 | 🟡中 | console.error 散布在生产代码 | 统一日志方案 | 中 |
| R10 | 🟡中 | Element Plus 图标全量注册 | 按需导入 | 中 |
| R11 | 🟡中 | 大组件文件（3000+ 行） | 拆分子组件 | 大 |
| R12 | 🟢低 | UserPage 使用 Options API | 迁移到 Composition API | 中 |

### 3.3 移动端剩余问题

| # | 严重度 | 问题 | 建议方案 | 工作量 |
|---|--------|------|----------|--------|
| R13 | 🟠高 | iOS/Desktop Token 持久化缺失 | 实现 Keychain/安全存储 | 大 |
| R14 | 🟡中 | Ktor 版本 2.3.12 偏旧 | 升级到最新补丁版本 | 小 |
| R15 | 🟡中 | ViewModel 生命周期问题 | 使用 hilt-nav-compose | 中 |
| R16 | 🟡中 | 每次进入页面重新请求数据 | ViewModel 缓存 | 中 |

## 4. 推荐的下一步行动

### 阶段一：提交当前修复（立即）
- 将已有的 30 项修复提交到 git
- 创建语义化的 commit message

### 阶段二：高优先级剩余修复（R1-R2, R8）
- R1: JWT token hash 存储
- R2: bind token 持久化
- R8: 前端 token 存储安全

### 阶段三：中优先级优化（R3-R7, R9-R16）
- 代码质量改进
- 性能优化
- 依赖升级

## 5. 执行记录

### 已完成的提交

| 提交 | 内容 | 文件数 | 行数变化 |
|------|------|--------|----------|
| `fc9c5f9` | 初步审查修复（30项） | 33 | +475/-91 |
| `2677cef` | JWT hash、日志统一、Ktor升级 | 6 | +83/-24 |
| `51e049c` | 重复端点清理、Lombok修复 | 8 | +85/-8 |
| `7c1a43e` | BindToken持久化、私钥加密、ViewModel缓存 | 9 | +356/-35 |
| `8233f21` | 图标按需导入、UserPage迁移Composition API | 3 | +659/-712 |

**总计**: 5 次提交，59 个文件修改，+1658/-960 行变化

### 已修复的剩余问题

| # | 问题 | 提交 |
|---|------|------|
| R1 | bind token 内存存储 → 数据库持久化 | `7c1a43e` |
| R5 | ECC 私钥明文存储 → AES-256-GCM 加密 | `7c1a43e` |
| R6 | Element Plus 图标全量注册 → 按需导入（200+→16个） | `8233f21` |
| R8 | ViewModel 缓存 → 添加数据缓存机制 | `7c1a43e` |
| R9 | UserPage Options API → Composition API | `8233f21` |

### 仍待处理问题

| # | 严重度 | 问题 | 建议方案 | 原因 |
|---|--------|------|----------|------|
| R2 | 🟠高 | 前端 JWT 存 localStorage | 改用 httpOnly cookie | 需要后端配合设置 cookie，架构改动大 |
| R3 | 🟠高 | iOS/Desktop Token 持久化缺失 | 实现 Keychain | 需要平台特定代码，当前仅支持 Android |
| R4 | 🟡中 | MD5 用于支付签名 | 受限于第三方协议 | 需确认支付网关是否支持 HMAC-SHA256 |
| R7 | 🟡中 | 大组件文件拆分 | 拆分子组件 | 工作量大，功能回归测试复杂 |

## 6. 风险评估

- **DDL_AUTO 改为 validate**：首次部署需要手动建表，需确认数据库初始化脚本是否完善
- **xlsx 升级到 0.20.0**：许可证从 Apache 变为商业许可，需确认合规性
- **暗色模式**：可能需要调整部分组件的颜色适配
