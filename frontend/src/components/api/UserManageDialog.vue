<!-- 用户管理弹窗：为API密钥分配/移除用户 -->
<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('update:visible', false)">
    <div class="modal-content large-modal" @click.stop>
      <div class="modal-header">
        <h3>{{ apiKeyName }} - 用户管理</h3>
        <button class="close-btn" @click="$emit('update:visible', false)">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body">
        <!-- 添加用户工具栏 -->
        <div class="users-header">
          <div class="form-group inline">
            <label>添加用户</label>
            <div class="custom-select-wrapper">
              <select v-model="selectedUserId" class="custom-select">
                <option value="">选择用户</option>
                <option v-for="user in availableUsers" :key="user.id" :value="user.id">
                  {{ user.username }} ({{ user.email }})
                </option>
              </select>
              <div class="select-arrow">
                <i class="fas fa-chevron-down"></i>
              </div>
            </div>
          </div>
          <button class="btn-primary" @click="handleAssign" :disabled="!selectedUserId">
            <i class="fas fa-user-plus"></i>
            分配用户
          </button>
        </div>

        <!-- 已分配用户列表 -->
        <div class="assigned-users-list">
          <div class="user-item" v-for="user in assignedUsers" :key="user.id">
            <div class="user-info">
              <div class="user-avatar">
                <i class="fas fa-user"></i>
              </div>
              <div class="user-details">
                <h4>{{ user.username }}</h4>
                <p>{{ user.email }}</p>
                <small>分配时间: {{ formatDate(user.assignedAt) }}</small>
              </div>
            </div>
            <div class="user-actions">
              <button class="btn-danger small" @click="$emit('unassign', user.id)">
                <i class="fas fa-user-minus"></i>
                移除
              </button>
            </div>
          </div>

          <div v-if="!assignedUsers || assignedUsers.length === 0" class="empty-users">
            <i class="fas fa-users"></i>
            <p>暂无分配用户，该API密钥可被所有用户使用</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

/**
 * UserManageDialog 组件
 * 管理API密钥绑定用户的弹窗，支持从可用用户列表中分配、查看已分配用户、移除用户
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
  /** 可分配的用户列表（排除已分配用户） */
  availableUsers: {
    type: Array,
    default: () => []
  },
  /** 已分配的用户列表 */
  assignedUsers: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits([
  'update:visible', // 双向绑定弹窗可见性
  'assign',         // 分配用户，携带 userId
  'unassign'        // 移除用户，携带 userId
])

/** 选中的待分配用户ID */
const selectedUserId = ref('')

/** 触发分配 */
function handleAssign() {
  if (!selectedUserId.value) return
  emit('assign', selectedUserId.value)
  selectedUserId.value = ''
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

.users-header {
  display: flex;
  gap: 1rem;
  align-items: end;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e9ecef;
}

.form-group.inline {
  margin-bottom: 0;
  flex: 1;
  min-width: 120px;
}

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

.assigned-users-list {
  max-height: 400px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  margin-bottom: 0.5rem;
  background: #f8f9fa;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.2rem;
}

.user-details h4 {
  margin: 0 0 0.25rem 0;
  color: #333;
  font-size: 0.9rem;
}

.user-details p {
  margin: 0 0 0.25rem 0;
  color: #666;
  font-size: 0.8rem;
}

.user-details small {
  color: #999;
  font-size: 0.7rem;
}

.user-actions {
  display: flex;
  gap: 0.5rem;
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

.btn-primary:disabled {
  background: #6c757d;
  cursor: not-allowed;
  transform: none;
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

.btn-danger.small {
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.empty-users {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.empty-users i {
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

  .users-header {
    flex-direction: column;
    align-items: stretch;
  }

  .user-item {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .user-info {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}
</style>
