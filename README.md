# 小小怪卡密验证系统 Pro - Java 全栈实现

基于原项目 [xxgkami-pro](https://github.com/xiaoxiaoguai-yyds/xxgkami-pro) 的完整 Java 全栈实现，包含后端、前端、Android 客户端和 Kotlin Multiplatform 共享模块。

## 项目结构

```
xxgkami-java/
├── backend/                    # Java Spring Boot 后端
├── frontend/                   # Vue 3 前端 (原仓库代码)
├── shared/                     # Kotlin Multiplatform 共享模块
├── androidApp/                 # Android Jetpack Compose 客户端
├── build.gradle.kts            # Gradle 根配置
├── settings.gradle.kts         # Gradle 设置
├── kami.sql                    # 数据库初始化脚本
└── README.md
```

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3 + JPA + JWT + BCrypt + MySQL 8.0 |
| 前端 | Vue 3 + Vite + Element Plus + ECharts |
| 共享模块 | Kotlin Multiplatform + Ktor Client + Kotlinx Serialization |
| Android | Jetpack Compose + Material 3 + Navigation |

## 快速开始

### 环境要求

- JDK 20+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+

### 1. 数据库初始化

```sql
CREATE DATABASE kami DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

导入 `kami.sql` 初始化表结构和默认数据。

### 2. 启动后端

```bash
cd backend
# 修改 src/main/resources/application.properties 中的数据库连接
mvn clean package -DskipTests
java -jar target/xxgkami-backend-1.0.2.jar
```

后端运行在 `http://localhost:8080/api`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173/`，自动代理 `/api` 到后端。

### 4. 构建 Android (可选)

```bash
./gradlew :androidApp:assembleDebug
```

APK 生成在 `androidApp/build/outputs/apk/debug/`

---

## 后端 API 接口

### 认证接口 `/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/auth/admin/login` | 管理员登录 | 否 |
| POST | `/auth/user/login` | 用户登录 | 否 |
| POST | `/auth/register` | 用户注册 | 否 |
| POST | `/auth/register-bind` | 绑定注册 | 否 |
| POST | `/auth/email-code` | 发送验证码 | 否 |
| POST | `/auth/reset-code` | 发送重置验证码 | 否 |
| POST | `/auth/reset-password` | 重置密码 | 否 |
| POST | `/auth/refresh` | 刷新 Token | 否 |
| POST | `/auth/logout` | 退出登录 | 是 |
| GET | `/auth/user/info` | 获取用户信息 | 是 |
| GET | `/auth/bind/token` | 获取绑定 Token | 是 |
| POST | `/auth/bind/validate` | 验证绑定 Token | 否 |
| POST | `/auth/totp/setup` | TOTP 配置 | 是 |
| POST | `/auth/totp/enable` | 启用 TOTP | 是 |
| POST | `/auth/totp/disable` | 禁用 TOTP | 是 |

### 卡密管理 `/cards`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/cards/use` | 使用卡密 | 否 |
| POST | `/cards/verify` | 验证卡密 | 否 |
| POST | `/cards/admin/create` | 管理员创建卡密 | 是 |
| POST | `/cards/generate` | 生成卡密 | 是 |
| GET | `/cards/admin/all` | 管理员全部卡密 | 是 |
| GET | `/cards/admin` | 管理员卡密分页 | 是 |
| PUT | `/cards/admin/{id}` | 编辑卡密 | 是 |
| PATCH | `/cards/admin/{id}/status` | 更新卡密状态 | 是 |
| GET | `/cards/user/{userId}` | 用户卡密列表 | 是 |
| GET | `/cards/apikey/{apiKeyId}` | API Key 卡密 | 是 |
| GET | `/cards/trend` | 使用趋势 | 是 |
| DELETE | `/cards/{id}` | 删除卡密 | 是 |
| PUT | `/cards/{id}/disable` | 停用卡密 | 是 |
| PUT | `/cards/{id}/enable` | 启用卡密 | 是 |
| PUT | `/cards/{id}/unbind` | 解绑机器码 | 是 |
| GET | `/cards/stats` | 卡密统计 | 是 |

### 订单管理 `/orders`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/orders` | 创建订单 | 是 |
| GET | `/orders?userId=X` | 用户订单 | 是 |
| GET | `/orders/admin` | 管理员订单 | 是 |
| GET | `/orders/admin/all` | 管理员全部订单 | 是 |
| POST | `/orders/admin/updateStatus` | 更新订单状态 | 是 |
| GET | `/orders/{orderNo}` | 订单详情 | 是 |
| PUT | `/orders/{orderNo}/complete` | 完成订单 | 是 |

### 用户管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/user/profile` | 获取个人资料 | 是 |
| PUT | `/user/profile` | 更新个人资料 | 是 |
| POST | `/user/password` | 修改密码 | 是 |
| POST | `/user/avatar` | 上传头像 | 是 |
| GET | `/user/social` | 社交绑定列表 | 是 |
| POST | `/user/social/bind` | 绑定社交账号 | 是 |
| POST | `/user/social/unbind` | 解绑社交账号 | 是 |
| GET | `/admin/users` | 管理员用户列表 | 是 |
| POST | `/admin/users` | 创建用户 | 是 |
| PUT | `/admin/users/{id}` | 更新用户 | 是 |
| DELETE | `/admin/users/{id}` | 删除用户 | 是 |
| PUT | `/admin/users/{id}/status` | 更新用户状态 | 是 |

### API 密钥 `/admin/apikeys`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/admin/apikeys` | 密钥列表 | 是 |
| POST | `/admin/apikeys` | 创建密钥 | 是 |
| PUT | `/admin/apikeys/{id}` | 更新密钥 | 是 |
| DELETE | `/admin/apikeys/{id}` | 删除密钥 | 是 |
| POST | `/admin/apikeys/{id}/users` | 分配用户 | 是 |
| DELETE | `/admin/apikeys/{id}/users/{userId}` | 取消分配 | 是 |

### 定价管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/pricing` | 定价列表 | 是 |
| POST | `/pricing` | 添加定价 | 是 |
| PUT | `/pricing/{id}` | 更新定价 | 是 |
| DELETE | `/pricing/{id}` | 删除定价 | 是 |

### 系统设置

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/settings/all` | 获取全部设置 | 是 |
| POST | `/settings/save` | 保存设置 | 是 |
| POST | `/settings/email/test` | 发送测试邮件 | 是 |

### 系统监控 `/monitor`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/monitor/all` | 全部监控数据 | 是 |
| GET | `/monitor/system` | 系统状态 | 是 |
| GET | `/monitor/database` | 数据库状态 | 是 |
| GET | `/monitor/api` | API 状态 | 是 |
| GET | `/monitor/users` | 在线用户 | 是 |

### 在线用户 `/online`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/online/list` | 在线用户列表 | 是 |
| POST | `/online/login` | 用户上线 | 是 |
| POST | `/online/logout` | 用户下线 | 是 |
| POST | `/online/heartbeat` | 心跳更新 | 是 |
| GET | `/online/check/{userId}` | 检查在线状态 | 是 |

### 维护与备份

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/maintenance/status` | 维护状态 | 是 |
| POST | `/maintenance/update` | 更新维护设置 | 是 |
| POST | `/maintenance/clear-cache` | 清理缓存 | 是 |
| POST | `/maintenance/clear-logs` | 清理日志 | 是 |
| POST | `/backup/create` | 创建备份 | 是 |

### 支付 `/payment`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/payment/pay` | 发起支付 | 是 |
| POST | `/payment/create` | 创建支付 | 是 |
| POST | `/payment/notify` | 支付回调 | 否 |
| GET | `/payment/return` | 支付返回 | 否 |

### 公开接口 `/public`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/public/features` | 首页特性 | 否 |
| GET | `/public/slides` | 轮播图 | 否 |
| POST | `/public/cards/machine-bind/query` | 查询机器码绑定 | 否 |
| POST | `/public/cards/machine-bind/unbind` | 自助解绑机器码 | 否 |

### 钱包管理 `/wallet` [新增]

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/wallet` | 获取钱包 | 是 |
| POST | `/wallet/recharge` | 余额充值 | 是 |
| GET | `/wallet/transactions` | 交易记录 | 是 |

### 安全中心 `/security` [新增]

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/security/blacklist` | IP 黑名单 | 是 |
| POST | `/security/blacklist` | 添加封禁 | 是 |
| DELETE | `/security/blacklist/{ip}` | 解除封禁 | 是 |
| GET | `/security/access-logs` | 访问日志 | 是 |

---

## 前端说明

前端代码直接使用原仓库 [xxgkami-pro](https://github.com/xiaoxiaoguai-yyds/xxgkami-pro) 的 Vue 3 前端。

**主要页面:**
- 首页 - 轮播图、特性展示、卡密验证
- 管理后台 - 数据概览、卡密管理、订单管理、用户管理、API 密钥、定价管理、系统设置、安全中心、系统监控
- 用户中心 - 个人面板、我的卡密、购买中心、订单、个人资料

**启动:**
```bash
cd frontend
npm install
npm run dev
```

## Android 客户端

基于 Jetpack Compose + Material 3 构建，使用 KMP 共享模块的 API 客户端。

**主要页面:**
- 首页 - 卡密验证入口
- 登录/注册 - 用户认证
- 我的卡密 - 卡密列表
- 订单 - 订单记录
- 钱包 - 余额管理

**构建:**
```bash
./gradlew :androidApp:assembleDebug
```

## Kotlin Multiplatform 共享模块

跨平台共享模块，支持 Android、iOS、Desktop。

**模块结构:**
- `api/` - Ktor HTTP 客户端 (ApiClient, AuthApi, CardApi, OrderApi, WalletApi)
- `model/` - 数据模型 (User, Card, Order, Wallet, ApiResponse)
- `util/` - 工具类 (TokenManager)

## 默认账号

- 管理员: `admin` / `admin123`

## 开源协议

MIT License
