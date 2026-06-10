<!-- 批量操作栏：导出数据 + 生成卡密按钮 -->
<template>
  <div class="section-header">
    <h2>卡密管理</h2>
    <div class="header-actions">
      <button class="btn-secondary" @click="$emit('show-export')">
        <i class="fas fa-file-export"></i>
        导出数据
      </button>
      <button class="btn-primary" @click="$emit('show-create')">
        <i class="fas fa-plus"></i>
        生成卡密
      </button>
    </div>
  </div>
</template>

<script setup>
defineEmits(['show-export', 'show-create'])
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0;
  padding: 2rem;
  background: white;
  border-bottom: 1px solid #e1e5e9;
}

.section-header h2 {
  color: #2d3748;
  margin: 0;
  font-size: 1.75rem;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

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

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
    padding: 1rem;
  }

  .section-header h2 {
    font-size: 1.5rem;
    text-align: center;
  }
}
</style>
