# 第四轮全栈代码审查报告与修复记录

**审查日期**: 2026-10-24
**审查范围**: 后端安全层/业务层 + 前端 + 移动端(KMP) + 部署配置
**审查方法**: 4 路并行深度审查（后端安全、后端业务、前端契约、移动端+DevOps），全部问题经读码验证
**前序轮次**: 2026-06-10 首轮（30 项）、2026-06-10 次轮（43 项）、2026-06-20 三轮（114 项）

---

## 1. 审查概述

本轮在前三轮 200+ 项修复基础上，重点排查：安全配置遗漏、前后端契约漂移、并发/事务正确性、移动端交付阻断。**共发现 73 项问题**（去重后）。

### 1.1 问题统计

| 模块 | Critical | High | Medium | Low | 小计 |
|------|----------|------|--------|-----|------|
| 后端安全 | 1 | 2 | 5 | 3 | 11 |
| 后端业务 | 0 | 4 | 7 | 8 | 19（含 1 项与安全轮重复） |
| 前端 | 3 | 8 | 9 | 7 | 27 |
| 移动端+部署 | 3 | 3 | 4 | 7 | 17 |
| **合计** | **7** | **17** | **25** | **25** | **73**（去重后 72） |

### 1.2 本轮核心发现

1. **前后端契约漂移是重灾区**：前端 3 个 Critical 全部是"前端按旧契约/想象契约调用"（snake_case 载荷、register-bind 退化、ADMIN-only 接口被用户端调用）。
2. **移动端从未成功编译/运行过**：baseUrl 从未初始化 + shared 模块反向依赖 androidApp（编译必失败）。
3. **安全配置最后一公里**：`/card-pricing` 写操作漏出 ADMIN 保护（可 0.01 元购卡）；`/auth/totp/*` 未校验角色（同名用户可劫持管理员 TOTP）。

---

## 2. Critical 级别问题（7 项）

### B-S1. /card-pricing 写操作未限 ADMIN，普通用户可改价格 0.01 元购卡
- **文件**: `backend/.../config/SecurityConfig.java:106-146` + `controller/CardPricingController.java:43-72`
- **问题**: SecurityConfig 仅有 `/card-pricing/admin/**` 死规则，实际写端点 `POST /card-pricing`、`PUT /card-pricing/{id}`、`DELETE /card-pricing/{id}` 落入 `anyRequest().authenticated()`。攻击链：注册用户 → 改价 0.01 → 下单支付 → 发卡。前端管理页用 `/pricing`（有保护），别名路径成漏网之鱼。
- **修复**: GET permitAll、其余 `/card-pricing`+`/pricing` 写操作限 ADMIN；`CardPricingService.save` 补 price>0 校验。

### F1. ApiManagePage 专属卡密生成载荷用 snake_case，功能整体失效
- **文件**: `frontend/src/components/ApiManagePage.vue:412-422`
- **问题**: 传 `card_type/total_count/api_key_id` 等字段，后端 `GenerateCardRequest` 是 camelCase，Jackson 静默忽略 → 类型/绑定/次数全丢。KeysManagePage.vue:700-718 已正确，仅此处漏。
- **修复**: 改 camelCase。

### F2. register-bind 契约不匹配，OAuth 首次注册闭环 100% 失败
- **文件**: `backend/.../controller/AuthController.java:177-180` + `frontend/src/components/loginform.vue:851-875`
- **问题**: 前端提交 `{username,password,email,registerToken}`，后端接收 `RegisterRequest`（email/code @NotBlank，无 registerToken 字段）→ 必 400。
- **修复**: 后端恢复 `RegisterBindRequest`（registerToken 流，免邮箱验证码）。

### F3 / M3. /cards/user/{userId}、/cards/query 限 ADMIN，普通用户"我的卡密/卡密查询"必 403
- **文件**: `backend/.../config/SecurityConfig.java:124` + `CardController.java:174`；前端 `UserPage.vue:1019,1227-1235`；移动端 `CardApi.kt:49`
- **问题**: 前端用户中心与移动端 MyCardsScreen 调用 ADMIN-only 接口，核心功能不可用。
- **修复**: 后端新增 `GET /cards/user/me`（token 提取 userId，脱敏返回），前端/移动端改调。

### M1. 移动端 API baseUrl 从未初始化，所有请求必抛 IllegalStateException
- **文件**: `mobile/shared/.../api/ApiClient.kt:33,65` + `ApiProvider.kt:18`
- **问题**: 全仓库无任何调用 `ApiProvider.updateBaseUrl()`，`requireBaseUrl()` 必抛异常。
- **修复**: androidApp 增加 `buildConfigField API_BASE_URL`，Application 启动时初始化。

### M2. shared 模块反向依赖 androidApp，Android 编译必失败
- **文件**: `mobile/shared/src/androidMain/.../util/PlatformTokenStore.kt:3`
- **问题**: `import com.xxgkami.android.data.TokenStore`，依赖方向循环，Unresolved reference。
- **修复**: 迁移 TokenStore 至 shared/androidMain 或删除死代码。

---

## 3. High 级别问题（17 项）

### 后端

| # | 问题 | 文件:行号 | 修复方案 |
|---|------|-----------|----------|
| B-S2 | /auth/totp/*、/auth/admin/update 仅 authenticated()，同名普通用户可劫持管理员 TOTP/邮箱（用户名可跨表碰撞注册） | AuthController.java:342-515 | 端点加 hasRole(ADMIN)；注册加保留用户名/字符白名单校验 |
| B-S3 | /cards/verify 机器码可选，绕过 /cards/use 的防共享策略（不传 machineCode 则永不绑定，任意设备可用） | VerifyCardRequest.java:16-18 + CardService.java:472-487 | machineCode @NotBlank，与 /cards/use 对齐 |
| B-1 | 验证码 attempts 递增随事务回滚，爆破限制完全失效（REQUIRES_NEW 未用于 register/resetPassword） | AuthService.java:262-266,477-481 | 仿 recordLoginFailureInNewTransaction 独立事务提交 |
| B-2 | WalletService 先普通读后加锁，余额丢失更新（无 @Version，lock 不刷新上下文） | WalletService.java:78-97,128-130 | 新增 findByUserIdForUpdate 锁定读 |
| B-3 | 重置/修改密码后不吊销已发 token，被盗号者 refresh token 可无限续签 | AuthService.java:488-493 + UserService.java:161-172 | 清空 accessToken/refreshToken 哈希 + 失效缓存 |
| B-4 | 已取消订单收到真实支付款后回调返回 success，吞单无对账；createPayment 不校验订单状态 | PaymentService.java:47-49,135-138 | cancelled/failed 返回 fail + 告警；createPayment 限 pending |

### 前端

| # | 问题 | 文件:行号 | 修复方案 |
|---|------|-----------|----------|
| F4 | user.status === 1 与后端 Boolean 比较，启用/禁用按钮永不渲染 | UserManagePage.vue:31-79 | 按布尔处理 |
| F5 | totpEnabled 后端不返回，禁用 TOTP 界面刷新后不可达 | SettingsPage.vue:384-394 + AuthService.java:115-119 | /auth/user/info 与登录响应补 totpEnabled |
| F6 | hasPassword 后端不返回，修改密码功能对所有用户不可用（oldPassword 恒空 → 400） | UserPage.vue:551,841-857 | /user/profile 返回 hasPassword + 前端兜底显示旧密码框 |
| F7 | /maintenance/status 限 ADMIN，维护遮罩对普通用户完全不生效 | App.vue:167-179 + SecurityConfig.java:130 | GET /maintenance/status permitAll（仅返回开关与文案） |
| F8 | /settings/all 限 ADMIN，登录页 oauthEnabled 恒 false，第三方登录入口永不显示 | loginform.vue:542 + UserPage.vue:893 | 新增公开 GET /public/settings/oauth |
| F9 | MaintenanceAdmin 读顶层 res.data.content，实际结构在 res.data.settings → 每次保存清空维护文案 | MaintenanceAdmin.vue:94-140 | 改读 res.data.settings |
| F10 | getAllCards() 不带分页参数只取前 100 条，客户端分页造成数据完整假象 | api.js:555-557 + Dashboard.vue:358-379 | 循环拉全量或服务端分页 |
| F11 | assignedUsers 后端不装配，分配弹窗"已分配"列表永远为空 | ApiManagePage.vue:199,605-614 | 后端 list 联查 user_api_keys 返回 |

### 移动端+部署

| # | 问题 | 文件:行号 | 修复方案 |
|---|------|-----------|----------|
| M4 | GET /orders 返回 Page<Order>，移动端按 List 解析必抛 SerializationException | OrderApi.kt:34-36 | 定义 PageResponse 包装解析 data.content |
| M5 | GET /wallet/transactions 同样 Page vs List 解析失败 | WalletApi.kt:37-39 | 同上 |
| M6 | XFF 取首跳可伪造绕过全部限流；docker-compose 可信代理 IP 硬编码猜测值（172.20.0.3 猜错则全员共享一个限流桶） | RateLimitFilter.java:207-218 + nginx.conf:37,61,76 + docker-compose.yml:68 | 后端自右向左取第一个非可信代理 IP；compose 固定子网/IP |

---

## 4. Medium 级别问题（25 项）

### 后端（12 项）

| # | 问题 | 文件:行号 | 修复方案 |
|---|------|-----------|----------|
| B-S4 | access token 不与库中哈希比对，重登/刷新后旧 token 仍有效至自然过期 | JwtRequestFilter.java:139-172 | 过滤器中 SHA-256 常量时间比对 |
| B-S5 | OAuth state 任何匿名者可铸造且不绑定请求者，set-cookies 存在登录 CSRF/会话固定 | AuthController.java:596-653 | set-cookies 校验 Origin/Referer（本轮最小缓解） |
| B-S6 | /user/social/bind 接受未验证的 socialUid，可抢注他人社交身份 | UserController.java:188-211 | socialType 白名单校验（记录待完整 OAuth 流程） |
| B-S7 | 公开自助解绑未提供 machine_code 时零凭据直接解绑，凭卡密明文即可盗卡 | CardService.java:634-654 | 解绑必须提供匹配的 machine_code |
| B-5 | failOrder 无锁 check-then-act，可将 completed 订单覆盖为 failed | OrderService.java:188-199 | 改用 findByOrderNoWithLock + 状态机校验 |
| B-6 | ApiKey 体系约一半死代码：incrementUseCount 无调用、requireMachineCode/machineSpecOnceConfig 无消费、ApiKeyMachineSpecRedemption 零调用且类型不一致、UserApiKey 无鉴权消费 | ApiKeyService.java:183-186 等 | verifyCard 接入 ApiKey 认证与计数；暂缓 Redemption 实体（记录） |
| B-7/B-S8 | 明文密码管理员仅锁 15 分钟自动恢复（日志称"已禁用"不实；Admin 无 status 字段） | DatabaseInitializer.java:83-85 | lock_time 置远未来实现永久锁定 |
| B-8 | BackupService mysqldump 无超时可永久挂起请求线程 | BackupService.java:75-81 | waitFor(10, MINUTES) + destroyForcibly |
| B-9 | KeyManagerService 整体死代码却强制 KEY_ENCRYPTION_KEY 环境变量，否则拒绝启动 | KeyManagerService.java:44-59 | 删除死代码服务与环境变量强校验 |
| B-10 | assignUser 不校验目标用户存在，产生孤儿数据 | ApiKeyController.java:123-141 | existsById 校验 |
| B-11 | 删除用户/ApiKey 留孤儿：user_api_keys、bind_tokens、cards.api_key_id 悬空 | UserService.java:219-237 + ApiKeyService.java:174-177 | 级联清理 |

### 前端（9 项）

| # | 问题 | 文件:行号 | 修复方案 |
|---|------|-----------|----------|
| F12 | machineSpecOnceConfig 编辑时空串提交清空后端配置（@JsonIgnore 列表不返回 + null 判断失效） | EditApiKeyDialog.vue:107-120 | 空值不提交该字段 |
| F13 | copy-encrypted-card 事件传明文字符串，处理器取 .encryptedKey 恒空 → 加密复制 100% 失败 | CardCodesDialog.vue:90 + ApiManagePage.vue:472-486 | emit 传整个对象 |
| F14 | 金额浮点直乘显示 ¥59.70000000000001 | UserPage.vue:348,415,1338,1342 | toFixed(2) 统一计价函数 |
| F15 | 订单成功弹窗读 snake_case 字段（order_id/total_price），后端是 camelCase | UserPage.vue:1417 | 改 id/totalPrice |
| F16 | 用户端使用卡密 deviceId='Unknown' 默认值污染机器码数据 | UserPage.vue:1269 + api.js:576-585 | 移除默认值，要求输入 |
| F17 | 客户端 AES 密钥派生自明文 token+硬编码盐，加密无保密性 | UserSettingsPage.vue:104-115 | 记录（需产品决策配对码方案） |
| F18 | 注册表单与后端校验不一致（nickname/phone 静默丢弃、密码无 8 位下限） | loginform.vue:589-676 | 前端补长度校验 |
| F19 | 购买竞态：loading 在 confirm 之后置位，双击叠两个确认框创建两笔订单 | UserPage.vue:1325-1351 | 入口即置 loading |
| F20 | xlsx 0.18.5 含 CVE-2023-30533/CVE-2024-22363，npm 通道停更 | package.json:23 | 记录（升级需换 CDN 渠道或 exceljs） |

### 移动端+部署（4 项）

| # | 问题 | 文件:行号 | 修复方案 |
|---|------|-----------|----------|
| M7 | CreateOrderRequest 缺后端必填 pricingId，下单必 400 | OrderApi.kt:16-21 | 补字段 |
| M8 | TokenStore 异步初始化晚于登录态读取，已登录用户冷启动丢登录态 | MainActivity.kt:25-27 | 等待 init 完成再组合导航 |
| M9 | .env.example 声明 ADMIN_PASSWORD/SPRING_PROFILES_ACTIVE/BACKEND_URL 等无效变量误导部署 | .env.example:76-84 | 清理并注明真实流程 |
| M10 | nginx add_header 继承陷阱：含 add_header 的 location 丢失 server 级安全头 | nginx*.conf 多处 | include 片段统一注入全部安全头 |

---

## 5. Low 级别问题（25 项）

### 后端（11 项）

| # | 问题 | 文件:行号 | 处理 |
|---|------|-----------|------|
| B-S9 | refresh_token Cookie path=/auth/refresh 未考虑 context-path=/api，Web 刷新 Cookie 永不回传 | AuthController.java:689-695 | 修复：拼 contextPath |
| B-S10 | 匿名请求逐条异步写 access_logs，可被刷爆 | RequestMonitorFilter.java:81-106 | 记录（需产品决策采样策略） |
| B-S11 | 知道用户名即可循环锁定任意账号并吊销其在线 token | AuthService.java:193-209 | 记录（需锁定策略重构） |
| B-12 | SettingsService.getValue 把调用方默认值当配置值缓存，跨调用方污染 | SettingsService.java:52-58 | 修复：不存在只缓存哨兵 |
| B-13 | validateBindToken TOCTOU 双花窗口 | AuthService.java:423-447 | 修复：UPDATE 判定影响行数 |
| B-14 | 验证码明文入库 + 每邮箱无每日上限 | VerificationCode.java:21-22 | 修复：哈希入库（发码/验码同步改） |
| B-15 | TOTP 加密密钥派生自 jwt.secret（轮换即失效）+ 验证码无重放保护 | TotpService.java:28-43,146-162 | 记录（改派生会使存量失效） |
| B-16 | Webhook IPv6 目标构造非法 URI 静默失败 | WebhookService.java:106 | 修复：IPv6 加方括号 |
| B-17 | 卡密过期依赖 JVM 默认时区，容器 UTC 时客户端比对偏差 8 小时 | application.properties:11 | 修复：main() 固定 Asia/Shanghai |
| B-18 | updateProfile 换绑邮箱无需任何验证，可冒认他人邮箱 | UserService.java:121-128 | 修复：邮箱变更需验证码 |
| B-19 | 卡密签名 salt 与签名同表存储，不防 DB 写权限攻击者 | CardService.java:379-397 | 记录（远期架构升级） |

### 前端（7 项）

| # | 问题 | 文件:行号 | 处理 |
|---|------|-----------|------|
| F21 | setCookieRes 返回值未检查；OAuth 管理员登录固定落入用户页 | App.vue:167-230 | 修复 |
| F22 | 401 排队重试复用已 clearTimeout 的旧 AbortController.signal，重试无超时 | api.js:96-105 | 修复：重试新建 controller |
| F23 | emit('login-success') 对 resultData.userInfo 无判空，报错语义失真 | loginform.vue:941-955 | 修复 |
| F24 | cardKey.js 用 console.error 未走 logger | utils/cardKey.js:32 | 修复 |
| F25 | 默认值硬编码第三方支付域名 pay.myzfw.com | SettingsPage.vue:486 | 修复：置空 |
| F26 | localStorage 以明文邮箱作 key 永久残留 | loginform.vue:619,750 | 修复：注册成功后清理 |
| F27 | 大批量创建卡密 for 循环逐条发最多 1000 个串行请求；≤3 张失败无提示 | Dashboard.vue:238-277 | 修复：批量接口 + 失败提示 |

### 移动端+部署（7 项）

| # | 问题 | 文件:行号 | 处理 |
|---|------|-----------|------|
| M11 | MySQL healthcheck 密码暴露在进程命令行 | docker-compose.yml:25 | 修复：去密码（ping 不需认证） |
| M12 | Root 检测文件 I/O 在主线程 | MainActivity.kt:29 | 修复：移入 IO 协程 |
| M13 | .gitignore 缺 *.jks/*.keystore 规则 | .gitignore | 修复 |
| M14 | logout 携带后端完全忽略的 id/role body | AuthApi.kt:72-79 | 修复：无 body 调用 |
| M15 | User 模型字段与 /auth/user/info 响应不齐（phone/status/createTime 恒空） | User.kt:21-30 + ProfileScreen.kt:81-83 | 修复：移除恒空展示 |
| M16 | 移动端 recharge 死代码且必 403 | WalletApi.kt:28-34 | 修复：删除 |
| M17 | CORS 默认值含无效条目 http://localhost:80 | docker-compose.yml:67 | 修复 |

---

## 6. 修复记录

**修复完成日期**: 2026-10-24（审查当轮完成）

### 6.1 后端（28 项修复，2 个新增 DTO，1 个删除服务）

| 项 | 修复内容 | 文件 |
|---|---------|------|
| B-S1 | /card-pricing 别名路径与 /pricing 权限对齐（GET 公开、写限 ADMIN）+ 定价 price>0/上限校验 | SecurityConfig.java、CardPricingService.java |
| B-S2 | /auth/totp/*、/auth/admin/update 加 @PreAuthorize(ADMIN)+@EnableMethodSecurity；注册/registerBind/createUser 增加用户名字符白名单与管理员保留名校验 | AuthController.java、AuthService.java、UserService.java、SecurityConfig.java |
| B-S3 | VerifyCardRequest.machineCode @NotBlank，与 /cards/use 防共享策略对齐 | VerifyCardRequest.java |
| B-S4 | JwtRequestFilter 对 access token 做 SHA-256 常量时间比对（缓存值附带 token 指纹防轮换误缓存） | JwtRequestFilter.java |
| B-S5 | set-cookies 增加 Origin/Referer 白名单校验（缓解登录会话固定） | AuthController.java |
| B-S6 | /user/social/bind socialType 白名单（qq/wx/wechat/alipay） | UserController.java |
| B-S7 | 公开自助解绑必须提供匹配 machine_code（删除零凭据解绑分支），PublicController 同步必填校验 | CardService.java、PublicController.java |
| B-S8/B-7 | 明文密码管理员 lock_time 置 100 年后实现永久锁定；修正日志中的无效 SQL；admins 表为空时创建默认 admin/admin123（与 README 对齐） | DatabaseInitializer.java |
| B-1 | 验证码尝试计数移入 REQUIRES_NEW 独立事务（新增 incrementAttempts 原子更新），爆破限制真正生效 | AuthService.java、VerificationCodeRepository.java |
| B-2 | WalletRepository 新增 findByUserIdForUpdate 锁定读，recharge/consume 改用，消除余额丢失更新 | WalletRepository.java、WalletService.java |
| B-3 | resetPassword/changePassword 后吊销全部 token + 失效认证缓存 | AuthService.java、UserService.java |
| B-4 | createPayment 限 pending 状态；已取消/失败订单回调返回 fail + 支付对账告警日志（completed 幂等返回 success） | PaymentService.java |
| B-5 | failOrder 改锁定读 + failed 状态幂等返回 | OrderService.java |
| B-8 | mysqldump 进程 10 分钟超时 + destroyForcibly | BackupService.java |
| B-9 | 删除死代码 KeyManagerService（消除 KEY_ENCRYPTION_KEY 启动强校验阻断） | 已删除 |
| B-10 | assignUser 校验目标用户存在 | ApiKeyController.java |
| B-11 | deleteUser 级联清理 user_api_keys、bind_tokens；deleteApiKey 级联清理 user_api_keys | UserService.java、ApiKeyService.java |
| B-12 | getValue 不再把调用方默认值写入缓存（只缓存哨兵） | SettingsService.java |
| B-13 | validateBindToken 改条件 UPDATE 原子消费（markUsedIfUnused），消除 TOCTOU 双花 | AuthService.java、BindTokenRepository.java |
| B-14 | 验证码 SHA-256 哈希入库，比对哈希（常量时间） | AuthService.java |
| B-16 | Webhook IPv6 目标加方括号 | WebhookService.java |
| B-17 | main() 固定时区 Asia/Shanghai | BackendApplication.java |
| B-18 | 换绑邮箱需 bind 类型验证码（adminUpdateProfile 管理员路径豁免） | UserService.java、UserController.java |
| F2 后端 | register-bind 恢复独立 RegisterBindRequest（username/password/email/registerToken）；bind/validate 改用新 BindValidateRequest | AuthController.java、dto 两个文件 |
| F3 后端 | 新增 GET /cards/user/me（token 提取 userId，USER/ADMIN 可用） | CardController.java、SecurityConfig.java |
| F5/F6 后端 | /auth/user/info、登录响应、/user/profile 补充 totpEnabled/hasPassword | AuthController.java、AuthService.java、UserController.java |
| F7/F8 后端 | GET /maintenance/status permitAll；新增公开 GET /public/settings/oauth | SecurityConfig.java、PublicController.java |
| F11 后端 | /admin/apikeys 列表装配 assignedUsers 用户摘要 | ApiKeyController.java、UserApiKeyRepository.java |
| M6 后端 | XFF 解析改自右向左取第一个非可信代理 IP（防伪造绕过限流） | RateLimitFilter.java |

### 6.2 前端（20 项修复）

| 项 | 修复内容 | 文件 |
|---|---------|------|
| F1 | 专属卡密生成载荷 snake_case→camelCase（补 days/apiKeyId） | ApiManagePage.vue |
| F3 | getUserCards 改 /cards/user/me；queryCard 改我的卡密本地匹配 | api.js、UserPage.vue |
| F4 | 用户状态按 Boolean 判断，启停按钮恢复可用 | UserManagePage.vue |
| F6 | hasPassword !== false 兜底显示旧密码框（校验+模板） | UserPage.vue |
| F8 | 登录页/用户中心改调公开 /public/settings/oauth（新增 getPublicOAuthSettings） | api.js、loginform.vue、UserPage.vue |
| F9 | 维护页改读 res.data.settings 双层结构，修复保存清空文案 | MaintenanceAdmin.vue |
| F10 | getAllCards 循环分页拉全量（上限 100 页防死循环） | api.js |
| F12 | machineSpecOnceConfig 空值不提交 | EditApiKeyDialog.vue |
| F13 | 加密复制 emit 传整个对象 | CardCodesDialog.vue |
| F14 | 购买总价 toFixed(2) | UserPage.vue |
| F15 | 订单弹窗读 id/totalPrice（camelCase） | UserPage.vue |
| F16 | 使用卡密改 ElMessageBox.prompt 输入机器码（必填） | UserPage.vue、api.js |
| F18 | 注册表单补用户名 3-50/密码 8-50 前置校验；载荷移除后端不接收的 nickname/phone | loginform.vue |
| F19 | purchaseLoading 前置到确认框之前，防双击叠单 | UserPage.vue |
| F21 | set-cookies 失败即中止并提示；OAuth 登录按角色跳转 admin/user | App.vue |
| F22 | 401 排队重试新建 AbortController | api.js |
| F23 | emit login-success 对 userInfo 判空兜底 | loginform.vue |
| F24 | cardKey.js 改用 logger | cardKey.js |
| F25 | epay_api_url 默认值置空 | SettingsPage.vue |
| F26/F27/F28 | 注册成功清理 localStorage 时间戳；批量生成改单请求+失败提示；换绑邮箱验证码输入（60s 倒计时） | loginform.vue、Dashboard.vue、UserPage.vue |

### 6.3 移动端+部署（15 项修复，1 个新增文件）

| 项 | 修复内容 | 文件 |
|---|---------|------|
| M1 | BuildConfig 注入 API_BASE_URL（debug 10.0.2.2，release 可 -PapiBaseUrl 覆盖），MainActivity 最先调用 updateBaseUrl | androidApp/build.gradle.kts、MainActivity.kt |
| M2 | PlatformTokenStore 改自包含实现，消除 shared→androidApp 反向依赖（编译阻断） | PlatformTokenStore.kt |
| M3 | /cards/user/me 后端已新增，移动端调用生效 | （后端侧完成） |
| M4/M5 | 新增 PageResponse 泛型包装，/orders、/wallet/transactions 按分页结构解析 | PageResponse.kt、OrderApi.kt、WalletApi.kt |
| M6 | compose 固定子网 172.20.0.0/24 + nginx 固定 IP 172.20.0.3（与后端可信代理配置对齐）；后端 XFF 解析修复见 6.1 | docker-compose.yml、RateLimitFilter.java |
| M7 | CreateOrderRequest 补必填 pricingId | OrderApi.kt |
| M8/M12 | TokenStore 初始化与 Root 检测移 IO 协程并行，ready 前显示加载占位（修复冷启动丢登录态+主线程 I/O） | MainActivity.kt |
| M9 | .env.example 删除无效变量（ADMIN_PASSWORD 等），注明默认管理员由 DatabaseInitializer 创建 | .env.example |
| M10 | 三个 nginx 配置所有含 add_header 的 location 补齐完整安全头 | nginx*.conf |
| M11 | MySQL healthcheck 去掉命令行密码 | docker-compose.yml |
| M13 | .gitignore 补 *.jks/*.keystore/keystore.properties | .gitignore |
| M14 | logout 改无 body 调用（AuthApi/ViewModel/ProfileScreen 链路同步） | AuthApi.kt 等 |
| M15 | ProfileScreen 移除恒空的手机号/注册时间展示 | ProfileScreen.kt |
| M16 | 删除移动端 recharge 死代码 | WalletApi.kt、WalletViewModel.kt |
| M17 | CORS 默认值移除无效的 http://localhost:80 | docker-compose.yml |

### 6.4 记录不改（需产品/架构决策，共 7 项）

- B-6 部分：ApiKeyMachineSpecRedemption 实体接线（apiKeyId Long vs Integer 类型需先统一）
- B-15：TOTP 加密密钥改独立派生（会使存量 TOTP 失效，需迁移方案）；TOTP 重放保护需加表字段
- B-19：卡密签名改服务端全局 HMAC 密钥（远期架构升级）
- B-S10：匿名请求 access_logs 采样策略；B-S11：账号锁定按 IP+账号组合重构
- F17：客户端 AES 配对码方案重设计；F20：xlsx 0.18.5 升级需换 SheetJS CDN 渠道或 exceljs

---

## 7. 验证结果

| 验证项 | 命令 | 结果 |
|--------|------|------|
| 后端测试 | `mvn test` | ✅ BUILD SUCCESS，75 个测试全部通过（0 失败 0 错误）；适配 AuthServiceTest 4 处（验证码哈希入库、BindToken 原子消费语义） |
| 后端编译 | `mvn compile` | ✅ 通过（删除 KeyManagerService 后无残留引用） |
| 前端构建 | `npm run build` | ✅ 5.08s 构建成功（chunk 体积警告为既有问题，非本轮引入） |
| 移动端 | Gradle 无网络环境 | ⚠️ 按约束逐文件语法自检（import/序列化注解/签名链路交叉核对），完整编译需网络环境执行 `gradle :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlinAndroid` |

**部署注意**：
1. `docker compose down && docker compose up -d` 重建网络（compose 已固定子网/IP）
2. 升级后存量在途邮箱验证码（明文）将失效，用户需重新获取（10 分钟有效期，影响极小）
3. JwtRequestFilter 现校验 token 哈希：重新登录后旧 token 立即失效（预期行为变更）
4. 公开自助解绑与 /cards/verify 现强制机器码：对接的第三方客户端需确保传递机器码
5. 换绑邮箱需验证码：前端已同步，旧版第三方前端调 PUT /user/profile 换邮箱会收到明确错误提示

---

## 8. 审查方法论

- 4 路并行 subagent 深度审查（后端安全 / 后端业务 / 前端契约 / 移动端+DevOps），每路给出文件:行号与代码证据
- 交叉排除前三轮已修复项（2026-06-10 两轮 73 项 + 2026-06-20 三轮 114 项清单）
- 重点维度：授权矩阵核对（SecurityConfig vs 控制器逐一比对）、前后端/移动端契约比对、事务与并发正确性、交付链路（Docker/nginx/gradle）
