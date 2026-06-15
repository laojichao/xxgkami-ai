<!-- 单个卡密表格行：展示卡密信息 + 操作按钮 -->
<template>
  <tr>
    <td>{{ keyData.id }}</td>
    <td class="key-code" @click="$emit('copy', cardKey)" :title="showKey ? '点击复制' : '点击复制（当前已隐藏）'">
      <span v-if="showKey">{{ cardKey }}</span>
      <span v-else class="key-masked">{{ maskKey(cardKey) }}</span>
      <button class="key-toggle-btn" @click.stop="showKey = !showKey" :title="showKey ? '隐藏' : '显示'">
        {{ showKey ? '🙈' : '👁' }}
      </button>
    </td>
    <td>
      <span class="card-type" :class="cardType">
        {{ getCardTypeText(cardType) }}
      </span>
    </td>
    <td>
      <span :class="['status', getStatusClass(keyData.status)]">
        {{ getStatusText(keyData.status) }}
      </span>
    </td>
    <td class="machine-code-cell" :title="machineCode || ''">
      <span v-if="machineCode" class="machine-code-tag">{{ machineCode }}</span>
      <span v-else class="machine-code-empty">未绑定</span>
    </td>
    <td>{{ formatDate(keyData.createTime || keyData.create_time) }}</td>
    <td class="duration-cell">
      <template v-if="cardType === 'time'">
        <span
          v-if="keyData.expireTime || keyData.expire_time"
          :class="['time-countdown', { 'is-expired': isTimeCardExpired(keyData) }]"
        >
          {{ formatTimeCardRemaining(keyData) }}
        </span>
        <span v-else class="time-spec">{{ (keyData.duration ?? 0) }} 天（未激活）</span>
      </template>
      <template v-else>{{ keyData.remainingCount || keyData.remaining_count }} 次</template>
    </td>
    <td>
      <div class="action-buttons">
        <button class="btn-secondary btn-sm" @click="$emit('copy', cardKey)">
          <i class="fas fa-copy"></i>
          复制
        </button>
        <button class="btn-primary btn-sm" @click="$emit('edit', keyData)">
          <i class="fas fa-edit"></i>
          编辑
        </button>
        <button
          v-if="keyData.status === 0 || keyData.status === 2"
          class="btn-success btn-sm"
          @click="$emit('toggle-status', { id: keyData.id, status: 1 })"
        >
          <i class="fas fa-play"></i>
          启用
        </button>
        <button
          v-if="keyData.status === 1"
          class="btn-warning btn-sm"
          @click="$emit('toggle-status', { id: keyData.id, status: 2 })"
        >
          <i class="fas fa-pause"></i>
          暂停
        </button>
        <button class="btn-danger btn-sm" @click="$emit('delete', keyData.id)">
          <i class="fas fa-trash"></i>
          删除
        </button>
      </div>
    </td>
  </tr>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  keyData: { type: Object, required: true },
  nowMs: { type: Number, default: () => Date.now() }
})

defineEmits(['copy', 'edit', 'toggle-status', 'delete'])

const showKey = ref(false)

// 兼容 camelCase 和 snake_case 属性名
const cardKey = computed(() => props.keyData.cardKey || props.keyData.card_key || '')
const cardType = computed(() => props.keyData.cardType || props.keyData.card_type || 'time')
const machineCode = computed(() => props.keyData.machineCode || props.keyData.machine_code || '')

/** 将卡密脱敏显示，保留首尾各4字符 */
const maskKey = (key) => {
  if (!key || key.length <= 8) return '****-****'
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
}

const pad2 = (n) => String(n).padStart(2, '0')

const parseExpireTimeMs = (item) => {
  const raw = item?.expireTime || item?.expire_time
  if (raw == null || raw === '') return null
  const t = new Date(raw).getTime()
  return Number.isFinite(t) ? t : null
}

const isTimeCardExpired = (item) => {
  const end = parseExpireTimeMs(item)
  return end != null && end <= Date.now()
}

const formatTimeCardRemaining = (item) => {
  const end = parseExpireTimeMs(item)
  if (end == null) return '—'
  const ms = end - Date.now()
  if (ms <= 0) return '已过期'
  const totalSec = Math.floor(ms / 1000)
  const days = Math.floor(totalSec / 86400)
  const h = Math.floor((totalSec % 86400) / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  if (days > 0) {
    return `${days} 天 ${pad2(h)}:${pad2(m)}:${pad2(s)}`
  }
  return `${pad2(h)}:${pad2(m)}:${pad2(s)}`
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getCardTypeText = (cardType) => {
  const typeMap = { time: '时间卡密', count: '次数卡密' }
  return typeMap[cardType] || cardType
}

const getStatusText = (status) => {
  const statusMap = { 0: '未使用', 1: '已使用', 2: '已暂停', 4: '已合并(续期)' }
  return statusMap[status] || status
}

const getStatusClass = (status) => {
  const statusClassMap = { 0: 'unused', 1: 'used', 2: 'disabled', 4: 'used' }
  return statusClassMap[status] || 'unknown'
}
</script>

<style scoped>
.key-code {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  background: #f1f5f9;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
  color: #475569;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 100%;
  display: block;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.key-code:hover {
  background: #e2e8f0;
  color: #2563eb;
}

.key-masked {
  color: #94a3b8;
  letter-spacing: 0.05em;
}

.key-toggle-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0 0.25rem;
  font-size: 0.75rem;
  opacity: 0.6;
  transition: opacity 0.2s;
  vertical-align: middle;
}

.key-toggle-btn:hover {
  opacity: 1;
}

.machine-code-cell {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.machine-code-tag {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 0.75rem;
  background: #f0fdf4;
  color: #15803d;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #bbf7d0;
}

.machine-code-empty {
  font-size: 0.75rem;
  color: #a1a1aa;
}

.card-type {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  display: inline-block;
}

.card-type.time {
  background: #dbeafe;
  color: #1e40af;
}

.card-type.count {
  background: #fce7f3;
  color: #be185d;
}

.status {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  display: inline-block;
}

.status.unused {
  background: #dcfce7;
  color: #166534;
}

.status.used {
  background: #e0f2fe;
  color: #0c4a6e;
}

.status.disabled {
  background: #fef2f2;
  color: #991b1b;
}

.duration-cell {
  font-size: 0.8125rem;
  line-height: 1.45;
  word-break: break-word;
}

.time-countdown {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-variant-numeric: tabular-nums;
  color: #0369a1;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.time-countdown.is-expired {
  color: #b91c1c;
}

.time-spec {
  color: #64748b;
  font-size: 0.8125rem;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-sm {
  padding: 0.375rem 0.5rem;
  font-size: 0.75rem;
  border-radius: 4px;
  min-width: auto;
}

.btn-primary {
  background: #4f46e5;
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

.btn-primary:hover {
  background: #4338ca;
}

.btn-secondary {
  background: #6b7280;
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

.btn-success {
  background: #10b981;
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

.btn-success:hover {
  background: #059669;
}

.btn-warning {
  background: #f59e0b;
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

.btn-warning:hover {
  background: #d97706;
}
</style>
