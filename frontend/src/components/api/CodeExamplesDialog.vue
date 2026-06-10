<!-- 核销接口多语言代码示例弹窗：支持语言切换/一键复制/语法高亮 -->
<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('update:visible', false)">
    <div class="modal-content large-modal code-examples-modal" @click.stop>
      <div class="modal-header">
        <h3>核销接口代码实例（use_card）</h3>
        <button type="button" class="close-btn" @click="$emit('update:visible', false)">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body code-examples-body">
        <p class="code-examples-intro">{{ useCardCodeIntroText }}</p>
        <p class="code-examples-hint">
          当前环境 API 根路径参考：<code>{{ apiBaseUrlHint }}</code>
          — 请将各示例中的 <code>BASE_URL</code> 换为此值（勿以 <code>/</code> 结尾）。
        </p>
        <div class="code-examples-layout">
          <aside class="code-examples-lang" aria-label="语言列表">
            <button
              v-for="ex in API_USE_CARD_EXAMPLES"
              :key="ex.id"
              type="button"
              class="code-lang-btn"
              :class="{ active: selectedExampleId === ex.id }"
              @click="selectedExampleId = ex.id"
            >
              {{ ex.label }}
            </button>
          </aside>
          <div class="code-examples-panel">
            <div class="code-examples-toolbar-inner">
              <span class="code-examples-lang-title">{{ currentExample?.label }}</span>
              <button type="button" class="btn-primary btn-code-copy" @click="$emit('copy-code', currentExample?.code)">
                复制代码
              </button>
            </div>
            <pre class="code-block code-examples-pre"><code class="hljs" v-html="highlightedHtml"></code></pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { highlightUseCardExample } from '../../utils/useCardCodeHighlight.js'
import { API_USE_CARD_INTRO, API_USE_CARD_EXAMPLES } from '../../data/apiUseCardCodeExamples.js'

/**
 * CodeExamplesDialog 组件
 * 展示核销接口在多种编程语言中的调用示例，支持语言切换和语法高亮
 */
const props = defineProps({
  /** 控制弹窗显示/隐藏 */
  visible: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'update:visible', // 双向绑定弹窗可见性
  'copy-code'       // 复制代码，携带代码字符串
])

/** 当前选中的代码示例语言ID */
const selectedExampleId = ref(API_USE_CARD_EXAMPLES[0]?.id ?? 'curl')

/** 弹窗打开时重置选中语言 */
watch(() => props.visible, (val) => {
  if (val) {
    selectedExampleId.value = API_USE_CARD_EXAMPLES[0]?.id ?? 'curl'
  }
})

/** 当前环境的API根路径 */
const apiBaseUrlHint = computed(() => {
  const raw = import.meta.env?.VITE_API_BASE_URL
  let base = typeof raw === 'string' && raw.trim() ? raw.trim() : '/api'
  base = base.replace(/\/+$/, '')
  if (base.startsWith('http://') || base.startsWith('https://')) {
    return base
  }
  if (typeof window !== 'undefined') {
    const prefix = base.startsWith('/') ? base : `/${base}`
    return `${window.location.origin}${prefix}`
  }
  return base
})

/** 简介文本 */
const useCardCodeIntroText = computed(() =>
  API_USE_CARD_INTRO.replace(/\{BASE_URL\}/g, apiBaseUrlHint.value)
)

/** 当前选中的示例对象 */
const currentExample = computed(
  () =>
    API_USE_CARD_EXAMPLES.find((e) => e.id === selectedExampleId.value) ?? API_USE_CARD_EXAMPLES[0]
)

/** 高亮后的HTML */
const highlightedHtml = computed(() => {
  const ex = currentExample.value
  if (!ex?.code) return ''
  return highlightUseCardExample(ex.code, ex.id)
})
</script>

<style scoped>
@import 'highlight.js/styles/github-dark.css';

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

.code-examples-modal {
  max-width: min(960px, 96vw);
  width: 100%;
  display: flex;
  flex-direction: column;
  max-height: 86vh;
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

.code-examples-body {
  padding: 1rem 1.25rem 1.25rem;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.code-examples-intro {
  white-space: pre-line;
  margin: 0 0 0.65rem;
  color: #374151;
  font-size: 0.9rem;
  line-height: 1.5;
}

.code-examples-hint {
  margin: 0 0 1rem;
  font-size: 0.82rem;
  color: #64748b;
  line-height: 1.5;
}

.code-examples-hint code {
  background: #f1f5f9;
  padding: 0.1em 0.35em;
  border-radius: 4px;
  font-size: 0.9em;
}

.code-examples-layout {
  display: flex;
  gap: 0.75rem;
  flex: 1;
  min-height: 0;
  align-items: stretch;
}

.code-examples-lang {
  flex: 0 0 11.5rem;
  max-height: min(52vh, 420px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 0.5rem;
  margin-right: 0.25rem;
  border-right: 1px solid #e5e7eb;
}

.code-lang-btn {
  text-align: left;
  padding: 0.45rem 0.6rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
  font-size: 0.8rem;
  color: #334155;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.code-lang-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.code-lang-btn.active {
  background: #eef2ff;
  border-color: #a5b4fc;
  color: #3730a3;
  font-weight: 600;
}

.code-examples-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.code-examples-toolbar-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
  flex-shrink: 0;
}

.code-examples-lang-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-code-copy {
  flex-shrink: 0;
  padding: 0.4rem 0.85rem;
  font-size: 0.8rem;
}

.code-examples-pre {
  flex: 1;
  min-height: 180px;
  max-height: min(52vh, 420px);
  margin: 0;
  overflow: auto;
  white-space: pre;
  tab-size: 2;
  background: #1e293b;
  color: #e2e8f0;
  padding: 1rem;
  border-radius: 6px;
  font-family: monospace;
  font-size: 0.85rem;
}

.code-examples-pre code.hljs {
  display: block;
  padding: 0;
  background: transparent !important;
  overflow: visible;
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

@media (max-width: 768px) {
  .modal-content {
    margin: 1rem;
    width: calc(100% - 2rem);
  }

  .code-examples-layout {
    flex-direction: column;
  }

  .code-examples-lang {
    flex: none;
    max-height: none;
    flex-direction: row;
    flex-wrap: wrap;
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
    padding-right: 0;
    margin-right: 0;
    padding-bottom: 0.5rem;
    margin-bottom: 0.5rem;
  }
}
</style>
