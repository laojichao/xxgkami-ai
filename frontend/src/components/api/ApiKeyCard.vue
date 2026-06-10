<!-- 单个API密钥卡片：展示密钥信息、元数据、操作按钮 -->
<template>
  <div class="api-key-card">
    <div class="api-key-info">
      <div class="api-key-header">
        <h3>{{ apiKey.name }}</h3>
        <span class="api-key-status" :class="{ active: apiKey.isActive }">
          {{ apiKey.isActive ? '活跃' : '未使用' }}
        </span>
      </div>
      <div class="api-key-value-container">
        <code class="api-key-value">{{ apiKey.key }}</code>
        <button class="copy-btn" @click="$emit('copy-key', apiKey.key)" title="复制API密钥">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
          复制
        </button>
      </div>
      <div class="api-key-meta">
        <div class="meta-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line><line x1="12" y1="15" x2="12" y2="15"></line></svg>
          <span>创建时间: {{ formatDate(apiKey.createdAt) }}</span>
        </div>
        <div class="meta-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          <span>最后使用: {{ apiKey.lastUsed ? formatDate(apiKey.lastUsed) : '从未使用' }}</span>
        </div>
        <div class="meta-item" v-if="apiKey.requestCount">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline></svg>
          <span>请求次数: {{ apiKey.requestCount }}</span>
        </div>
        <div class="meta-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
          <span>专属卡密: {{ apiKey.cardCodes ? apiKey.cardCodes.length : 0 }} 个</span>
        </div>
        <div class="meta-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          <span>绑定用户: {{ apiKey.assignedUsers ? apiKey.assignedUsers.length : 0 }} 个</span>
        </div>
      </div>
    </div>
    <div class="api-key-actions">
      <button class="btn-info" @click="$emit('manage-cards', apiKey)">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
        卡密管理
      </button>
      <button class="btn-info" @click="$emit('interface-settings', apiKey)">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V3.09a1.65 1.65 0 0 0 1.82.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0 .33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
        接口设置
      </button>
      <button class="btn-info" @click="$emit('manage-users', apiKey)">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
        用户管理
      </button>
      <button class="btn-secondary" @click="$emit('edit', apiKey)">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
        编辑
      </button>
      <button class="btn-warning" @click="$emit('toggle', apiKey)" :title="apiKey.isActive ? '禁用' : '启用'">
        <svg v-if="apiKey.isActive" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="4" width="4" height="16"></rect><rect x="14" y="4" width="4" height="16"></rect></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
        {{ apiKey.isActive ? '禁用' : '启用' }}
      </button>
      <button class="btn-danger" @click="$emit('delete', apiKey.id)">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
        删除
      </button>
    </div>
  </div>
</template>

<script setup>
/**
 * ApiKeyCard 组件
 * 展示单个API密钥的完整信息，包括名称、密钥值、创建时间、使用统计等
 * 提供卡密管理、接口设置、用户管理、编辑、启用/禁用、删除等操作按钮
 */
defineProps({
  /** API密钥对象，包含 id, name, key, isActive, createdAt, lastUsed, requestCount, cardCodes, assignedUsers 等字段 */
  apiKey: {
    type: Object,
    required: true
  }
})

defineEmits([
  'copy-key',          // 复制密钥值
  'manage-cards',      // 打开卡密管理
  'interface-settings', // 打开接口设置
  'manage-users',      // 打开用户管理
  'edit',              // 编辑密钥
  'toggle',            // 启用/禁用切换
  'delete'             // 删除密钥
])

/** 格式化日期为本地字符串 */
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}
</script>

<style scoped>
.api-key-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border: 1px solid #e1e5e9;
  transition: all 0.3s ease;
}

.api-key-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-color: #d1d5db;
}

.api-key-info {
  flex: 1;
  margin-right: 1rem;
}

.api-key-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.api-key-header h3 {
  margin: 0;
  color: #333;
  font-size: 1.1rem;
}

.api-key-status {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  background: #fee2e2;
  color: #991b1b;
}

.api-key-status.active {
  background: #dcfce7;
  color: #166534;
}

.api-key-value-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.api-key-value {
  font-family: 'Courier New', monospace;
  background: #f8f9fa;
  padding: 0.75rem;
  border-radius: 4px;
  font-size: 0.85rem;
  color: #495057;
  flex: 1;
  border: 1px solid #e9ecef;
  word-break: break-all;
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

.api-key-meta {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: #666;
}

.meta-item svg {
  color: #667eea;
}

.api-key-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 120px;
}

.btn-info {
  background: #0ea5e9;
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

.btn-info:hover {
  background: #0284c7;
  transform: translateY(-1px);
}

.btn-secondary {
  background: #6b7280;
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

.btn-secondary:hover {
  background: #4b5563;
  transform: translateY(-1px);
}

.btn-warning {
  background: #f59e0b;
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

.btn-warning:hover {
  background: #d97706;
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

@media (max-width: 768px) {
  .api-key-card {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .api-key-info {
    margin-right: 0;
  }

  .api-key-actions {
    flex-direction: row;
    min-width: auto;
  }

  .api-key-value-container {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
