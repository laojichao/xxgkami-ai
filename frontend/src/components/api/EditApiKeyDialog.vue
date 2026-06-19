<!-- 编辑API密钥弹窗：修改名称/描述/加密/机器码/同机同规格配置 -->
<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('update:visible', false)">
    <div class="modal-content" role="dialog" aria-modal="true" aria-labelledby="edit-api-key-dialog-title" @click.stop>
      <div class="modal-header">
        <h3 id="edit-api-key-dialog-title">编辑API密钥</h3>
        <button class="close-btn" @click="$emit('update:visible', false)">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body">
        <div class="form-group">
          <label>密钥名称</label>
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

        <div class="form-group">
          <label class="switch-container">
            <div class="switch">
              <input type="checkbox" v-model="form.requireMachineCode">
              <span class="slider round"></span>
            </div>
            <span class="switch-text">核销时强制传入机器码</span>
          </label>
          <small style="color: #666; font-size: 0.8rem; display: block; margin-top: 0.2rem;">开启后客户端必须在核销接口中附带 machine_code，避免未绑定机器前被多台设备抢先使用。</small>
        </div>

        <div class="form-group">
          <label>同机同规格仅一次（JSON）</label>
          <textarea v-model="form.machineSpecOnceConfig" rows="5" placeholder='例如：{"enabled":true,"rules":[{"card_type":"time","duration":1}]}'></textarea>
          <small style="color: #666; font-size: 0.8rem; display: block; margin-top: 0.2rem;">
            匹配的卡类型+规格在本密钥下每台机器码仅可成功核销一次。可写自定义 spec_key 精确指定一条规则。留空表示不启用；保存空内容将清空配置。
          </small>
        </div>

      </div>
      <div class="modal-actions">
        <button class="btn-secondary" @click="$emit('update:visible', false)">取消</button>
        <button class="btn-primary" @click="handleSubmit">
          <i class="fas fa-save"></i>
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'

/**
 * EditApiKeyDialog 组件
 * 编辑已有API密钥的弹窗表单，支持修改名称、描述、加密开关、机器码配置、同机同规格配置
 */
const props = defineProps({
  /** 控制弹窗显示/隐藏 */
  visible: {
    type: Boolean,
    default: false
  },
  /** 待编辑的API密钥数据 */
  apiKey: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits([
  'update:visible', // 双向绑定弹窗可见性
  'save'            // 提交保存请求
])

/** 表单数据 */
const form = reactive({
  id: null,
  name: '',
  description: '',
  isActive: true,
  enableCardEncryption: false,
  requireMachineCode: false,
  machineSpecOnceConfig: ''
})

/** 当 apiKey prop 变化时，同步到表单 */
watch(() => props.apiKey, (key) => {
  if (key && key.id) {
    form.id = key.id
    form.name = key.name || ''
    form.description = key.description || ''
    form.isActive = key.isActive !== undefined ? key.isActive : true
    form.enableCardEncryption = key.enableCardEncryption || false
    form.requireMachineCode = key.requireMachineCode || false
    form.machineSpecOnceConfig = key.machineSpecOnceConfig || ''
  }
}, { immediate: true, deep: true })

/** 提交保存 */
function handleSubmit() {
  emit('save', {
    id: form.id,
    name: form.name,
    description: form.description,
    isActive: form.isActive,
    enableCardEncryption: form.enableCardEncryption,
    requireMachineCode: form.requireMachineCode,
    machineSpecOnceConfig: form.machineSpecOnceConfig
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
