# xxgkami-ai (小小怪卡密 Pro)

## 项目概述
全栈卡密验证系统 "小小怪卡密 Pro"。支持管理员/用户角色、卡密生成/验证/绑定、订单管理、钱包/充值、API Key 管理、支付集成、安全功能 (IP 黑名单/限流/TOTP 2FA)、系统设置、维护模式、数据备份。包含 Kotlin Multiplatform 移动客户端。

## 技术栈
- **后端**: Spring Boot 3.3.5 + Java 17 + Spring Security + JPA + JWT (JJWT 0.12.6) + BCrypt + Argon2 + MySQL 8
- **前端**: Vue 3 + Vite + Element Plus + CryptoJS + QRCode + SheetJS
- **移动端**: Kotlin Multiplatform (KMP 2.0.21) + Jetpack Compose + Ktor Client + Kotlinx Serialization
- **构建**: Maven (后端), npm/Vite (前端), Gradle Kotlin DSL (移动端)

## 项目结构
```
├── backend/               # Spring Boot 后端
│   ├── 12 个 REST 控制器 (Auth, Card, Order, User, ApiKey, Settings, Security, Maintenance, Monitor, Payment, Public, CardPricing)
│   ├── 实体/DTO/JWT 加密工具
│   ├── 限流过滤器/CORS 配置
│   └── 数据库自动初始化
├── frontend/              # Vue 3 SPA
│   ├── 17 个 Vue 组件
│   ├── Dashboard, HomePage, KeysManagePage, OrdersManagePage
│   ├── UserManagePage, SettingsPage, ApiManagePage
│   └── PricingManagePage, MaintenanceAdmin, OnlineUnbindPage
└── mobile/                # KMP 移动客户端
    ├── Android (Jetpack Compose + Material 3)
    ├── iOS/Desktop 源集
    ├── Login, Home, MyCards, Orders 页面
    └── Token 持久化 (SharedPreferences)
```

## 核心功能
- 卡密生命周期管理 (生成/验证/绑定/解绑)
- 订单与支付管理
- 钱包与充值系统
- API Key 管理
- 安全功能 (IP 黑名单、限流、TOTP 2FA)
- 维护模式与数据备份
- 多平台客户端 (Web + Android + iOS + Desktop)

## 开发注意事项
- 密码哈希使用 BCrypt + Argon2 双重加密
- JWT 使用 JJWT 0.12.6
- 前端使用 CryptoJS 进行客户端加密
- KMP 移动端共享业务逻辑
- 数据库自动初始化脚本
