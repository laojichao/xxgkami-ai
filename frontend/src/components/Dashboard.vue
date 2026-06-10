<template>
  <div class="dashboard" :class="{ 'dashboard-sidebar-collapsed': sidebarCollapsed }">
    <!-- 导航栏组件 -->
    <NavigationBar
      :user-info="userInfo"
      :active-tab="activeTab"
      @tab-change="handleTabChange"
      @logout="handleLogout"
      @collapse-change="handleSidebarCollapse"
    />

    <!-- 卡密创建进度条 -->
    <div v-if="createProgress.visible" class="create-progress-bar">
      <div class="progress-content">
        <div class="progress-info">
          <span class="progress-icon">
            <svg v-if="createProgress.done" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5" width="18" height="18"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            <svg v-else class="spinning" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="18" height="18"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"></path></svg>
          </span>
          <span class="progress-text">
            <template v-if="createProgress.done">
              全部创建完成！成功 {{ createProgress.success }} 条<template v-if="createProgress.fail > 0">，失败 {{ createProgress.fail }} 条</template>
            </template>
            <template v-else>
              正在创建卡密... {{ createProgress.current }} / {{ createProgress.total }}（剩余 {{ createProgress.total - createProgress.current }} 条）
            </template>
          </span>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: createProgress.percent + '%' }"></div>
        </div>
        <button v-if="createProgress.done" class="progress-close" @click="createProgress.visible = false">&times;</button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <main class="dashboard-main">
      <!-- 概览页面 -->
      <OverviewPage
        v-if="activeTab === 'overview'"
        :stats="stats"
        :features="features"
      />

      <!-- 卡密管理页面 -->
      <KeysManagePage
        v-if="activeTab === 'keys'"
        :keys="keys"
        @create-keys="handleCreateKeys"
        @delete-key="handleDeleteKey"
        @toggle-key-status="handleToggleKeyStatus"
        @update-key="handleUpdateKey"
      />

      <!-- 定价管理页面 -->
      <PricingManagePage
        v-if="activeTab === 'pricing'"
      />

      <!-- 订单管理页面 -->
      <OrdersManagePage
        v-if="activeTab === 'orders'"
      />

      <!-- API管理页面 -->
      <ApiManagePage
        v-if="activeTab === 'api'"
        :api-keys="apiKeys"
        @generate-api-key="handleGenerateApiKey"
        @delete-api-key="handleDeleteApiKey"
        @update-api-key="handleUpdateApiKey"
        @toggle-api-key="handleToggleApiKey"
      />

      <!-- 用户管理页面 -->
      <UserManagePage
        v-if="activeTab === 'users'"
      />

      <!-- 通知管理页面 -->
      <NotificationPage
        v-if="activeTab === 'notification'"
      />

      <!-- 系统设置页面 -->
      <SettingsPage
        v-if="activeTab === 'settings'"
        :user-info="userInfo"
        @save-settings="handleSaveSettings"
        @clear-cache="handleClearCache"
        @optimize-database="handleOptimizeDatabase"
        @clear-logs="handleClearLogs"
        @create-backup="handleCreateBackup"
      />

      <!-- 系统维护页面 -->
      <MaintenanceAdmin
        v-if="activeTab === 'maintenance'"
      />

      <!-- 系统信息页面 -->
      <SystemInfo
        v-if="activeTab === 'system_info'"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { cardApi, statsApi, apiKeyApi, publicApi, settingsApi, maintenanceApi } from '../services/api.js'
import { ElMessage } from 'element-plus'
import NavigationBar from './NavigationBar.vue'

// 首屏组件直接加载
import OverviewPage from './OverviewPage.vue'

// 非首屏组件懒加载，减少初始打包体积
const KeysManagePage = defineAsyncComponent({
  loader: () => import('./KeysManagePage.vue'),
  delay: 200,
  timeout: 15000
})
const PricingManagePage = defineAsyncComponent({
  loader: () => import('./PricingManagePage.vue'),
  delay: 200,
  timeout: 15000
})
const OrdersManagePage = defineAsyncComponent({
  loader: () => import('./OrdersManagePage.vue'),
  delay: 200,
  timeout: 15000
})
const ApiManagePage = defineAsyncComponent({
  loader: () => import('./ApiManagePage.vue'),
  delay: 200,
  timeout: 15000
})
const UserManagePage = defineAsyncComponent({
  loader: () => import('./UserManagePage.vue'),
  delay: 200,
  timeout: 15000
})
const SettingsPage = defineAsyncComponent({
  loader: () => import('./SettingsPage.vue'),
  delay: 200,
  timeout: 15000
})
const NotificationPage = defineAsyncComponent({
  loader: () => import('./NotificationPage.vue'),
  delay: 200,
  timeout: 15000
})
const MaintenanceAdmin = defineAsyncComponent({
  loader: () => import('./MaintenanceAdmin.vue'),
  delay: 200,
  timeout: 15000
})
const SystemInfo = defineAsyncComponent({
  loader: () => import('./SystemInfo.vue'),
  delay: 200,
  timeout: 15000
})

const props = defineProps({
  userInfo: Object
})

const emit = defineEmits(['logout'])

// 响应式数据
const activeTab = ref('overview')
/** 与 NavigationBar 折叠状态同步，用于主区域 padding 动画 */
const sidebarCollapsed = ref(false)

const handleSidebarCollapse = (collapsed) => {
  sidebarCollapsed.value = collapsed
}
const stats = reactive({
  totalKeys: 0,
  usedKeys: 0,
  activeKeys: 0,
  totalUsers: 0
})

const features = ref([])
const keys = ref([])
const apiKeys = ref([])

const createProgress = reactive({
  visible: false,
  current: 0,
  total: 0,
  success: 0,
  fail: 0,
  done: false,
  percent: 0
})


// 方法
const handleLogout = () => {
  emit('logout')
}

const handleTabChange = (tab, section) => {
  activeTab.value = tab

  if (section && tab === 'settings') {
    setTimeout(() => {
      const element = document.getElementById('settings-' + section)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }, 100)
  }
}

const formatDate = (timestamp) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

const handleCreateKeys = async (keyData) => {
  const totalCount = keyData.count || 1

  // 少量（≤3）直接批量创建，不显示进度条
  if (totalCount <= 3) {
    try {
      const result = await cardApi.createCards(keyData)
      if (result.success) {
        await loadKeys()
        ElMessage.success(`成功创建 ${totalCount} 条卡密`)
      }
    } catch (error) {
      console.error('生成卡密失败:', error)
      ElMessage.error('生成卡密失败: ' + error.message)
    }
    return
  }

  // 逐条创建，显示进度条
  createProgress.visible = true
  createProgress.current = 0
  createProgress.total = totalCount
  createProgress.success = 0
  createProgress.fail = 0
  createProgress.done = false
  createProgress.percent = 0

  const singleData = { ...keyData, count: 1 }

  for (let i = 0; i < totalCount; i++) {
    try {
      const result = await cardApi.createCards(singleData)
      if (result.success) {
        createProgress.success++
      } else {
        createProgress.fail++
      }
    } catch (error) {
      console.error(`创建第 ${i + 1} 条失败:`, error)
      createProgress.fail++
    }
    createProgress.current = i + 1
    createProgress.percent = Math.round(((i + 1) / totalCount) * 100)
  }

  createProgress.done = true
  await loadKeys()

  // 5 秒后自动关闭进度条
  setTimeout(() => {
    if (createProgress.done) {
      createProgress.visible = false
    }
  }, 5000)
}

const handleDeleteKey = async (keyId) => {
  try {
    const result = await cardApi.deleteCard(keyId)
    if (result.success) {
      await loadKeys()
      ElMessage.success(result.message)
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    console.error('删除卡密失败:', error)
    ElMessage.error('删除卡密失败')
  }
}

const handleUpdateKey = async (keyData) => {
  try {
    const result = await cardApi.updateCard(keyData.id, keyData)
    if (result.success) {
      ElMessage.success(result.message || '卡密更新成功')
      await loadKeys()
    } else {
      ElMessage.error(result.message || '更新失败')
    }
  } catch (error) {
    console.error('更新卡密失败:', error)
    ElMessage.error(error.message || '更新卡密失败')
  }
}

const handleToggleKeyStatus = async ({ id, status }) => {
  try {
    const result = await cardApi.updateAdminStatus(id, status)
    if (result.success) {
      const msg = result.message || '操作成功'
      if (status === 2 && msg.includes('停止使用')) {
        ElMessage.warning(msg)
      } else {
        ElMessage.success(msg)
      }
      await loadKeys()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    console.error('更新卡密状态失败:', error)
    ElMessage.error(error.message || '更新卡密状态失败')
  }
}

// 加载统计数据
const loadDashboardStats = async () => {
  try {
    const result = await statsApi.getDashboardStats()
    if (result && result.success) {
      stats.totalKeys = result.totalKeys ?? result.data?.totalKeys ?? 0
      stats.usedKeys = result.usedKeys ?? result.data?.usedKeys ?? 0
      stats.activeKeys = result.activeKeys ?? result.data?.activeKeys ?? 0
      stats.totalUsers = result.totalUsers ?? result.data?.totalUsers ?? 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载卡密数据
const loadKeys = async () => {
  try {
    const result = await cardApi.getAllCards()
    if (result.success) {
      keys.value = result.data
    }
    // 加载统计数据
    await loadDashboardStats()
  } catch (error) {
    console.error('加载卡密数据失败:', error)
  }
}

const handleGenerateApiKey = async () => {
  try {
    const res = await apiKeyApi.createApiKey({ name: `API密钥 ${apiKeys.value.length + 1}` })
    if (res.success) {
      apiKeys.value.push(res.data)
      ElMessage.success('API Key 创建成功')
    }
  } catch (error) {
    ElMessage.error('创建失败: ' + error.message)
  }
}

const handleDeleteApiKey = async (keyId) => {
  try {
    await apiKeyApi.deleteApiKey(keyId)
    apiKeys.value = apiKeys.value.filter(key => key.id !== keyId)
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error('删除失败: ' + error.message)
  }
}

const handleUpdateApiKey = async (updatedKey) => {
  try {
    await apiKeyApi.updateApiKey(updatedKey.id, updatedKey)
    const index = apiKeys.value.findIndex(key => key.id === updatedKey.id)
    if (index !== -1) {
      apiKeys.value[index] = { ...apiKeys.value[index], ...updatedKey }
    }
    ElMessage.success('更新成功')
  } catch (error) {
    ElMessage.error('更新失败: ' + error.message)
  }
}

const handleToggleApiKey = async (keyId) => {
  const key = apiKeys.value.find(key => key.id === keyId)
  if (key) {
    try {
      await apiKeyApi.updateApiKey(keyId, { status: !key.isActive })
      key.isActive = !key.isActive
    } catch (error) {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

const handleSaveSettings = async (settingsData) => {
  try {
    await settingsApi.saveSettings(settingsData)
    ElMessage.success('设置已保存')
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  }
}

const handleClearCache = async () => {
  try {
    await maintenanceApi.clearCache()
    ElMessage.success('缓存已清理')
  } catch (error) {
    ElMessage.error('清理失败: ' + error.message)
  }
}

const handleOptimizeDatabase = () => {
  ElMessage.info('数据库优化功能暂不可用')
}

const handleClearLogs = async () => {
  try {
    await maintenanceApi.clearLogs()
    ElMessage.success('日志已清理')
  } catch (error) {
    ElMessage.error('清理失败: ' + error.message)
  }
}

const handleCreateBackup = async () => {
  try {
    await maintenanceApi.createBackup()
    ElMessage.success('备份创建成功')
  } catch (error) {
    ElMessage.error('备份失败: ' + error.message)
  }
}


// 初始化数据
onMounted(async () => {
  // 从 API 加载数据
  try {
    const [featuresRes, apiKeysRes] = await Promise.all([
      publicApi.getFeatures().catch(() => ({ data: [] })),
      apiKeyApi.getAllApiKeys().catch(() => ({ data: [] }))
    ])
    features.value = featuresRes.data || []
    apiKeys.value = apiKeysRes.data || []
  } catch (e) {
    console.error('加载初始数据失败:', e)
  }

  // 异步加载卡密数据
  await loadKeys()
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: #f9fafb;
  width: 100%;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: row;
  padding-left: 220px;
  transition: padding-left 0.3s ease;
  box-sizing: border-box;
}

.dashboard.dashboard-sidebar-collapsed {
  padding-left: 64px;
}

.dashboard-main {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  flex: 1;
}

@media (max-width: 768px) {
  .dashboard,
  .dashboard.dashboard-sidebar-collapsed {
    padding-left: 64px;
  }

  .dashboard-main {
    padding: 1rem;
  }
}

/* 创建进度条 */
.create-progress-bar {
  position: fixed;
  top: 0;
  left: 220px;
  right: 0;
  z-index: 999;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  animation: slideDown 0.3s ease;
  transition: left 0.3s ease;
}

.dashboard.dashboard-sidebar-collapsed .create-progress-bar {
  left: 64px;
}

@keyframes slideDown {
  from { transform: translateY(-100%); opacity: 0; }
  to   { transform: translateY(0); opacity: 1; }
}

@media (max-width: 768px) {
  .create-progress-bar {
    left: 64px;
  }
}

.progress-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 12px 2rem;
  position: relative;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.progress-icon {
  display: flex;
  align-items: center;
  color: #2563eb;
}

.spinning {
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.progress-text {
  font-size: 0.85rem;
  font-weight: 500;
  color: #374151;
}

.progress-track {
  width: 100%;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #0ea5e9);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-close {
  position: absolute;
  top: 8px;
  right: 2rem;
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #9ca3af;
  cursor: pointer;
  padding: 4px 8px;
  line-height: 1;
  border-radius: 4px;
}

.progress-close:hover {
  background: #f3f4f6;
  color: #374151;
}
</style>
