<!-- 创建API密钥弹窗：输入名称/描述/加密开关 -->
<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('update:visible', false)">
    <div class="modal-content" role="dialog" aria-modal="true" aria-labelledby="create-api-key-dialog-title" @click.stop>
      <div class="modal-header">
        <h3 id="create-api-key-dialog-title">创建新的API密钥</h3>
        <button class="close-btn" @click="$emit('update:visible', false)">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>密钥名称 <span class="required">*</span></label>
          <input type="text" v-model="form.name" placeholder="请输入密钥名称" />
        </div>
        <div class="form-group">
          <label>描述</label>
          <textarea v-model="form.description" rows="3" placeholder="请输入密钥描述（可选）"></textarea>
        </div>
        <div class="form-group">
          <label class="switch-container">
            <div class="switch">
              <input type="checkbox" v-model="form.enableCardEncryption">
              <span class="slider round"></span>
            </div>
            <span class="switch-text">开启卡密加密验证</span>
          </label>
          <small style="color: #666; font-size: 0.8rem; display: block; margin-top: 0.2rem;">开启后，调用接口必须传入加密后的卡密，系统会自动解密验证。</small>
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn-secondary" @click="$emit('update:visible', false)">取消</button>
        <button class="btn-primary" @click="handleSubmit" :disabled="!form.name.trim()">
          <i class="fas fa-key"></i>
          创建密钥
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'

/**
 * CreateApiKeyDialog 组件
 * 创建新API密钥的弹窗表单，包含名称、描述、卡密加密开关
 */
const props = defineProps({
  /** 控制弹窗显示/隐藏 */
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'update:visible', // 双向绑定弹窗可见性
  'create'          // 提交创建请求，携带 { name, description, enableCardEncryption }
])

/** 表单数据 */
const form = reactive({
  name: '',
  description: '',
  enableCardEncryption: false
})

/** 弹窗关闭时重置表单 */
watch(() => props.visible, (val) => {
  if (!val) {
    form.name = ''
    form.description = ''
    form.enableCardEncryption = false
  }
})

/** 提交创建 */
function handleSubmit() {
  if (!form.name.trim()) return
  emit('create', {
    name: form.name,
    description: form.description,
    enableCardEncryption: form.enableCardEncryption
  })
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

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: bold;
  color: #333;
  font-size: 0.9rem;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.required {
  color: #dc3545;
  font-weight: bold;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding: 0 1.5rem 1.5rem;
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

.btn-primary:disabled:hover {
  background: #6c757d;
  transform: none;
  box-shadow: none;
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

/* 开关样式 */
.switch-container {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3);
}

input:checked + .slider {
  background-color: #2196F3;
}

input:focus + .slider {
  box-shadow: 0 0 1px #2196F3;
}

input:checked + .slider:before {
  transform: translateX(20px);
}

.slider.round {
  border-radius: 34px;
}

.slider.round:before {
  border-radius: 50%;
}

.switch-text {
  font-size: 0.9rem;
  color: #555;
  font-weight: 500;
}

@media (max-width: 768px) {
  .modal-content {
    margin: 1rem;
    width: calc(100% - 2rem);
  }
}
</style>
