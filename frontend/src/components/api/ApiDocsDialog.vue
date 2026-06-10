<!-- API接口文档弹窗：展示接口参数和响应说明 -->
<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('update:visible', false)">
    <div class="modal-content large-modal" @click.stop>
      <div class="modal-header">
        <h3>API 接口文档</h3>
        <button class="close-btn" @click="$emit('update:visible', false)">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body docs-body">
        <div class="doc-section">
          <h4>1. 使用卡密接口</h4>
          <p>通过 API 密钥和卡密，直接使用/核销卡密。</p>

          <div class="endpoint-box">
            <span class="method-badge post">POST</span>
            <span class="method-badge get">GET</span>
            <code class="url">/api/v1/use_card</code>
          </div>

          <h5>请求参数</h5>
          <div class="table-container">
            <table class="params-table">
              <thead>
                <tr>
                  <th>参数名</th>
                  <th>必选</th>
                  <th>类型</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>api_key</td>
                  <td>是</td>
                  <td>String</td>
                  <td>您的 API 密钥 (api_key)</td>
                </tr>
                <tr>
                  <td>card_key</td>
                  <td>是</td>
                  <td>String</td>
                  <td>要使用的卡密</td>
                </tr>
                <tr>
                  <td>machine_code</td>
                  <td>否</td>
                  <td>String</td>
                  <td>机器码 (一机一码，首次使用时绑定)</td>
                </tr>
                <tr>
                  <td>ip_address</td>
                  <td>否</td>
                  <td>String</td>
                  <td>客户端IP (若不传则自动获取请求IP)</td>
                </tr>
              </tbody>
            </table>
          </div>

          <h5>响应示例</h5>
          <pre class="code-block">
{
  "code": 200,
  "message": "Card used successfully",
  "success": true
}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * ApiDocsDialog 组件
 * 展示API接口文档的弹窗，包括接口地址、请求参数、响应示例
 */
defineProps({
  /** 控制弹窗显示/隐藏 */
  visible: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'update:visible' // 双向绑定弹窗可见性
])
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

.docs-body {
  padding: 1.5rem;
  max-height: 70vh;
  overflow-y: auto;
}

.doc-section {
  margin-bottom: 2rem;
}

.doc-section h4 {
  margin-top: 0;
  margin-bottom: 0.5rem;
  color: #333;
}

.doc-section h5 {
  margin-top: 1.5rem;
  margin-bottom: 0.8rem;
  color: #555;
  font-size: 1rem;
}

.endpoint-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 1rem;
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin: 1rem 0;
}

.method-badge {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  text-transform: uppercase;
}

.method-badge.post {
  background: #dbeafe;
  color: #1e40af;
}

.method-badge.get {
  background: #dcfce7;
  color: #166534;
}

.url {
  font-family: monospace;
  color: #475569;
  font-size: 0.9rem;
}

.params-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.params-table th,
.params-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.params-table th {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
}

.code-block {
  background: #1e293b;
  color: #e2e8f0;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
  font-family: monospace;
  font-size: 0.85rem;
  margin: 0;
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
}
</style>
