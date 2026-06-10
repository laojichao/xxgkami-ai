<!-- 卡密管理页面：卡密列表展示、搜索、分页、生成/编辑/删除/导出、时间卡倒计时 -->
<template>
  <div class="keys-manage-page">
    <!-- 批量操作栏：导出数据 + 生成卡密按钮 -->
    <BatchActions
      @show-export="showExportModal = true"
      @show-create="showCreateKeyModal = true"
    />

    <!-- 搜索筛选区域 -->
    <KeyFilters
      v-model="machineCodeSearch"
      :filtered-count="filteredKeys.length"
      :total-count="props.keys?.length || 0"
    />

    <!-- 卡密数据表格 -->
    <div class="keys-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>卡密</th>
            <th>类型</th>
            <th>状态</th>
            <th>机器码</th>
            <th>创建时间</th>
            <th>剩余时间/剩余次数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredKeys.length === 0">
            <td colspan="8" class="empty-table-hint">
              {{ machineCodeSearch ? '没有绑定该机器码的卡密（可尝试缩短关键字或清除筛选）' : '暂无卡密数据' }}
            </td>
          </tr>
          <template v-else>
            <KeyCard
              v-for="key in paginatedKeys"
              :key="key.id"
              :key-data="key"
              :now-ms="nowMs"
              @copy="copyKey"
              @edit="editKey"
              @toggle-status="toggleKeyStatus"
              @delete="deleteKey"
            />
          </template>
        </tbody>
      </table>
    </div>

    <!-- 分页导航（含页码跳转） -->
    <div class="pagination-container" v-if="totalPages > 1">
      <div class="pagination">
        <button
          class="pagination-btn"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)"
        >
          ‹ 上一页
        </button>

        <div class="page-numbers">
          <button
            v-if="showFirstPage"
            class="page-btn"
            :class="{ active: currentPage === 1 }"
            @click="goToPage(1)"
          >
            1
          </button>

          <span v-if="showStartEllipsis" class="ellipsis">...</span>

          <button
            v-for="page in visiblePages"
            :key="page"
            class="page-btn"
            :class="{ active: currentPage === page }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>

          <span v-if="showEndEllipsis" class="ellipsis">...</span>

          <button
            v-if="showLastPage"
            class="page-btn"
            :class="{ active: currentPage === totalPages }"
            @click="goToPage(totalPages)"
          >
            {{ totalPages }}
          </button>
        </div>

        <button
          class="pagination-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          下一页 ›
        </button>

        <div class="page-jump">
          <span>跳转到</span>
          <input
            type="number"
            v-model.number="jumpPage"
            :min="1"
            :max="totalPages"
            @keyup.enter="jumpToPage"
            class="jump-input"
          />
          <span>页</span>
          <button class="jump-btn" @click="jumpToPage">跳转</button>
        </div>

        <div class="pagination-info">
          共 {{ totalItems }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
        </div>
      </div>
    </div>

    <!-- 导出数据弹窗 -->
    <ExportDialog
      v-if="showExportModal"
      :available-columns="availableColumns"
      v-model:selected-columns="selectedColumns"
      v-model:export-format="exportFormat"
      v-model:export-usage-scope="exportUsageScope"
      :preview-data="previewData"
      :keys-for-export-count="keysForExport.length"
      :exporting="exporting"
      @close="showExportModal = false"
      @export="exportData"
    />

    <!-- 编辑卡密模态框 -->
    <div v-if="showEditKeyModal" class="modal-overlay" @click="showEditKeyModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>编辑卡密</h3>
          <button class="close-btn" @click="showEditKeyModal = false">
            ×
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>卡密</label>
            <input type="text" :value="editingKey.card_key" readonly class="readonly-input" />
          </div>
          <div class="form-group">
            <label>卡密类型</label>
            <select v-model="editingKey.card_type">
              <option value="time">时间卡密</option>
              <option value="count">次数卡密</option>
            </select>
          </div>
          <div class="form-group">
            <label>持续时间（天）</label>
            <input type="number" v-model="editingKey.duration" min="1" max="365" />
          </div>
          <div class="form-group" v-if="editingKey.card_type === 'count'">
            <label>总次数</label>
            <input type="number" v-model="editingKey.total_count" min="1" max="10000" />
          </div>
          <div class="form-group" v-if="editingKey.card_type === 'count'">
            <label>剩余次数</label>
            <input type="number" v-model="editingKey.remaining_count" min="0" :max="editingKey.total_count" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="editingKey.status">
              <option value="0">未使用</option>
              <option value="1">已使用</option>
              <option value="2">已停用</option>
            </select>
          </div>
          <div class="form-group" v-if="editingKey.card_type === 'time'">
            <label>允许重复验证</label>
            <select v-model="editingKey.allow_reverify">
              <option value="1">允许</option>
              <option value="0">不允许</option>
            </select>
            <small class="form-hint" v-if="editingKey.allow_reverify == 0">关闭后，时间卡密激活一次即不可再次验证</small>
            <small class="form-hint" v-else>开启后，时间卡密在有效期内可无限次验证</small>
          </div>
          <div class="form-group stack-time-stack-group">
            <div
              class="stack-option-card"
              :class="{ 'stack-option-card--active': editingKey.allow_self_unbind }"
            >
              <label class="stack-toggle-row">
                <span class="stack-switch">
                  <input
                    type="checkbox"
                    v-model="editingKey.allow_self_unbind"
                    class="stack-switch-input"
                  />
                  <span class="stack-switch-track">
                    <span class="stack-switch-thumb"></span>
                  </span>
                </span>
                <span class="stack-toggle-copy">
                  <span class="stack-toggle-title">
                    允许自助解绑
                    <span v-if="editingKey.allow_self_unbind" class="stack-toggle-pill">已开启</span>
                  </span>
                  <span class="stack-toggle-desc">
                    开启后，用户可凭卡密在首页「在线解绑」自行清空机器码；关闭则只能通过管理员重置。
                  </span>
                </span>
              </label>
            </div>
          </div>
          <div class="form-group">
            <label>机器码</label>
            <div class="machine-code-edit">
              <input
                type="text"
                :value="editingKey.machine_code || ''"
                readonly
                class="readonly-input"
                :placeholder="editingKey.machine_code ? '' : '未绑定'"
              />
              <button
                v-if="editingKey.machine_code"
                class="btn-danger btn-sm"
                @click="editingKey.machine_code = ''"
                title="重置机器码"
              >
                <i class="fas fa-undo"></i> 重置
              </button>
              <span v-else class="machine-code-hint">未绑定，无需重置</span>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showEditKeyModal = false">取消</button>
          <button class="btn-primary" @click="updateKey">
            <i class="fas fa-save"></i>
            保存
          </button>
        </div>
      </div>
    </div>

    <!-- 生成卡密模态框 -->
    <div v-if="showCreateKeyModal" class="modal-overlay" @click="showCreateKeyModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>生成卡密</h3>
          <button class="close-btn" @click="showCreateKeyModal = false">
            ×
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>卡密类型</label>
            <select v-model="newKey.card_type">
              <option value="time">时间卡密</option>
              <option value="count">次数卡密</option>
            </select>
          </div>
          <div class="form-group">
            <label>生成数量</label>
            <input type="number" v-model="newKey.count" min="1" max="100" />
          </div>
          <div class="form-group">
            <label>持续时间（天）</label>
            <input type="number" v-model="newKey.duration" min="1" max="365" />
          </div>
          <div class="form-group" v-if="newKey.card_type === 'count'">
            <label>总次数</label>
            <input type="number" v-model="newKey.total_count" min="1" max="10000" />
          </div>
          <div class="form-group" v-if="newKey.card_type === 'time'">
            <label>允许重复验证</label>
            <select v-model="newKey.allow_reverify">
              <option value="1">允许</option>
              <option value="0">不允许</option>
            </select>
            <small class="form-hint" v-if="newKey.allow_reverify == 0">关闭后，时间卡密激活一次即不可再次验证</small>
            <small class="form-hint" v-else>开启后，时间卡密在有效期内可无限次验证</small>
          </div>
          <div class="form-group stack-time-stack-group" v-if="newKey.card_type === 'time'">
            <div
              class="stack-option-card"
              :class="{ 'stack-option-card--active': newKey.stack_time_if_same_machine }"
            >
              <label class="stack-toggle-row">
                <span class="stack-switch">
                  <input
                    type="checkbox"
                    v-model="newKey.stack_time_if_same_machine"
                    class="stack-switch-input"
                  />
                  <span class="stack-switch-track">
                    <span class="stack-switch-thumb"></span>
                  </span>
                </span>
                <span class="stack-toggle-copy">
                  <span class="stack-toggle-title">
                    同机时长叠加（续期）
                    <span v-if="newKey.stack_time_if_same_machine" class="stack-toggle-pill">已开启</span>
                  </span>
                  <span class="stack-toggle-desc">
                    同一机器码上若已有未过期时间卡，激活本卡时将天数累加到原卡到期时间（本卡标记为「已合并」）；关闭则每次仍从激活时刻重新起算。
                  </span>
                </span>
              </label>
            </div>
          </div>
          <div class="form-group stack-time-stack-group">
            <div
              class="stack-option-card"
              :class="{ 'stack-option-card--active': newKey.allow_self_unbind }"
            >
              <label class="stack-toggle-row">
                <span class="stack-switch">
                  <input
                    type="checkbox"
                    v-model="newKey.allow_self_unbind"
                    class="stack-switch-input"
                  />
                  <span class="stack-switch-track">
                    <span class="stack-switch-thumb"></span>
                  </span>
                </span>
                <span class="stack-toggle-copy">
                  <span class="stack-toggle-title">
                    允许自助解绑
                    <span v-if="newKey.allow_self_unbind" class="stack-toggle-pill">已开启</span>
                  </span>
                  <span class="stack-toggle-desc">
                    开启后，用户可凭卡密在首页「在线解绑」自行清空机器码；关闭则只能通过管理员重置。
                  </span>
                </span>
              </label>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showCreateKeyModal = false">取消</button>
          <button class="btn-primary" @click="createKeys">
            <i class="fas fa-key"></i>
            生成
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cardApi } from '../services/api.js'
import logger from '../utils/logger'
import { copyToClipboard } from '../utils/clipboard.js'
import BatchActions from './keys/BatchActions.vue'
import KeyFilters from './keys/KeyFilters.vue'
import KeyCard from './keys/KeyCard.vue'
import ExportDialog from './keys/ExportDialog.vue'

const props = defineProps({
  keys: Array
})

const emit = defineEmits(['create-keys', 'delete-key', 'update-key', 'toggle-key-status'])

/* ========== 时间卡倒计时 ========== */
const nowMs = ref(Date.now())
let countdownTimer = null

onMounted(() => {
  countdownTimer = setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})

/* ========== 弹窗与导出状态 ========== */
const showCreateKeyModal = ref(false)
const showEditKeyModal = ref(false)
const showExportModal = ref(false)
const exporting = ref(false)
const exportFormat = ref('xlsx')
const exportUsageScope = ref('all')
const machineCodeSearch = ref('')
const selectedColumns = ref(['id', 'card_key', 'card_type', 'status', 'create_time'])

/** 可导出的列定义 */
const availableColumns = [
  { key: 'id', label: '序号' },
  { key: 'card_key', label: '卡密' },
  { key: 'encrypted_key', label: '加密卡密' },
  { key: 'card_type', label: '卡密类型' },
  { key: 'status', label: '状态' },
  { key: 'create_time', label: '创建时间' },
  { key: 'user_info', label: '使用者' },
  { key: 'remaining_time', label: '剩余时间' },
  { key: 'remaining_count', label: '剩余次数' },
  { key: 'expire_time', label: '过期时间' },
  { key: 'machine_code', label: '机器码' },
  { key: 'is_exclusive', label: '是否专属' },
  { key: 'api_key_id', label: '专属API Key' }
]

/** 根据列key获取中文标签 */
const getColumnLabel = (key) => {
  const col = availableColumns.find(c => c.key === key)
  return col ? col.label : key
}

/** 卡密混淆函数（与ApiManagePage保持一致） */
const obfuscateCardKey = (rawKey) => {
  if (!rawKey) return rawKey
  try {
    const encoded = encodeURIComponent(rawKey)
    const reversed = encoded.split('').reverse().join('')
    const base64 = btoa(reversed)
    return base64.replace(/e/g, '*').replace(/U/g, '-')
  } catch (e) {
    logger.error('Obfuscation failed:', e)
    return rawKey
  }
}

/** 处理导出数据：根据选中列映射字段值 */
const processExportData = (data) => {
  return data.map(item => {
    const processed = {}

    if (selectedColumns.value.includes('id')) processed.id = item.id
    if (selectedColumns.value.includes('card_key')) processed.card_key = item.card_key
    if (selectedColumns.value.includes('encrypted_key')) processed.encrypted_key = obfuscateCardKey(item.card_key)

    if (selectedColumns.value.includes('user_info')) {
      processed.user_info = item.device_id ? `Device: ${item.device_id}` : (item.ip_address ? `IP: ${item.ip_address}` : '-')
    }

    if (selectedColumns.value.includes('remaining_time')) {
      if (item.card_type === 'time') {
        if (item.expire_time) {
          const ms = new Date(item.expire_time).getTime() - Date.now()
          if (ms <= 0) {
            processed.remaining_time = '已过期'
          } else {
            const d = Math.floor(ms / 86400000)
            const h = Math.floor((ms % 86400000) / 3600000)
            const m = Math.floor((ms % 3600000) / 60000)
            processed.remaining_time = d > 0 ? `${d}天${h}小时${m}分钟` : `${h}小时${m}分钟`
          }
        } else {
          processed.remaining_time = `${item.duration}天（未激活）`
        }
      } else {
        processed.remaining_time = '-'
      }
    }
    if (selectedColumns.value.includes('remaining_count')) {
      processed.remaining_count = item.card_type === 'count' ? `${item.remaining_count}/${item.total_count}` : '-'
    }

    if (selectedColumns.value.includes('expire_time')) {
      processed.expire_time = item.expire_time ? formatDate(item.expire_time) : (item.card_type === 'time' ? '未激活' : '-')
    }
    if (selectedColumns.value.includes('create_time')) processed.create_time = formatDate(item.create_time)

    if (selectedColumns.value.includes('card_type')) processed.card_type = getCardTypeText(item.card_type)
    if (selectedColumns.value.includes('status')) processed.status = getStatusText(item.status)
    if (selectedColumns.value.includes('machine_code')) processed.machine_code = item.machine_code || '-'
    if (selectedColumns.value.includes('is_exclusive')) processed.is_exclusive = item.api_key_id ? '是' : '否'
    if (selectedColumns.value.includes('api_key_id')) processed.api_key_id = item.api_key_id || '-'

    return processed
  })
}

/** 按机器码搜索关键字过滤后的卡密列表 */
const filteredKeys = computed(() => {
  const list = props.keys || []
  const q = (machineCodeSearch.value || '').trim().toLowerCase()
  if (!q) return list
  return list.filter((k) => {
    const mc = (k.machine_code ?? '').toString().toLowerCase()
    return mc.includes(q)
  })
})

/** 符合导出条件的卡密列表（按使用状态范围筛选） */
const keysForExport = computed(() => {
  let keys = filteredKeys.value
  if (exportUsageScope.value === 'unused') {
    keys = keys.filter((k) => Number(k.status) === 0)
  } else if (exportUsageScope.value === 'used') {
    keys = keys.filter((k) => [1, 2, 4].includes(Number(k.status)))
  }
  return keys
})

/** 导出预览数据（前5条） */
const previewData = computed(() => {
  const src = keysForExport.value
  if (!src.length) return []
  return processExportData(src.slice(0, 5))
})

/** 执行导出（生成Excel/CSV文件并下载） */
const exportData = async () => {
  if (selectedColumns.value.length === 0) return

  const allData = keysForExport.value
  if (allData.length === 0) {
    ElMessage.warning('当前筛选条件下没有可导出的数据')
    return
  }

  exporting.value = true
  try {
    const XLSX = await import('xlsx')
    const dataToExport = processExportData(allData)

    const wb = XLSX.utils.book_new()
    const header = selectedColumns.value.map(key => getColumnLabel(key))
    const body = dataToExport.map(row => selectedColumns.value.map(key => row[key]))

    const ws = XLSX.utils.aoa_to_sheet([header, ...body])
    XLSX.utils.book_append_sheet(wb, ws, "卡密数据")

    const fileName = `卡密导出_${new Date().toISOString().slice(0,10)}.${exportFormat.value}`
    XLSX.writeFile(wb, fileName)

    ElMessage.success('导出成功')
    showExportModal.value = false
  } catch (error) {
    logger.error('Export failed:', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// 分页相关状态
const currentPage = ref(1)
const pageSize = ref(10)
const jumpPage = ref(1)

watch(machineCodeSearch, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const newKey = reactive({
  card_type: 'time',
  count: 1,
  duration: 30,
  total_count: 100,
  verify_method: 'web',
  encryption_type: 'advanced',
  allow_reverify: 1,
  stack_time_if_same_machine: false,
  allow_self_unbind: false
})

const editingKey = reactive({
  id: null,
  card_key: '',
  card_type: 'time',
  duration: 30,
  total_count: 100,
  remaining_count: 100,
  status: 0,
  verify_method: 'web',
  encryption_type: 'sha1',
  allow_reverify: 1,
  machine_code: '',
  allow_self_unbind: false
})

// 计算属性
const totalItems = computed(() => filteredKeys.value?.length ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize.value)))

const paginatedKeys = computed(() => {
  const list = filteredKeys.value || []
  if (!list.length) return []
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return list.slice(start, end)
})

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    let start = Math.max(2, current - 2)
    let end = Math.min(total - 1, current + 2)

    if (end - start < 4) {
      if (start === 2) {
        end = Math.min(total - 1, start + 4)
      } else {
        start = Math.max(2, end - 4)
      }
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
  }

  return pages
})

const showFirstPage = computed(() => {
  return totalPages.value > 7 && !visiblePages.value.includes(1)
})

const showLastPage = computed(() => {
  return totalPages.value > 7 && !visiblePages.value.includes(totalPages.value)
})

const showStartEllipsis = computed(() => {
  return showFirstPage.value && visiblePages.value[0] > 2
})

const showEndEllipsis = computed(() => {
  return showLastPage.value && visiblePages.value[visiblePages.value.length - 1] < totalPages.value - 1
})

// 分页方法
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    jumpPage.value = page
  }
}

const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
  } else {
    jumpPage.value = currentPage.value
  }
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getCardTypeText = (cardType) => {
  const typeMap = {
    time: '时间卡密',
    count: '次数卡密'
  }
  return typeMap[cardType] || cardType
}

const getStatusText = (status) => {
  const statusMap = {
    0: '未使用',
    1: '已使用',
    2: '已暂停',
    4: '已合并(续期)'
  }
  return statusMap[status] || status
}

const getStatusClass = (status) => {
  const statusClassMap = {
    0: 'unused',
    1: 'used',
    2: 'disabled',
    4: 'used'
  }
  return statusClassMap[status] || 'unknown'
}

const createKeys = () => {
  const keyData = { ...newKey }
  if (keyData.card_type === 'time') {
    keyData.total_count = 0
  }

  emit('create-keys', keyData)
  showCreateKeyModal.value = false
  newKey.card_type = 'time'
  newKey.count = 1
  newKey.duration = 30
  newKey.total_count = 100
  newKey.verify_method = 'web'
  newKey.encryption_type = 'advanced'
  newKey.allow_reverify = 1
  newKey.stack_time_if_same_machine = false
  newKey.allow_self_unbind = false
}

const editKey = (key) => {
  Object.assign(editingKey, {
    id: key.id,
    card_key: key.card_key,
    card_type: key.card_type,
    duration: key.duration,
    total_count: key.total_count || 100,
    remaining_count: key.remaining_count || key.total_count || 100,
    status: key.status,
    verify_method: key.verify_method || 'web',
    encryption_type: key.encryption_type || 'advanced',
    allow_reverify: key.allow_reverify !== undefined ? key.allow_reverify : 1,
    machine_code: key.machine_code || '',
    allow_self_unbind: key.allow_self_unbind === true || key.allow_self_unbind === 1
  })
  showEditKeyModal.value = true
}

const updateKey = () => {
  emit('update-key', { ...editingKey })
  showEditKeyModal.value = false
}

const toggleKeyStatus = ({ id, status }) => {
  emit('toggle-key-status', { id, status })
}

const deleteKey = async (keyId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个卡密吗？此操作不可恢复！', '确认删除', { type: 'warning' })
  } catch { return }
  emit('delete-key', keyId)
}

const copyKey = async (cardKey) => {
  const success = await copyToClipboard(cardKey)
  if (success) {
    ElMessage.success('卡密已复制到剪贴板')
  } else {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.keys-manage-page {
  padding: 0;
  width: 100%;
  box-sizing: border-box;
  overflow-x: auto;
  background: #fafbfc;
  min-height: 100vh;
}

/* 表格样式 */
.keys-table {
  overflow-x: hidden;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin: 1rem 2rem 2rem;
  border: 1px solid #e1e5e9;
}

.keys-table table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.empty-table-hint {
  text-align: center;
  padding: 2.5rem 1rem !important;
  color: #718096;
  font-size: 0.9rem;
}

.keys-table th:nth-child(1) { width: 4%; }
.keys-table th:nth-child(2) { width: 20%; }
.keys-table th:nth-child(3) { width: 7%; }
.keys-table th:nth-child(4) { width: 7%; }
.keys-table th:nth-child(5) { width: 10%; }
.keys-table th:nth-child(6) { width: 13%; }
.keys-table th:nth-child(7) { width: 17%; }
.keys-table th:nth-child(8) { width: 22%; }

.keys-table th,
.keys-table td {
  padding: 0.5rem 0.75rem;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.keys-table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
  font-size: 0.875rem;
}

.keys-table tbody tr {
  transition: background-color 0.2s ease;
}

.keys-table tbody tr:hover {
  background: #f8fafc;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.modal-header {
  padding: 1.5rem 1.5rem 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #2d3748;
  font-size: 1.25rem;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #6b7280;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.875rem;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 0.875rem;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.readonly-input {
  background: #f9fafb !important;
  color: #6b7280 !important;
  cursor: not-allowed !important;
}

.form-hint {
  display: block;
  margin-top: 0.35rem;
  font-size: 0.8rem;
  color: #6b7280;
}

.machine-code-edit {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.machine-code-edit .readonly-input {
  flex: 1;
}

.machine-code-hint {
  font-size: 0.8rem;
  color: #a1a1aa;
  white-space: nowrap;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  padding: 0 1.5rem 1.5rem;
}

/* 开关样式 */
.stack-time-stack-group {
  margin-bottom: 1.5rem;
}

.stack-option-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1rem 1.125rem;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.stack-option-card--active {
  border-color: #c7d2fe;
  background: linear-gradient(165deg, #f8faff 0%, #ffffff 55%);
  box-shadow:
    0 0 0 1px rgba(79, 70, 229, 0.07),
    0 6px 20px rgba(79, 70, 229, 0.08);
}

.stack-toggle-row {
  display: flex !important;
  align-items: flex-start;
  gap: 0.875rem;
  margin: 0 !important;
  cursor: pointer;
  font-weight: normal !important;
  color: inherit !important;
}

.stack-switch {
  position: relative;
  width: 48px;
  height: 28px;
  flex-shrink: 0;
  margin-top: 2px;
}

.stack-switch-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  z-index: 2;
  cursor: pointer;
}

.stack-switch-track {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: #d1d5db;
  transition: background 0.22s ease;
}

.stack-switch-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.18);
  transition: transform 0.22s cubic-bezier(0.34, 1.1, 0.64, 1);
  pointer-events: none;
}

.stack-switch-input:checked + .stack-switch-track {
  background: #4f46e5;
}

.stack-switch-input:checked + .stack-switch-track .stack-switch-thumb {
  transform: translateX(20px);
}

.stack-switch-input:focus-visible + .stack-switch-track {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.28);
}

.stack-toggle-copy {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  min-width: 0;
}

.stack-toggle-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #111827;
  line-height: 1.35;
}

.stack-toggle-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #4338ca;
  background: #e0e7ff;
}

.stack-toggle-desc {
  font-size: 0.8125rem;
  line-height: 1.5;
  color: #6b7280;
  font-weight: 400;
}

/* 分页样式 */
.pagination-container {
  padding: 1.5rem 2rem;
  background: white;
  border-top: 1px solid #e1e5e9;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.pagination-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f9fafb;
}

.page-numbers {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.page-btn {
  width: 2.5rem;
  height: 2.5rem;
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.page-btn.active {
  background: #4f46e5;
  border-color: #4f46e5;
  color: white;
}

.ellipsis {
  padding: 0 0.5rem;
  color: #6b7280;
  font-size: 0.875rem;
}

.page-jump {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #374151;
}

.jump-input {
  width: 4rem;
  padding: 0.375rem 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.875rem;
  text-align: center;
}

.jump-input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.1);
}

.jump-btn {
  padding: 0.375rem 0.75rem;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background-color 0.2s ease;
}

.jump-btn:hover {
  background: #4338ca;
}

.pagination-info {
  font-size: 0.875rem;
  color: #6b7280;
  white-space: nowrap;
}

/* 按钮通用样式 */
.btn-primary {
  background: #4f46e5;
  color: white;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  font-weight: 500;
}

.btn-primary:hover {
  background: #4338ca;
  transform: translateY(-1px);
}

.btn-secondary {
  background: #6b7280;
  color: white;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  font-weight: 500;
}

.btn-secondary:hover {
  background: #4b5563;
}

.btn-danger {
  background: #ef4444;
  color: white;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  font-weight: 500;
}

.btn-danger:hover {
  background: #dc2626;
}

.btn-sm {
  padding: 0.375rem 0.5rem;
  font-size: 0.75rem;
  border-radius: 4px;
  min-width: auto;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .keys-table {
    margin: 0 1rem 2rem;
  }

  .pagination-container {
    padding: 1.5rem 1rem;
  }
}

@media (max-width: 768px) {
  .keys-table {
    font-size: 0.875rem;
    margin: 0 0.5rem 1rem;
    border-radius: 8px;
  }

  .keys-table th,
  .keys-table td {
    padding: 0.75rem 0.5rem;
  }

  .modal-content {
    margin: 1rem;
    width: calc(100% - 2rem);
    border-radius: 8px;
  }

  .modal-header,
  .modal-body,
  .modal-actions {
    padding: 1rem;
  }

  .btn-primary,
  .btn-secondary,
  .btn-danger {
    padding: 0.75rem 1rem;
    font-size: 0.875rem;
  }

  .pagination-container {
    padding: 1rem 0.5rem;
  }

  .pagination {
    flex-direction: column;
    gap: 0.75rem;
  }

  .page-numbers {
    order: 1;
  }

  .pagination-btn {
    order: 2;
    padding: 0.5rem 0.75rem;
    font-size: 0.8rem;
  }

  .page-jump {
    order: 3;
    font-size: 0.8rem;
  }

  .pagination-info {
    order: 4;
    text-align: center;
    font-size: 0.8rem;
  }
}

@media (max-width: 480px) {
  .keys-manage-page {
    border-radius: 0;
  }

  .keys-table {
    border-radius: 0;
    margin: 0 0 1rem;
  }

  .modal-content {
    border-radius: 0;
    margin: 0;
    width: 100%;
    height: 100%;
  }

  .pagination-container {
    padding: 0.75rem 0.25rem;
    border-radius: 0;
  }

  .page-btn {
    width: 2rem;
    height: 2rem;
    font-size: 0.75rem;
  }

  .jump-input {
    width: 3rem;
  }
}
</style>
