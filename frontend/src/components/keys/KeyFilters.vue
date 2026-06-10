<!-- 搜索筛选区域：机器码搜索 + 列表统计信息 -->
<template>
  <div class="keys-toolbar">
    <div class="toolbar-search">
      <label class="toolbar-label" for="machine-code-search">机器码搜索</label>
      <input
        id="machine-code-search"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        type="search"
        class="toolbar-input"
        placeholder="输入设备码关键字，筛选已绑定该机器码的卡密"
        autocomplete="off"
        spellcheck="false"
      />
      <button v-if="modelValue" type="button" class="toolbar-clear" @click="$emit('update:modelValue', '')">
        清除
      </button>
    </div>
    <p class="toolbar-meta">
      当前列表：<strong>{{ filteredCount }}</strong> 条
      <template v-if="modelValue">（已按机器码过滤）</template>
      <span class="toolbar-divider">|</span>
      全库共计 {{ totalCount }} 条
    </p>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: '' },
  filteredCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.keys-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem 1.5rem;
  padding: 1rem 2rem 1.25rem;
  background: white;
  border-bottom: 1px solid #e1e5e9;
}

.toolbar-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 0.75rem;
  flex: 1;
  min-width: 220px;
}

.toolbar-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #4a5568;
  white-space: nowrap;
}

.toolbar-input {
  flex: 1;
  min-width: 200px;
  max-width: 480px;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
}

.toolbar-input:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 3px rgba(49, 130, 206, 0.15);
}

.toolbar-clear {
  background: none;
  border: none;
  color: #718096;
  font-size: 0.8125rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
}

.toolbar-clear:hover {
  color: #2d3748;
  text-decoration: underline;
}

.toolbar-meta {
  margin: 0;
  font-size: 0.8125rem;
  color: #718096;
}

.toolbar-divider {
  margin: 0 0.35rem;
  opacity: 0.5;
}
</style>
