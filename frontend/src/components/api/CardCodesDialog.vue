<!-- API专属卡密管理弹窗：生成/查看/删除卡密 -->
<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('update:visible', false)">
    <div class="modal-content large-modal" @click.stop>
      <div class="modal-header">
        <h3>{{ apiKeyName }} - 专属卡密管理</h3>
        <button class="close-btn" @click="$emit('update:visible', false)">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body">
        <!-- 卡密生成工具栏 -->
        <div class="card-codes-header">
          <div class="card-codes-toolbar">
            <div class="form-group inline">
              <label>生成数量</label>
              <input type="number" v-model="genCount" min="1" max="100" placeholder="1-100" />
            </div>
            <div class="form-group inline">
              <label>卡密类型</label>
              <div class="custom-select-wrapper">
                <select v-model="genType" class="custom-select">
                  <option value="time">时间卡密</option>
                  <option value="count">次数卡密</option>
                </select>
                <div class="select-arrow">
                  <i class="fas fa-chevron-down"></i>
                </div>
              </div>
            </div>
            <div class="form-group inline">
              <label>{{ genType === 'time' ? '有效期（天）' : '使用次数' }}</label>
              <input type="number" v-model="genValue" min="1" max="9999" :placeholder="genType === 'time' ? '30' : '100'" />
            </div>
            <button class="btn-primary card-codes-generate-btn" @click="$emit('generate', { count: genCount, type: genType, value: genValue, stackTime: genType === 'time' && genStackTime })">
              <i class="fas fa-plus"></i>
              生成卡密
            </button>
          </div>

          <!-- 同机时长叠加开关 -->
          <div class="stack-time-stack-group api-exclusive-stack" v-if="genType === 'time'">
            <div
              class="stack-option-card"
              :class="{ 'stack-option-card--active': genStackTime }"
            >
              <label class="stack-toggle-row">
                <span class="stack-switch">
                  <input
                    type="checkbox"
                    v-model="genStackTime"
                    class="stack-switch-input"
                  />
                  <span class="stack-switch-track">
                    <span class="stack-switch-thumb"></span>
                  </span>
                </span>
                <span class="stack-toggle-copy">
                  <span class="stack-toggle-title">
                    同机时长叠加（续期）
                    <span v-if="genStackTime" class="stack-toggle-pill">已开启</span>
                  </span>
                  <span class="stack-toggle-desc">
                    同一机器码上若已有未过期时间卡，激活本卡时将天数累加到原卡到期时间（本卡标记为「已合并」）；关闭则每次仍从激活时刻重新起算。
                  </span>
                </span>
              </label>
            </div>
          </div>
        </div>

        <!-- 卡密列表 -->
        <div class="card-codes-list">
          <div class="card-code-item" v-for="cardCode in cardCodes" :key="cardCode.id">
            <div class="card-code-info">
              <code class="card-code-value">{{ cardCode.code }}</code>
              <div class="card-code-meta">
                <span class="type-badge">{{ cardCode.type }} ({{ cardCode.value }})</span>
                <span class="status" :class="cardCode.status">{{ getCardCodeStatusText(cardCode.status) }}</span>
                <span class="expiry" v-if="cardCode.expiryDate">到期: {{ formatDate(cardCode.expiryDate) }}</span>
                <span class="usage" v-if="cardCode.usedBy">使用者: {{ cardCode.usedBy }}</span>
              </div>
            </div>
            <div class="card-code-actions">
              <button class="copy-btn small" @click="$emit('copy-card', cardCode.code)" title="复制卡密">
                <i class="fas fa-copy"></i>
                复制
              </button>
              <button v-if="enableCardEncryption" class="copy-btn small warning" @click="$emit('copy-encrypted-card', cardCode.code)" title="复制加密卡密">
                <i class="fas fa-lock"></i>
                加密复制
              </button>
              <button class="btn-danger small" @click="$emit('delete-card', cardCode.id)" v-if="cardCode.status === 'unused'">
                <i class="fas fa-trash"></i>
                删除
              </button>
            </div>
          </div>

          <div v-if="!cardCodes || cardCodes.length === 0" class="empty-card-codes">
            <i class="fas fa-credit-card"></i>
            <p>暂无专属卡密，点击上方按钮生成</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

/**
 * CardCodesDialog 组件
 * 管理API密钥专属卡密的弹窗，支持批量生成、查看列表、复制、加密复制、删除
 */
defineProps({
  /** 控制弹窗显示/隐藏 */
  visible: {
    type: Boolean,
    default: false
  },
  /** 当前API密钥名称（用于标题展示） */
  apiKeyName: {
    type: String,
    default: ''
  },
  /** 卡密列表数据 */
  cardCodes: {
    type: Array,
    default: () => []
  },
  /** 是否开启卡密加密（控制加密复制按钮显示） */
  enableCardEncryption: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'update:visible',        // 双向绑定弹窗可见性
  'generate',              // 生成卡密，携带 { count, type, value, stackTime }
  'copy-card',             // 复制卡密明文
  'copy-encrypted-card',   // 复制加密卡密
  'delete-card'            // 删除卡密
])

/** 生成参数 */
const genCount = ref(10)
const genType = ref('time')
const genValue = ref(30)
const genStackTime = ref(false)

/** 卡密状态文本映射 */
const getCardCodeStatusText = (status) => {
  const map = {
    'unused': '未使用',
    'used': '已使用',
    'merged': '已合并续期',
    'expired': '已过期'
  }
  return map[status] || status
}

/** 格式化日期 */
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}
</script>

<style scoped>
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
  backdrop-filter: blur(5px);
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  animation: modalSlideUp 0.3s ease-out;
}

@keyframes modalSlideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.large-modal {
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  padding: 1.5rem 1.5rem 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #333;
  font-size: 1.2rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #666;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 50%;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: #f8f9fa;
  color: #333;
}

.modal-body {
  padding: 1.5rem;
}

.card-codes-header {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e9ecef;
}

.card-codes-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.card-codes-generate-btn {
  flex-shrink: 0;
}

.form-group.inline {
  margin-bottom: 0;
  flex: 1;
  min-width: 120px;
}

.form-group.inline input,
.form-group.inline select {
  width: 100%;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: bold;
  color: #333;
  font-size: 0.9rem;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* 自定义下拉选择框 */
.custom-select-wrapper {
  position: relative;
  display: inline-block;
  width: 100%;
}

.custom-select {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
  font-size: 0.9rem;
  color: #495057;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.custom-select:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.custom-select:hover {
  border-color: #007bff;
}

.select-arrow {
  position: absolute;
  top: 50%;
  right: 0.75rem;
  transform: translateY(-50%);
  pointer-events: none;
  color: #6c757d;
  transition: transform 0.3s ease;
}

.custom-select:focus + .select-arrow {
  transform: translateY(-50%) rotate(180deg);
  color: #007bff;
}

/* 同机时长叠加开关 */
.stack-time-stack-group.api-exclusive-stack {
  margin-bottom: 0;
}

.stack-option-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1rem 1.125rem;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.stack-option-card--active {
  border-color: #c7d2fe;
  background: linear-gradient(165deg, #f8faff 0%, #ffffff 55%);
  box-shadow: 0 0 0 1px rgba(79, 70, 229, 0.07), 0 6px 20px rgba(79, 70, 229, 0.08);
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

/* 卡密列表 */
.card-codes-list {
  max-height: 400px;
  overflow-y: auto;
}

.card-code-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.2rem;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  margin-bottom: 0.75rem;
  background: #f8f9fa;
  gap: 1rem;
}

.card-code-info {
  flex: 1;
  min-width: 0;
}

.card-code-value {
  font-family: 'Courier New', monospace;
  background: white;
  padding: 0.75rem;
  border-radius: 4px;
  font-size: 0.85rem;
  color: #495057;
  border: 1px solid #dee2e6;
  display: block;
  margin-bottom: 0.75rem;
  word-break: break-all;
  line-height: 1.4;
}

.card-code-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: #666;
  align-items: center;
}

.card-code-meta .status {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: bold;
  font-size: 0.75rem;
}

.card-code-meta .status.unused {
  background: #dcfce7;
  color: #166534;
}

.card-code-meta .status.used {
  background: #dbeafe;
  color: #1e40af;
}

.card-code-meta .status.expired {
  background: #fee2e2;
  color: #991b1b;
}

.type-badge {
  background: #e0e7ff;
  color: #4f46e5;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
}

.card-code-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex-shrink: 0;
  min-width: 80px;
}

.btn-primary {
  background: #4f46e5;
  color: white;
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

.btn-primary:hover {
  background: #4338ca;
  transform: translateY(-1px);
}

.btn-danger {
  background: #ef4444;
  color: white;
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

.btn-danger:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.copy-btn {
  background: #4f46e5;
  color: white;
  border: none;
  padding: 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.copy-btn:hover {
  background: #4338ca;
  transform: scale(1.05);
}

.copy-btn.small,
.btn-danger.small {
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.copy-btn.warning {
  background: #f59e0b;
}

.copy-btn.warning:hover {
  background: #d97706;
}

.empty-card-codes {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.empty-card-codes i {
  color: #ccc;
  margin-bottom: 0.5rem;
  display: block;
  font-size: 2rem;
}

@media (max-width: 768px) {
  .modal-content {
    margin: 1rem;
    width: calc(100% - 2rem);
  }

  .large-modal {
    max-width: 95%;
    margin: 1rem;
  }

  .card-codes-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .card-codes-generate-btn {
    width: 100%;
  }

  .card-code-item {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .card-code-actions {
    flex-direction: row;
    justify-content: flex-end;
    min-width: auto;
  }

  .card-code-meta {
    flex-direction: column;
    gap: 0.5rem;
    align-items: flex-start;
  }
}
</style>
