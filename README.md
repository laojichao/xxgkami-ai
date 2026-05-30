# 小小怪卡密验证系统 Pro

基于原项目 [xxgkami-pro](https://github.com/xiaoxiaoguai-yyds/xxgkami-pro) 的完整 Java 全栈实现，包含 Spring Boot 后端、Vue 3 前端、Android 客户端和 Kotlin Multiplatform 共享模块。

## 项目结构

```
xxgkami-ai/
├── backend/                # Spring Boot 后端 (Maven)
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/xxg/backend/backend/
│       │   ├── config/     # 安全配置、数据库初始化、CORS
│       │   ├── controller/ # REST 控制器
│       │   ├── dto/        # 数据传输对象
│       │   ├── entity/     # JPA 实体
│       │   ├── exception/  # 全局异常处理
│       │   ├── filter/     # JWT、速率限制、请求监控
│       │   ├── mapper/     # Spring Data Repository
│       │   ├── service/    # 业务逻辑
│       │   └── util/       # JWT、密码、加密工具
│       └── resources/
│           └── application.properties
├── frontend/               # Vue 3 前端 (Vite)
│   ├── package.json
│   └── src/
│       ├── components/     # 页面组件
│       ├── services/       # API 服务
│       ├── data/           # 模拟数据
│       └── utils/          # 工具函数
└── mobile/                 # Android/KMP 移动端 (Gradle)
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── androidApp/         # Android Jetpack Compose 应用
    │   ├── build.gradle.kts
    │   ├── proguard-rules.pro
    │   └── src/main/kotlin/com/xxgkami/android/
    │       ├── data/       # Token 持久化
    │       ├── navigation/ # 导航配置
    │       ├── ui/screens/ # 页面
    │       ├── ui/theme/   # 主题
    │       └── viewmodel/  # ViewModel
    └── shared/             # Kotlin Multiplatform 共享模块
        └── src/
            ├── commonMain/ # 跨平台代码
            ├── androidMain/# Android 平台实现
            ├── iosMain/    # iOS 平台实现
            └── desktopMain/# Desktop 平台实现
```

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3 + Spring Security + JPA + JWT + BCrypt + MySQL 8.0 |
| 前端 | Vue 3 + Vite + Element Plus |
| 共享模块 | Kotlin Multiplatform + Ktor Client + Kotlinx Serialization |
| Android | Jetpack Compose + Material 3 + Navigation Compose |

## 安全特性

- JWT 认证（HS256，强制配置密钥，启动校验长度）
- 登录/卡密验证速率限制（基于 IP）
- SSRF 防护（Webhook URL 内网地址校验）
- CORS 白名单校验（禁止通配符 + allowCredentials）
- 敏感配置脱敏（Settings API 不返回密钥明文）
- 命令注入防护（BackupService 参数校验）
- JSON 注入防护（共享模块使用 JsonObject 构建）
- Token 自动刷新（401 拦截器 + Mutex 防并发）
- Token 持久化（Android SharedPreferences）
- ProGuard 代码混淆（Android Release 构建）

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+

### 1. 数据库初始化

```sql
CREATE DATABASE kami DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后端后 JPA 会自动创建表结构（`ddl-auto=update`）。

### 2. 启动后端

```bash
cd backend

# 必须配置环境变量
export JWT_SECRET=your-secret-key-at-least-32-chars
export DB_PASSWORD=your-database-password

# 可选配置
export DB_URL=jdbc:mysql://localhost:3306/kami?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
export DB_USERNAME=root
export CORS_ALLOWED_ORIGINS=http://localhost:5173

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
cd mobile
./gradlew :androidApp:assembleDebug
```

APK 生成在 `mobile/androidApp/build/outputs/apk/debug/`

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
| POST | `/auth/refresh-token` | 刷新 Token | 否 |
| POST | `/auth/logout` | 退出登录 | 是 |
| GET | `/auth/user/info` | 获取用户信息 | 是 |
| POST | `/auth/totp/setup` | TOTP 配置 | 是 |
| POST | `/auth/totp/enable` | 启用 TOTP | 是 |
| POST | `/auth/totp/disable` | 禁用 TOTP | 是 |

### 卡密管理 `/cards`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/cards/use` | 使用卡密 | 否 |
| POST | `/cards/verify` | 验证卡密 | 否 |
| POST | `/cards/generate` | 生成卡密 | ADMIN |
| GET | `/cards/admin` | 管理员卡密分页 | ADMIN |
| PATCH | `/cards/admin/{id}/status` | 更新卡密状态 | ADMIN |
| GET | `/cards/user/{userId}` | 用户卡密列表 | 是 |
| DELETE | `/cards/{id}` | 删除卡密 | 是 |
| PUT | `/cards/{id}/disable` | 停用卡密 | 是 |
| PUT | `/cards/{id}/enable` | 启用卡密 | 是 |
| PUT | `/cards/{id}/unbind` | 解绑机器码 | 是 |
| GET | `/cards/stats` | 卡密统计 | ADMIN |

### 订单管理 `/orders`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/orders` | 创建订单 | 是 |
| GET | `/orders` | 用户订单 | 是 |
| GET | `/orders/admin/all` | 管理员全部订单 | ADMIN |
| POST | `/orders/admin/updateStatus` | 更新订单状态 | ADMIN |
| GET | `/orders/{orderNo}` | 订单详情 | 是 |
| GET | `/orders/stats` | 订单统计 | ADMIN |

### 用户管理 `/user` `/admin/users`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/user/profile` | 获取个人资料 | 是 |
| PUT | `/user/profile` | 更新个人资料 | 是 |
| POST | `/user/password` | 修改密码 | 是 |
| GET | `/admin/users` | 管理员用户列表 | ADMIN |
| PUT | `/admin/users/{id}` | 更新用户 | ADMIN |
| DELETE | `/admin/users/{id}` | 删除用户 | ADMIN |
| PUT | `/admin/users/{id}/status` | 更新用户状态 | ADMIN |

### API 密钥 `/admin/apikeys`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/admin/apikeys` | 密钥列表 | ADMIN |
| POST | `/admin/apikeys` | 创建密钥 | ADMIN |
| PUT | `/admin/apikeys/{id}` | 更新密钥 | ADMIN |
| DELETE | `/admin/apikeys/{id}` | 删除密钥 | ADMIN |

### 钱包管理 `/wallet`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/wallet` | 获取钱包 | 是 |
| POST | `/wallet/recharge` | 余额充值 | 是 |
| GET | `/wallet/transactions` | 交易记录 | 是 |

### 安全中心 `/security`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/security/blacklist` | IP 黑名单 | ADMIN |
| POST | `/security/blacklist` | 添加封禁 | ADMIN |
| DELETE | `/security/blacklist/{ip}` | 解除封禁 | ADMIN |
| GET | `/security/access-logs` | 访问日志 | ADMIN |

### 系统设置 `/settings`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/settings/all` | 获取全部设置（敏感字段脱敏） | ADMIN |
| POST | `/settings/save` | 保存设置 | ADMIN |
| POST | `/settings/email/test` | 发送测试邮件 | ADMIN |

### 维护与备份 `/maintenance` `/backup`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/maintenance/status` | 维护状态 | 否 |
| POST | `/maintenance/update` | 更新维护设置 | ADMIN |
| POST | `/backup/create` | 创建备份 | ADMIN |

### 支付 `/payment`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/payment/pay` | 发起支付 | 是 |
| POST | `/payment/notify` | 支付回调 | 否 |
| GET | `/payment/return` | 支付返回 | 否 |

### 公开接口 `/public`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/public/features` | 首页特性 | 否 |
| GET | `/public/slides` | 轮播图 | 否 |
| POST | `/public/cards/machine-bind/query` | 查询机器码绑定 | 否 |
| POST | `/public/cards/machine-bind/unbind` | 自助解绑机器码 | 否 |

---

## 默认账号

- 管理员: `admin` / `admin123`

## 开源协议

MIT License
