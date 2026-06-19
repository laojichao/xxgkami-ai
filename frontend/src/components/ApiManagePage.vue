<!-- API密钥管理页面：管理API密钥的增删改查、卡密分配、用户绑定、接口回调配置 -->
<template>
  <div class="api-manage-page">
    <!-- 页面头部：标题 + 代码实例/接口文档/生成密钥按钮 -->
    <div class="section-header">
      <h2>API密钥管理</h2>
      <div class="header-actions">
        <button type="button" class="btn-secondary" title="查看多语言调用核销接口示例" @click="showCodeExamplesModal = true">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
          代码实例
        </button>
        <button class="btn-secondary" @click="showDocsModal = true">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          接口文档
        </button>
        <button class="btn-primary" @click="showCreateModal = true">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"></path></svg>
          生成API密钥
        </button>
      </div>
    </div>

    <!-- API密钥卡片列表 -->
    <div class="api-keys-list">
      <ApiKeyCard
        v-for="apiKey in apiKeys"
        :key="apiKey.id"
        :api-key="apiKey"
        @copy-key="copyApiKey"
        @manage-cards="manageCardCodes"
        @interface-settings="openInterfaceSettings"
        @manage-users="manageUsers"
        @edit="editApiKey"
        @toggle="toggleApiKey"
        @delete="deleteApiKey"
      />

      <div v-if="apiKeys.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"></path></svg>
        </div>
        <h3>暂无API密钥</h3>
        <p>点击上方按钮生成您的第一个API密钥</p>
      </div>
    </div>

    <!-- 子组件：各种弹窗 -->
    <CreateApiKeyDialog
      v-model:visible="showCreateModal"
      @create="handleCreateApiKey"
    />

    <EditApiKeyDialog
      v-model:visible="showEditModal"
      :api-key="editingKeyData"
      @save="handleSaveApiKey"
    />

    <CardCodesDialog
      v-model:visible="showCardCodesModal"
      :api-key-name="currentApiKey.name"
      :card-codes="currentApiKey.cardCodes"
      :enable-card-encryption="currentApiKey.enableCardEncryption"
      @generate="handleGenerateCardCodes"
      @copy-card="copyCardCode"
      @copy-encrypted-card="copyEncryptedCardCode"
      @delete-card="deleteCardCode"
    />

    <UserManageDialog
      v-model:visible="showUsersModal"
      :api-key-name="currentApiKey.name"
      :available-users="availableUsers"
      :assigned-users="currentApiKey.assignedUsers"
      @assign="handleAssignUser"
      @unassign="handleUnassignUser"
    />

    <ApiDocsDialog
      v-model:visible="showDocsModal"
    />

    <CodeExamplesDialog
      v-model:visible="showCodeExamplesModal"
      @copy-code="handleCopyUseCardCode"
    />

    <InterfaceSettingsDialog
      v-model:visible="showInterfaceModal"
      :api-key-name="currentApiKey.name"
      :api-key="currentApiKey"
      @save="handleSaveInterfaceConfig"
      @copy-preview="handleCopyPreview"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { apiKeyApi, cardApi } from '../services/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import logger from '../utils/logger'
import { copyToClipboard } from '../utils/clipboard.js'
import { obfuscateCardKey } from '../utils/cardKey.js'
import ApiKeyCard from './api/ApiKeyCard.vue'
import CreateApiKeyDialog from './api/CreateApiKeyDialog.vue'
import EditApiKeyDialog from './api/EditApiKeyDialog.vue'
import CardCodesDialog from './api/CardCodesDialog.vue'
import UserManageDialog from './api/UserManageDialog.vue'
import ApiDocsDialog from './api/ApiDocsDialog.vue'
import CodeExamplesDialog from './api/CodeExamplesDialog.vue'
import InterfaceSettingsDialog from './api/InterfaceSettingsDialog.vue'

/**
 * ApiManagePage 主组件（重构后）
 * 作为容器组件，负责数据获取和业务逻辑，UI拆分到子组件中
 * 子组件通过 props 接收数据，通过 emit 向父组件通信
 */

const props = defineProps({})
const emit = defineEmits([])

/* ========== 数据状态 ========== */

/** API密钥列表 */
const apiKeys = ref([])
/** 所有用户列表（用于分配用户） */
const allUsers = ref([])

/* 各弹窗的显示状态 */
const showEditModal = ref(false)
const showCreateModal = ref(false)
const showDocsModal = ref(false)
const showCodeExamplesModal = ref(false)
const showInterfaceModal = ref(false)
const showCardCodesModal = ref(false)
const showUsersModal = ref(false)

/** 当前操作的API密钥对象 */
const currentApiKey = ref({})

/** 编辑弹窗使用的数据副本 */
const editingKeyData = computed(() => {
  if (!showEditModal.value) return {}
  return {
    id: currentApiKey.value.id,
    name: currentApiKey.value.name,
    description: currentApiKey.value.description,
    isActive: currentApiKey.value.isActive,
    enableCardEncryption: currentApiKey.value.enableCardEncryption,
    requireMachineCode: currentApiKey.value.requireMachineCode,
    machineSpecOnceConfig: currentApiKey.value.machineSpecOnceConfig
  }
})

/* ========== API密钥CRUD方法 ========== */

/** 从后端获取所有API密钥及其关联的卡密和用户数据 */
const fetchApiKeys = async () => {
  try {
    const data = await apiKeyApi.getAllApiKeys()
    apiKeys.value = await Promise.all(data.map(async key => {
      let cardCodes = [];
      try {
        const cardsRes = await cardApi.getApiKeyCards(key.id);
        if (cardsRes.success) {
          cardCodes = cardsRes.data.map(c => ({
            id: c.id,
            code: c.cardKey || c.card_key,
            status: c.status === 0 ? 'unused' : (c.status === 4 ? 'merged' : 'used'),
            expiryDate: c.expireTime || c.expire_time,
            type: (c.cardType || c.card_type) === 'time' ? '时间卡' : '次数卡',
            value: (c.cardType || c.card_type) === 'time' ? `${c.duration}天` : `${c.totalCount || c.total_count}次`,
            usedBy: (c.machineCode || c.device_id) ? `Device ${(c.machineCode || c.device_id).substring(0, 6)}...` : null
          }));
        }
      } catch (e) {
        logger.warn(`Failed to fetch cards for key ${key.id}`, e);
      }

      return {
        id: key.id,
        name: key.keyName,
        key: key.apiKey,
        description: key.description,
        isActive: key.status === 1,
        createdAt: key.createTime,
        lastUsed: null,
        requestCount: 0,
        cardCodes: cardCodes,
        webhookConfig: (() => { try { return key.webhookConfig ? JSON.parse(key.webhookConfig) : null } catch(e) { logger.warn('Invalid webhookConfig JSON:', e); return null } })(),
        assignedUsers: key.assignedUsers || [],
        enableCardEncryption: key.enableCardEncryption || false,
        requireMachineCode: key.requireMachineCode || false,
        machineSpecOnceConfig: key.machineSpecOnceConfig || ''
      }
    }))
  } catch (error) {
    logger.error('Failed to fetch API keys:', error)
    ElMessage.error('获取API密钥失败')
  }
}

/** 获取所有用户列表 */
const fetchUsers = async () => {
  try {
    const data = await apiKeyApi.getAllUsers()
    allUsers.value = data?.users || data || []
  } catch (error) {
    logger.error('Failed to fetch users:', error)
    allUsers.value = []
  }
}

/** 创建新的API密钥 */
const handleCreateApiKey = async ({ name, description, enableCardEncryption }) => {
  if (!name.trim()) return
  try {
    await apiKeyApi.createApiKey({
      name: name,
      description: description,
      enable_card_encryption: enableCardEncryption
    })
    ElMessage.success('创建成功')
    showCreateModal.value = false
    fetchApiKeys()
  } catch (error) {
    logger.error('Create failed:', error)
    ElMessage.error('创建失败')
  }
}

/** 保存编辑后的API密钥配置 */
const handleSaveApiKey = async (formData) => {
  try {
    await apiKeyApi.updateApiKey(formData.id, {
      name: formData.name,
      description: formData.description,
      status: formData.isActive ? 1 : 0,
      enable_card_encryption: formData.enableCardEncryption,
      require_machine_code: formData.requireMachineCode,
      machine_spec_once_config: formData.machineSpecOnceConfig || ''
    })
    ElMessage.success('保存成功')

    const keyIndex = apiKeys.value.findIndex(k => k.id === formData.id)
    if (keyIndex !== -1) {
      apiKeys.value[keyIndex].name = formData.name
      apiKeys.value[keyIndex].description = formData.description
      apiKeys.value[keyIndex].enableCardEncryption = formData.enableCardEncryption
      apiKeys.value[keyIndex].requireMachineCode = formData.requireMachineCode
      apiKeys.value[keyIndex].machineSpecOnceConfig = formData.machineSpecOnceConfig
    }

    showEditModal.value = false
    fetchApiKeys()
  } catch (error) {
    logger.error('Update failed:', error)
    ElMessage.error('保存失败')
  }
}

/** 删除API密钥 */
const deleteApiKey = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个API密钥吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await apiKeyApi.deleteApiKey(id)
    ElMessage.success('删除成功')
    fetchApiKeys()
  } catch (error) {
    if (error !== 'cancel') {
      logger.error('Delete failed:', error)
      ElMessage.error('删除失败')
    }
  }
}

/** 切换API密钥启用/禁用状态 */
const toggleApiKey = async (apiKey) => {
  try {
    const newStatus = !apiKey.isActive
    await apiKeyApi.updateApiKey(apiKey.id, {
      name: apiKey.name,
      description: apiKey.description,
      status: newStatus ? 1 : 0,
      enable_card_encryption: apiKey.enableCardEncryption
    })
    apiKey.isActive = newStatus
    ElMessage.success(newStatus ? '已启用' : '已禁用')
  } catch (error) {
    logger.error('Toggle failed:', error)
    ElMessage.error('操作失败')
  }
}

/** 将用户分配到当前API密钥 */
const handleAssignUser = async (userId) => {
  if (!userId || !currentApiKey.value.id) return
  try {
    await apiKeyApi.assignUser(currentApiKey.value.id, userId)
    ElMessage.success('分配成功')
    await fetchApiKeys()
    const updatedKey = apiKeys.value.find(k => k.id === currentApiKey.value.id)
    if (updatedKey) {
      currentApiKey.value = updatedKey
    }
  } catch (error) {
    logger.error('Assign failed:', error)
    ElMessage.error('分配用户失败')
  }
}

/** 从当前API密钥移除用户 */
const handleUnassignUser = async (userId) => {
  if (!currentApiKey.value.id) return
  try {
    await apiKeyApi.unassignUser(currentApiKey.value.id, userId)
    ElMessage.success('移除成功')
    await fetchApiKeys()
    const updatedKey = apiKeys.value.find(k => k.id === currentApiKey.value.id)
    if (updatedKey) {
      currentApiKey.value = updatedKey
    }
  } catch (error) {
    logger.error('Unassign failed:', error)
    ElMessage.error('移除用户失败')
  }
}

/* ========== API专属卡密管理 ========== */

/** 获取指定API密钥下的卡密列表 */
const fetchCardCodes = async (apiKeyId) => {
  if (!apiKeyId) return
  try {
    const res = await cardApi.getApiKeyCards(apiKeyId)
    const cards = res.data.map(c => ({
      id: c.id,
      code: c.cardKey || c.card_key,
      status: c.status === 0 ? 'unused' : 'used',
      expiryDate: c.expireTime || c.expire_time,
      type: (c.cardType || c.card_type) === 'time' ? '时间卡' : '次数卡',
      value: (c.cardType || c.card_type) === 'time' ? `${c.duration}天` : `${c.totalCount || c.total_count}次`,
      usedBy: (c.machineCode || c.device_id) ? `Device ${(c.machineCode || c.device_id).substring(0, 6)}...` : null
    }))

    if (currentApiKey.value.id === apiKeyId) {
      currentApiKey.value.cardCodes = cards
    }

    const keyIndex = apiKeys.value.findIndex(k => k.id === apiKeyId)
    if (keyIndex !== -1) {
      apiKeys.value[keyIndex].cardCodes = cards
    }
  } catch (error) {
    logger.error('Fetch cards failed:', error)
    ElMessage.error('获取卡密失败')
  }
}

/** 为当前API密钥批量生成卡密 */
const handleGenerateCardCodes = async ({ count, type, value, stackTime }) => {
  if (!currentApiKey.value.id) return
  try {
    const res = await cardApi.createCards({
      count: count,
      card_type: type,
      duration: type === 'time' ? value : 0,
      total_count: type === 'count' ? value : 0,
      verify_method: 'web',
      encryption_type: 'advanced',
      allow_reverify: 1,
      api_key_id: currentApiKey.value.id,
      stack_time_if_same_machine: type === 'time' && stackTime
    })
    ElMessage.success(`成功生成 ${res.data.length} 个卡密`)
    await fetchCardCodes(currentApiKey.value.id)
  } catch (error) {
    logger.error('Generate cards failed:', error)
    ElMessage.error('生成卡密失败')
  }
}

/** 删除指定卡密 */
const deleteCardCode = async (cardId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个卡密吗？此操作不可恢复！', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const result = await cardApi.deleteCard(cardId)
    if (result.success) {
      ElMessage.success('卡密删除成功')
      await fetchCardCodes(currentApiKey.value.id)
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      logger.error('删除卡密失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

/** 复制卡密明文到剪贴板 */
const copyCardCode = async (code) => {
  const success = await copyToClipboard(code)
  if (success) {
    ElMessage.success('卡密已复制')
  } else {
    ElMessage.error('复制失败')
  }
}

/**
 * 卡密混淆函数已抽取到 src/utils/cardKey.js，统一由工具模块导入使用
 */

/** 复制混淆后的加密卡密到剪贴板 */
const copyEncryptedCardCode = async (code) => {
  const encrypted = obfuscateCardKey(code)
  const success = await copyToClipboard(encrypted)
  if (success) {
    ElMessage.success('加密卡密已复制')
  } else {
    ElMessage.error('复制失败')
  }
}

/* ========== 接口回调配置管理 ========== */

/** 打开接口回调设置弹窗 */
const openInterfaceSettings = (apiKey) => {
  currentApiKey.value = apiKey
  showInterfaceModal.value = true
}

/** 保存接口回调配置到后端 */
const handleSaveInterfaceConfig = async (configData) => {
  const hasCardKey = configData.params.some(p => p.type === 'variable' && p.value === 'card_key')
  if (!hasCardKey) {
    ElMessage.error('必须配置"卡密 (card_key)"变量，否则系统无法获取卡密信息')
    return
  }

  const hasStatusCode = configData.response.some(p => p.type === 'variable' && p.value === 'status_code')
  if (!hasStatusCode) {
    try {
      await ElMessageBox.confirm(
        '检测到您配置了状态码规则，但未在返回结果中添加"状态码"字段。是否自动添加？',
        '配置提示',
        {
          confirmButtonText: '自动添加',
          cancelButtonText: '保持原样',
          type: 'warning'
        }
      )
      configData.response.unshift({
        key: 'code',
        type: 'variable',
        value: 'status_code'
      })
    } catch (e) {
      // 用户选择保持原样
    }
  }

  try {
    const configStr = JSON.stringify(configData)
    await apiKeyApi.updateApiKey(currentApiKey.value.id, {
      name: currentApiKey.value.name,
      description: currentApiKey.value.description,
      status: currentApiKey.value.isActive ? 1 : 0,
      webhook_config: configStr
    })

    currentApiKey.value.webhookConfig = JSON.parse(configStr)
    const keyIndex = apiKeys.value.findIndex(k => k.id === currentApiKey.value.id)
    if (keyIndex !== -1) {
      apiKeys.value[keyIndex].webhookConfig = JSON.parse(configStr)
    }

    ElMessage.success('接口配置已保存')
    showInterfaceModal.value = false
  } catch (error) {
    logger.error('Save interface config failed:', error)
    ElMessage.error('保存失败')
  }
}

/** 复制预览内容 */
const handleCopyPreview = async (content) => {
  if (!content) return
  const success = await copyToClipboard(content)
  if (success) {
    ElMessage.success('内容已复制')
  } else {
    ElMessage.error('复制失败')
  }
}

/** 复制代码示例 */
const handleCopyUseCardCode = async (code) => {
  if (!code) return
  const ok = await copyToClipboard(code)
  if (ok) ElMessage.success('代码已复制')
  else ElMessage.error('复制失败')
}

/* ========== 辅助方法 ========== */

/** 打开编辑弹窗 */
const editApiKey = (apiKey) => {
  currentApiKey.value = apiKey
  showEditModal.value = true
}

/** 打开用户管理弹窗 */
const manageUsers = (apiKey) => {
  currentApiKey.value = apiKey
  showUsersModal.value = true
}

/** 打开卡密管理弹窗 */
const manageCardCodes = (apiKey) => {
  currentApiKey.value = apiKey
  showCardCodesModal.value = true
  fetchCardCodes(apiKey.id)
}

/** 复制API密钥 */
const copyApiKey = async (key) => {
  const success = await copyToClipboard(key)
  if (success) {
    ElMessage.success('API密钥已复制')
  } else {
    ElMessage.error('复制失败')
  }
}

/** 计算属性：可用用户列表（排除已分配用户） */
const availableUsers = computed(() => {
  if (!allUsers.value || !Array.isArray(allUsers.value)) {
    return []
  }
  if (!currentApiKey.value.assignedUsers) {
    return allUsers.value
  }
  const assignedUserIds = currentApiKey.value.assignedUsers.map(u => u.id)
  return allUsers.value.filter(user => !assignedUserIds.includes(user.id))
})

/* ========== 生命周期 ========== */

onMounted(() => {
  fetchApiKeys()
  fetchUsers()
})
</script>

<style scoped>
.api-manage-page {
  padding: 0;
  width: 100%;
  box-sizing: border-box;
  overflow-x: auto;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.section-header h2 {
  color: #333;
  margin: 0;
  font-size: 1.5rem;
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.btn-primary,
.btn-secondary {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
  font-size: 0.85rem;
  font-weight: 500;
}

.btn-primary {
  background: #4f46e5;
  color: white;
}

.btn-primary:hover {
  background: #4338ca;
  transform: translateY(-1px);
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover {
  background: #4b5563;
  transform: translateY(-1px);
}

.api-keys-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #666;
}

.empty-icon {
  font-size: 3rem;
  color: #ccc;
  margin-bottom: 1rem;
}

.empty-state h3 {
  margin-bottom: 0.5rem;
  color: #333;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
}
</style>
