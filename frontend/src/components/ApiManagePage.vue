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
    const res = await apiKeyApi.getAllApiKeys()
    // 后端返回统一响应格式 {success, message, data:[...]}，
    // 必须取 res.data 后再 map；直接对响应体调用 map 会因缺少该方法而抛错，
    // 导致 API 密钥列表始终为空。
    const list = res && Array.isArray(res.data) ? res.data : []
    apiKeys.value = await Promise.all(list.map(async key => {
      let cardCodes = [];
      try {
        const cardsRes = await cardApi.getApiKeyCards(key.id);
        if (cardsRes.success) {
          cardCodes = cardsRes.data.map(c => ({
            id: c.id,
            code: c.cardKey || c.card_key,
            // 后端返回的真实 SHA-256 值，用于「复制加密卡密」功能
            encryptedKey: c.encryptedKey || c.encrypted_key || '',
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
        // 后端 ApiKey.status 是 Boolean（true=启用），JSON 中为 true/false；
        // 兼容历史数据可能出现的 1/0 数字形式，避免密钥状态永远显示为「未使用」
        isActive: key.status === true || key.status === 1,
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
    const res = await apiKeyApi.getAllUsers()
    // 后端 /admin/users 返回统一响应格式，data 为 Spring Data Page 对象
    // （{content:[...], totalElements, ...}）。此前读取 res.users 恒为 undefined，
    // 导致「用户管理」弹窗中可选用户列表始终为空。
    const page = res && res.data ? res.data : null
    if (Array.isArray(page)) {
      allUsers.value = page
    } else if (page && Array.isArray(page.content)) {
      allUsers.value = page.content
    } else {
      allUsers.value = []
    }
  } catch (error) {
    logger.error('Failed to fetch users:', error)
    allUsers.value = []
  }
}

/** 创建新的API密钥 */
const handleCreateApiKey = async ({ name, description, enableCardEncryption }) => {
  if (!name.trim()) return
  try {
    const result = await apiKeyApi.createApiKey({
      name: name,
      description: description,
      // 后端 DTO 字段为 camelCase（enableCardEncryption），
      // 使用 snake_case 会被 Jackson 静默忽略，导致「卡密加密传输」开关不生效
      enableCardEncryption: enableCardEncryption
    })
    // 密钥值仅在创建时返回一次，列表接口不会返回（后端 @JsonIgnore）。
    // 必须在此处提示管理员立即保存，否则关闭弹窗后无法再次获取。
    const createdKey = result?.data?.apiKey
    showCreateModal.value = false
    await fetchApiKeys()
    if (createdKey) {
      // 使用纯文本展示，避免 dangerouslyUseHTMLString 引入 XSS 风险
      try {
        await ElMessageBox.alert(
          `请立即保存以下 API 密钥，关闭后将无法再次查看：\n\n${createdKey}`,
          'API 密钥创建成功',
          {
            confirmButtonText: '复制并关闭',
            showClose: false
          }
        )
        copyApiKey(createdKey)
      } catch (e) {
        // 用户关闭弹窗，忽略
      }
    } else {
      ElMessage.success('创建成功')
    }
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
      status: formData.isActive,
      // 必须使用后端 DTO 的 camelCase 字段名，snake_case 会被静默忽略
      enableCardEncryption: formData.enableCardEncryption,
      requireMachineCode: formData.requireMachineCode,
      machineSpecOnceConfig: formData.machineSpecOnceConfig || ''
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
    // 后端 status 为 Boolean，直接传布尔值，避免 1/0 与 Boolean 的反序列化歧义
    await apiKeyApi.updateApiKey(apiKey.id, {
      name: apiKey.name,
      description: apiKey.description,
      status: newStatus,
      enableCardEncryption: apiKey.enableCardEncryption
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
      // 后端返回的真实 SHA-256 值，用于「复制加密卡密」功能
      encryptedKey: c.encryptedKey || c.encrypted_key || '',
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
 * 复制加密卡密到剪贴板。
 * <p>使用后端返回的 encryptedKey（SHA-256 + Base64）原值，
 * 不再前端重新计算：前端 obfuscateCardKey 的算法与后端不一致，
 * 计算出的值与数据库中存储的加密卡密不同，复制结果无实际用途。</p>
 *
 * @param {Object} cardCode 卡密对象（含 encryptedKey 字段）
 */
const copyEncryptedCardCode = async (cardCode) => {
  const encrypted = typeof cardCode === 'object' && cardCode !== null
    ? (cardCode.encryptedKey || '')
    : ''
  if (!encrypted) {
    ElMessage.warning('该卡密没有可用的加密值')
    return
  }
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
      status: currentApiKey.value.isActive,
      webhookConfig: configStr
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
  // 列表接口不返回密钥值，key 可能为 undefined，此时给出明确提示而不是静默失败
  if (!key) {
    ElMessage.warning('密钥仅在创建时显示一次，无法再次复制')
    return
  }
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
