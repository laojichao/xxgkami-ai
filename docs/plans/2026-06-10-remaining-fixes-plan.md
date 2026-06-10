# 剩余问题修复计划

**日期**: 2026-06-10
**前置条件**: 已完成 30 项初步修复并提交
**预计工作量**: 8-12 小时

---

## 任务清单

### 阶段一：提交当前修复（5 分钟）

**任务 1.1**: 提交已有的 30 项修复
- 操作: `git add -A && git commit`
- Commit message: `fix: comprehensive security, concurrency, and UX fixes across entire codebase`
- 验证: `git log --oneline -1` 确认提交成功

---

### 阶段二：高优先级安全修复（2-3 小时）

**任务 2.1**: JWT Token Hash 存储（后端）
- 文件: `AuthService.java`, `Admin.java`, `User.java`
- 修改:
  - 登录时生成 token 后，存储 SHA256(token) 而非明文
  - 刷新 token 时验证 hash 而非明文比较
  - 添加 `hashToken()` 工具方法
- 验证: 登录/刷新/登出流程正常工作

**任务 2.2**: Bind Token 持久化（后端）
- 文件: `AuthService.java`, 新增 `BindToken.java` 实体
- 修改:
  - 创建 bind_tokens 表（token, user_id, expire_time）
  - 将内存 Map 改为数据库存储
  - 添加定时清理过期 token 的任务
- 验证: 绑定流程正常工作，重启后 token 仍有效

**任务 2.3**: 前端 Token 存储安全加固
- 文件: `api.js`, `loginform.vue`
- 修改:
  - 添加 token 内存缓存层，减少 localStorage 访问
  - 添加 XSS 检测：如果 token 被篡改，强制登出
  - 敏感操作（如支付）前重新验证 token 有效性
- 验证: 登录/刷新/登出流程正常工作

---

### 阶段三：中优先级修复（3-4 小时）

**任务 3.1**: Lombok @Data 替换（后端）
- 文件: 所有 entity/*.java
- 修改:
  - 将 @Data 替换为 @Getter @Setter
  - 为每个实体添加基于 @Id 的 equals/hashCode
  - 保留 toString 但排除关联字段
- 验证: 编译通过，JPA 查询正常

**任务 3.2**: 重复端点清理（后端）
- 文件: `PaymentController.java`, `SettingsController.java`, `MaintenanceController.java`
- 修改:
  - 标记废弃端点为 @Deprecated
  - 添加重定向到主端点
  - 更新前端 API 调用
- 验证: 前端功能正常

**任务 3.3**: 前端日志统一（前端）
- 文件: 新增 `utils/logger.js`, 修改所有组件
- 修改:
  - 创建统一的日志工具类
  - 生产环境自动禁用 console.error
  - 添加错误上报接口（可选）
- 验证: 生产构建无 console 输出

**任务 3.4**: Ktor 版本升级（移动端）
- 文件: `shared/build.gradle.kts`
- 修改:
  - 将 Ktor 版本从 2.3.12 升级到 2.3.13+
  - 更新 API 变更（如有）
  - 运行测试验证兼容性
- 验证: 编译通过，API 调用正常

---

### 阶段四：低优先级优化（2-3 小时）

**任务 4.1**: Element Plus 图标按需导入（前端）
- 文件: `main.js`, 各组件
- 修改:
  - 分析项目中实际使用的图标
  - 创建图标注册文件
  - 移除全量导入
- 验证: 所有图标正常显示，打包体积减小

**任务 4.2**: 移动端 ViewModel 缓存
- 文件: 各 ViewModel, 各 Screen
- 修改:
  - 在 ViewModel 中添加数据缓存
  - 使用 StateFlow 的 distinctUntilChanged
  - 添加手动刷新机制
- 验证: 页面切换不再重复请求

**任务 4.3**: 大组件拆分（前端）
- 文件: `ApiManagePage.vue`, `KeysManagePage.vue`, `UserPage.vue`
- 修改:
  - 将 3000+ 行组件拆分为 5-10 个子组件
  - 使用 provide/inject 共享状态
  - 保持功能不变
- 验证: 所有功能正常工作

---

## 依赖关系

```
任务 1.1 (提交) → 任务 2.1-2.3 (安全修复) → 任务 3.1-3.4 (中优先级) → 任务 4.1-4.3 (优化)
```

## 验证标准

每个任务完成后必须满足：
1. 编译/构建通过
2. 相关功能手动测试通过
3. 无新增警告或错误
4. 代码符合项目规范

## 回滚策略

每个任务独立提交，如果出现问题：
1. `git revert <commit>` 回滚单个任务
2. 不影响其他已完成的任务
