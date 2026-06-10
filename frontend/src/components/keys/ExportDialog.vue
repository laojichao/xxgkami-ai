<!-- 导出数据弹窗：列选择/格式/范围/预览 -->
<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content export-modal" @click.stop>
      <div class="modal-header">
        <h3>导出卡密数据</h3>
        <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>
      <div class="modal-body">
        <div class="export-settings">
          <div class="setting-group">
            <h4>选择导出列</h4>
            <div class="checkbox-grid">
              <label v-for="col in availableColumns" :key="col.key" class="checkbox-label">
                <input type="checkbox" :checked="selectedColumns.includes(col.key)" @change="toggleColumn(col.key)">
                {{ col.label }}
              </label>
            </div>
          </div>
          <div class="setting-group">
            <h4>导出格式</h4>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" :checked="exportFormat === 'xlsx'" @change="$emit('update:exportFormat', 'xlsx')"> Excel (.xlsx)
              </label>
              <label class="radio-label">
                <input type="radio" :checked="exportFormat === 'csv'" @change="$emit('update:exportFormat', 'csv')"> CSV (.csv)
              </label>
            </div>
          </div>
        </div>
        <div class="setting-group export-scope-group">
          <h4>导出范围</h4>
          <p class="export-scope-hint">会先应用上方「机器码搜索」的结果，再按使用状态筛选；选「全部」则仅受机器码搜索影响。</p>
          <div class="radio-group horizontal">
            <label class="radio-label">
              <input type="radio" :checked="exportUsageScope === 'all'" @change="$emit('update:exportUsageScope', 'all')"> 全部（未按状态筛选）
            </label>
            <label class="radio-label">
              <input type="radio" :checked="exportUsageScope === 'unused'" @change="$emit('update:exportUsageScope', 'unused')"> 仅未使用
            </label>
            <label class="radio-label">
              <input type="radio" :checked="exportUsageScope === 'used'" @change="$emit('update:exportUsageScope', 'used')"> 仅已使用（含已暂停）
            </label>
          </div>
        </div>

        <div class="preview-section">
          <h4>数据预览 (前 5 条，共符合条件 {{ keysForExportCount }} 条)</h4>
          <div class="preview-table-container">
            <table class="preview-table">
              <thead>
                <tr>
                  <th v-for="colKey in selectedColumns" :key="colKey">
                    {{ getColumnLabel(colKey) }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in previewData" :key="index">
                  <td v-for="colKey in selectedColumns" :key="colKey">
                    {{ row[colKey] }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn-secondary" @click="$emit('close')">取消</button>
        <button class="btn-primary" @click="$emit('export')" :disabled="selectedColumns.length === 0 || exporting">
          <i class="fas" :class="exporting ? 'fa-spinner fa-spin' : 'fa-file-export'"></i>
          {{ exporting ? '导出中...' : '确认导出' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  availableColumns: { type: Array, default: () => [] },
  selectedColumns: { type: Array, default: () => [] },
  exportFormat: { type: String, default: 'xlsx' },
  exportUsageScope: { type: String, default: 'all' },
  previewData: { type: Array, default: () => [] },
  keysForExportCount: { type: Number, default: 0 },
  exporting: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'export', 'update:selectedColumns', 'update:exportFormat', 'update:exportUsageScope'])

const toggleColumn = (key) => {
  const cols = [...props.selectedColumns]
  const idx = cols.indexOf(key)
  if (idx >= 0) {
    cols.splice(idx, 1)
  } else {
    cols.push(key)
  }
  emit('update:selectedColumns', cols)
}

const getColumnLabel = (key) => {
  const col = props.availableColumns.find(c => c.key === key)
  return col ? col.label : key
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
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.export-modal {
  max-width: 800px;
  width: 90%;
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

.export-settings {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
  margin-bottom: 2rem;
}

.setting-group h4 {
  margin: 0 0 1rem 0;
  color: #2d3748;
  font-size: 1rem;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 0.75rem;
}

.checkbox-label, .radio-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  color: #4a5568;
  font-size: 0.875rem;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.radio-group.horizontal {
  flex-direction: row;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 0.75rem 1.25rem;
}

.export-scope-group {
  margin-bottom: 1rem;
}

.export-scope-hint {
  margin: 0 0 0.75rem 0;
  font-size: 0.8125rem;
  color: #718096;
  line-height: 1.5;
}

.preview-section {
  border-top: 1px solid #e2e8f0;
  padding-top: 1.5rem;
}

.preview-section h4 {
  margin: 0 0 1rem 0;
  color: #2d3748;
  font-size: 1rem;
}

.preview-table-container {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.preview-table th, .preview-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  white-space: nowrap;
}

.preview-table th {
  background: #f7fafc;
  font-weight: 600;
  color: #4a5568;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  padding: 0 1.5rem 1.5rem;
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

.btn-primary:hover:not(:disabled) {
  background: #4338ca;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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
  .export-settings {
    grid-template-columns: 1fr;
    gap: 1.5rem;
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
}
</style>
