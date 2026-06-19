<!-- 接口回调设置弹窗：配置Webhook URL/请求方式/自定义参数/返回配置/状态码 -->
<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('update:visible', false)">
    <div class="modal-content large-modal" role="dialog" aria-modal="true" aria-labelledby="interface-settings-dialog-title" @click.stop>
      <div class="modal-header">
        <h3 id="interface-settings-dialog-title">{{ apiKeyName }} - 接口回调设置</h3>
        <div class="modal-header-right">
          <button type="button" class="btn-doc" @click.stop="toggleDocPanel" title="查看配置说明">
            <i class="fas fa-book"></i> 文档
          </button>
          <button class="close-btn" @click="$emit('update:visible', false)">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
      <div class="modal-body">
        <div class="interface-settings">
          <!-- 回调URL配置 -->
          <div class="form-group">
            <label>回调 URL (Webhook)</label>
            <div
              class="stack-option-card webhook-url-toggle-card"
              :class="{ 'stack-option-card--active': config.isCustomUrl }"
            >
              <label class="stack-toggle-row">
                <span class="stack-switch">
                  <input
                    type="checkbox"
                    v-model="config.isCustomUrl"
                    class="stack-switch-input"
                  />
                  <span class="stack-switch-track">
                    <span class="stack-switch-thumb"></span>
                  </span>
                </span>
                <span class="stack-toggle-copy">
                  <span class="stack-toggle-title">
                    自定义回调 URL
                    <span v-if="config.isCustomUrl" class="stack-toggle-pill">已开启</span>
                  </span>
                  <span class="stack-toggle-desc">
                    开启后可手动填写 Webhook 地址；关闭后由系统根据当前环境自动生成默认回调 URL。
                  </span>
                </span>
              </label>
            </div>
            <input type="text" v-model="config.url" :disabled="!config.isCustomUrl" :placeholder="config.isCustomUrl ? 'http://your-server.com/callback' : '系统将自动生成回调 URL'" />
            <small v-if="config.isCustomUrl">当卡密核销成功后，系统将请求此 URL</small>
            <small v-else>系统自动设置 URL (默认: http://{client_ip}:8888/callback)，支持变量替换</small>
          </div>

          <!-- 请求方式选择 -->
          <div class="form-group">
            <label>请求方式</label>
            <div class="method-selector-group">
              <div
                class="method-option"
                :class="{ active: config.method === 'GET' }"
                @click="config.method = 'GET'"
              >
                <div class="radio-circle"></div>
                <span class="method-name">GET</span>
              </div>
              <div
                class="method-option"
                :class="{ active: config.method === 'POST' }"
                @click="config.method = 'POST'"
              >
                <div class="radio-circle"></div>
                <span class="method-name">POST</span>
              </div>
            </div>
          </div>

          <!-- 自定义参数配置 -->
          <div class="params-config">
            <div class="params-header">
              <label>自定义参数配置 (输入)</label>
              <button class="btn-primary small" @click="addParam">
                <i class="fas fa-plus"></i> 添加参数
              </button>
            </div>

            <div class="params-list">
              <div class="param-row header">
                <span>参数名</span>
                <span>值类型</span>
                <span>值/变量</span>
                <span>操作</span>
              </div>
              <div class="param-row" v-for="(param, index) in config.params" :key="'param-'+index">
                <input type="text" v-model="param.key" placeholder="key" />
                <select v-model="param.type">
                  <option value="fixed">固定值</option>
                  <option value="variable">系统变量</option>
                </select>
                <div class="value-input">
                  <input v-if="param.type === 'fixed'" type="text" v-model="param.value" placeholder="value" />
                  <select v-else v-model="param.value">
                    <option value="time">当前时间 (time)</option>
                    <option value="client_ip">使用者IP (client_ip)</option>
                    <option value="card_key">卡密 (card_key)</option>
                    <option value="api_key">API Key (api_key)</option>
                    <option value="machine_code">机器码 (machine_code)</option>
                    <option value="remaining_time">剩余时间 (remaining_time)</option>
                    <option value="remaining_count">剩余次数 (remaining_count)</option>
                  </select>
                </div>
                <div class="row-actions">
                  <button class="btn-icon" @click="moveParam(index, -1)" :disabled="index === 0" title="上移">
                    ↑
                  </button>
                  <button class="btn-icon" @click="moveParam(index, 1)" :disabled="index === config.params.length - 1" title="下移">
                    ↓
                  </button>
                  <button class="btn-danger small" @click="removeParam(index)">
                    <i class="fas fa-trash"></i>
                  </button>
                </div>
              </div>
              <div v-if="config.params.length === 0" class="empty-params">
                暂无自定义参数，点击右上角添加
              </div>
            </div>
          </div>

          <!-- 自定义返回配置 -->
          <div class="params-config" style="margin-top: 1.5rem;">
            <div class="params-header">
              <label>自定义返回配置 (JSON)</label>
              <button class="btn-primary small" @click="addResponseField">
                <i class="fas fa-plus"></i> 添加字段
              </button>
            </div>

            <div class="params-list">
              <div class="param-row header">
                <span>字段名 (Key)</span>
                <span>值类型</span>
                <span>值/变量</span>
                <span>操作</span>
              </div>
              <div class="param-row" v-for="(param, index) in config.response" :key="'resp-'+index">
                <input type="text" v-model="param.key" placeholder="json key" />
                <select v-model="param.type">
                  <option value="fixed">固定值</option>
                  <option value="variable">系统变量</option>
                </select>
                <div class="value-input">
                  <input v-if="param.type === 'fixed'" type="text" v-model="param.value" placeholder="value" />
                  <select v-else v-model="param.value">
                    <option value="success">成功标识 (true/false)</option>
                    <option value="message">提示信息 (Success/Error)</option>
                    <option value="status_code">状态码 (Code)</option>
                    <option value="remaining_time">剩余时间 (秒)</option>
                    <option value="remaining_count">剩余次数</option>
                    <option value="card_key">卡密</option>
                    <option value="expire_time">过期时间</option>
                    <option value="card_type">卡密类型</option>
                    <option value="card_status">卡密状态 (yes/no)</option>
                    <option value="machine_code">机器码</option>
                  </select>
                </div>
                <div class="row-actions">
                  <button class="btn-icon" @click="moveResponseField(index, -1)" :disabled="index === 0" title="上移">
                    ↑
                  </button>
                  <button class="btn-icon" @click="moveResponseField(index, 1)" :disabled="index === config.response.length - 1" title="下移">
                    ↓
                  </button>
                  <button class="btn-danger small" @click="removeResponseField(index)">
                    <i class="fas fa-trash"></i>
                  </button>
                </div>
              </div>
              <div v-if="!config.response || config.response.length === 0" class="empty-params">
                默认返回: {"success": true, "message": "..."}
              </div>
            </div>
          </div>

          <!-- 状态码配置 -->
          <div class="params-config" style="margin-top: 1.5rem;" v-if="config.statusCodes">
            <div class="params-header">
              <label>状态码配置 (Status Codes)</label>
              <button class="btn-secondary small" @click="restoreDefaultStatusCodes">
                <i class="fas fa-undo"></i> 恢复默认
              </button>
            </div>

            <div class="params-list">
              <div class="param-row header">
                <span>场景 (Scenario)</span>
                <span>状态码 (Value)</span>
                <span>说明</span>
              </div>
              <div class="param-row" v-for="(code, index) in config.statusCodes" :key="'status-'+index" style="grid-template-columns: 2fr 2fr 3fr;">
                <span style="padding: 0.5rem; color: #495057;">{{ code.label }}</span>
                <input type="text" v-model="code.value" placeholder="code" />
                <span style="padding: 0.5rem; color: #999; font-size: 0.8rem;">对应变量: status_code</span>
              </div>
            </div>
          </div>

          <!-- 链接/返回预览 -->
          <div class="url-preview-section">
            <div class="preview-row">
              <div class="preview-col">
                <label>实时链接预览 (Request)</label>
                <div class="preview-box">
                  <code class="preview-url">{{ previewUrl }}</code>
                  <button class="copy-btn small" @click="$emit('copy-preview', previewUrl)" title="复制链接">
                    复制
                  </button>
                </div>
              </div>
            </div>

            <div class="preview-row" style="margin-top: 1rem;">
              <div class="preview-col">
                <label>实时返回预览 (Response JSON)</label>
                <div class="preview-box json-preview">
                  <pre>{{ previewResponseJson }}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="$emit('update:visible', false)">取消</button>
          <button class="btn-primary" @click="$emit('save', JSON.parse(JSON.stringify(config)))">保存配置</button>
        </div>
      </div>
    </div>

    <!-- 接口回调配置说明浮动面板 -->
    <Teleport to="body">
      <div
        v-if="docVisible"
        ref="docPanelRef"
        class="interface-doc-panel"
        :class="{ 'interface-doc-panel--fullscreen': docFullscreen }"
        :style="docPanelStyle"
      >
        <div class="interface-doc-header" @mousedown="startDocPanelDrag">
          <span class="interface-doc-title">接口回调配置说明</span>
          <div class="interface-doc-toolbar" @mousedown.stop>
            <button type="button" class="interface-doc-tool-btn" @click="toggleDocFullscreen" :title="docFullscreen ? '退出全屏' : '全屏'">
              <i :class="docFullscreen ? 'fas fa-compress' : 'fas fa-expand'"></i>
            </button>
            <button type="button" class="interface-doc-tool-btn" @click="docVisible = false" title="关闭">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>
        <div class="interface-doc-body">
          <div class="interface-doc-callout">
            <strong>新手提示：</strong>绝大多数场景下<strong>无需修改</strong>系统自动生成的<strong>回调 URL</strong>与默认的<strong>请求方式（GET）</strong>。仅在您的业务有特殊要求时，再调整请求方式或开启「自定义回调 URL」填写自有地址。
          </div>

          <section class="interface-doc-section">
            <h4>一、自定义参数配置（输入）</h4>
            <p>定义系统在核销成功后，向您的 Webhook 发起请求时<strong>附带哪些查询参数（GET）或表单/JSON 字段（POST）</strong>。</p>
            <ul>
              <li><strong>参数名</strong>：对方接口约定的 key，例如 <code>token</code>、<code>cdkey</code>。</li>
              <li><strong>值类型 · 固定值</strong>：每次请求都传同一个字符串，适合固定密钥等。</li>
              <li><strong>值类型 · 系统变量</strong>：由本系统自动填充，例如卡密 <code>card_key</code>、API Key、机器码 <code>machine_code</code>、剩余时间/次数等。</li>
              <li>使用「上移 / 下移」可调整参数顺序；GET 请求下参数会按顺序拼接到 URL 查询串。</li>
            </ul>
          </section>

          <section class="interface-doc-section">
            <h4>二、自定义返回配置（JSON）</h4>
            <p>用于定义当您的接口<strong>返回给客户端</strong>时，JSON 里各字段如何由系统变量或固定值组成（与下方「实时返回预览」一致）。</p>
            <ul>
              <li><strong>字段名 (Key)</strong>：返回 JSON 中的属性名，如 <code>success</code>、<code>msg</code>、<code>code</code>。</li>
              <li><strong>系统变量</strong>：将核销结果映射到字段，例如成功标识、提示信息、状态码、剩余时间、卡密类型等。</li>
              <li>请至少保留能表达「成功 / 失败」的字段，并与<strong>状态码配置</strong>中的逻辑一致，便于客户端判断。</li>
            </ul>
          </section>

          <section class="interface-doc-section">
            <h4>三、状态码配置（Status Codes）</h4>
            <p>为各类业务结果指定<strong>数字状态码</strong>（会映射到变量 <code>status_code</code>，供自定义返回 JSON 使用）。</p>
            <ul>
              <li>每一行对应一种场景（如验证成功、卡密不存在、已过期、机器码不匹配等）。</li>
              <li>修改「状态码 (Value)」即可；若不确定，可点击「恢复默认」还原推荐值。</li>
              <li>请在「自定义返回配置」中选用变量 <code>status_code</code>，与这里配置保持一致，客户端才能正确分支。</li>
            </ul>
          </section>

          <section class="interface-doc-section interface-doc-muted">
            <p>拖动顶部标题栏可移动窗口；拖动右下角斜线区域可调整大小；全屏后便于阅读长文档，再次点击可还原并回到右下角默认尺寸。</p>
          </section>
        </div>
        <div
          v-if="!docFullscreen"
          class="interface-doc-resize"
          @mousedown.stop.prevent="startDocPanelResize"
          title="拖动调整大小"
        />
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'

/**
 * InterfaceSettingsDialog 组件
 * 配置API密钥的接口回调设置，包括Webhook URL、请求方式、自定义输入参数、自定义返回JSON、状态码
 * 包含可拖动/缩放/全屏的配置说明浮动面板
 * 包含实时链接预览和返回JSON预览
 */
const props = defineProps({
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
  /** 当前API密钥对象（用于加载已有配置和生成预览URL） */
  apiKey: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits([
  'update:visible', // 双向绑定弹窗可见性
  'save',           // 保存配置，携带完整的config对象
  'copy-preview'    // 复制预览内容
])

/** 默认状态码配置 */
const DEFAULT_STATUS_CODES = [
  { key: 'success', label: '验证成功', value: '200' },
  { key: 'not_found', label: '卡密不存在', value: '404' },
  { key: 'expired', label: '卡密已过期', value: '401' },
  { key: 'used', label: '卡密已使用/停用', value: '402' },
  { key: 'no_count', label: '次数已用尽', value: '403' },
  { key: 'machine_code_mismatch', label: '机器码不匹配', value: '406' },
  { key: 'reverify_denied', label: '不允许重复验证', value: '407' },
  { key: 'machine_code_required', label: '需提供机器码', value: '408' },
  { key: 'spec_once_used', label: '同机同规格已用', value: '409' },
  { key: 'merged', label: '卡密已合并续期', value: '410' },
  { key: 'spec_once_concurrency', label: '核销冲突', value: '411' },
  { key: 'error', label: '其他错误', value: '500' }
]

/** 接口回调配置表单数据 */
const config = reactive({
  url: '',
  method: 'GET',
  isCustomUrl: true,
  params: [],
  response: [],
  statusCodes: DEFAULT_STATUS_CODES.map(c => ({ ...c }))
})

/** 浮动文档面板状态 */
const docVisible = ref(false)
const docFullscreen = ref(false)
const docPanelRef = ref(null)
const docPanelLayout = reactive({
  x: null,
  y: null,
  w: 440,
  h: 420
})
// 记录当前拖拽/缩放活动的清理函数，用于对话框关闭时主动移除残留监听器
let activeDragCleanup = null

/** 浮动面板样式计算 */
const docPanelStyle = computed(() => {
  if (docFullscreen.value) {
    return {
      position: 'fixed',
      left: '0',
      top: '0',
      width: '100vw',
      height: '100vh',
      maxWidth: '100vw',
      maxHeight: '100vh',
      zIndex: '10050'
    }
  }
  const base = {
    position: 'fixed',
    width: `${docPanelLayout.w}px`,
    height: `${docPanelLayout.h}px`,
    zIndex: '10050',
    minWidth: '300px',
    minHeight: '220px'
  }
  if (docPanelLayout.x != null && docPanelLayout.y != null) {
    return {
      ...base,
      left: `${docPanelLayout.x}px`,
      top: `${docPanelLayout.y}px`,
      right: 'auto',
      bottom: 'auto'
    }
  }
  return {
    ...base,
    right: '24px',
    bottom: '24px',
    left: 'auto',
    top: 'auto'
  }
})

/** 根据当前环境自动生成默认回调URL */
function generateDefaultUrl() {
  const protocol = window.location.protocol
  const host = window.location.host
  const key = props.apiKey?.key || '{api_key}'
  return `${protocol}//${host}/api/custom/${key}/use`
}

/** 弹窗打开/关闭时初始化或重置配置 */
watch(() => props.visible, (open) => {
  if (open) {
    loadConfig()
  } else {
    docVisible.value = false
    docFullscreen.value = false
    docPanelLayout.x = null
    docPanelLayout.y = null
    docPanelLayout.w = 440
    docPanelLayout.h = 420
    // 主动移除可能残留的拖拽/缩放 document 监听器，防止内存泄漏
    if (activeDragCleanup) {
      activeDragCleanup()
      activeDragCleanup = null
    }
  }
})

/** 从apiKey加载已有配置 */
function loadConfig() {
  const key = props.apiKey
  if (key?.webhookConfig) {
    const wc = key.webhookConfig
    config.method = wc.method || 'GET'
    config.params = wc.params || []
    config.response = wc.response || []
    config.isCustomUrl = wc.isCustomUrl !== undefined ? wc.isCustomUrl : true

    if (wc.statusCodes && wc.statusCodes.length > 0) {
      config.statusCodes = DEFAULT_STATUS_CODES.map(def => {
        const saved = wc.statusCodes.find(s => s.key === def.key)
        return saved ? { ...def, value: saved.value } : { ...def }
      })
    } else {
      config.statusCodes = DEFAULT_STATUS_CODES.map(c => ({ ...c }))
    }

    if (!config.isCustomUrl) {
      config.url = generateDefaultUrl()
    } else {
      config.url = wc.url || ''
    }
  } else {
    config.method = 'GET'
    config.params = []
    config.response = [
      { key: 'code', type: 'variable', value: 'status_code' },
      { key: 'msg', type: 'variable', value: 'message' },
      { key: 'data', type: 'variable', value: 'remaining_count' }
    ]
    config.statusCodes = DEFAULT_STATUS_CODES.map(c => ({ ...c }))
    config.isCustomUrl = true
    config.url = generateDefaultUrl()
  }
}

/** 自动生成URL（非自定义模式下） */
watch(() => config.isCustomUrl, (isCustom) => {
  if (!isCustom) {
    config.url = generateDefaultUrl()
  }
})

/** 切换文档面板显示 */
function toggleDocPanel() {
  docVisible.value = !docVisible.value
}

/** 切换文档面板全屏 */
function toggleDocFullscreen() {
  docFullscreen.value = !docFullscreen.value
  if (!docFullscreen.value) {
    docPanelLayout.x = null
    docPanelLayout.y = null
    docPanelLayout.w = 440
    docPanelLayout.h = 420
  }
}

/** 拖动文档面板 */
function startDocPanelDrag(e) {
  if (docFullscreen.value) return
  if (e.button !== 0) return
  const el = docPanelRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  if (docPanelLayout.x == null || docPanelLayout.y == null) {
    docPanelLayout.x = rect.left
    docPanelLayout.y = rect.top
  }
  const sx = e.clientX
  const sy = e.clientY
  const ox = docPanelLayout.x
  const oy = docPanelLayout.y
  const onMove = (ev) => {
    let nx = ox + ev.clientX - sx
    let ny = oy + ev.clientY - sy
    const vw = window.innerWidth
    const vh = window.innerHeight
    const w = Math.min(docPanelLayout.w, vw)
    nx = Math.min(Math.max(0, nx), Math.max(0, vw - w))
    ny = Math.min(Math.max(0, ny), Math.max(0, vh - 48))
    docPanelLayout.x = nx
    docPanelLayout.y = ny
  }
  const onUp = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    // 拖拽结束，清理活动记录
    activeDragCleanup = null
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  // 记录清理函数，供对话框关闭时主动移除残留监听器
  activeDragCleanup = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  e.preventDefault()
}

/** 缩放文档面板 */
function startDocPanelResize(e) {
  if (docFullscreen.value) return
  const el = docPanelRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  if (docPanelLayout.x == null || docPanelLayout.y == null) {
    docPanelLayout.x = rect.left
    docPanelLayout.y = rect.top
  }
  const sw = docPanelLayout.w
  const sh = docPanelLayout.h
  const sx = e.clientX
  const sy = e.clientY
  const onMove = (ev) => {
    docPanelLayout.w = Math.max(300, sw + ev.clientX - sx)
    docPanelLayout.h = Math.max(220, sh + ev.clientY - sy)
  }
  const onUp = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    // 缩放结束，清理活动记录
    activeDragCleanup = null
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  // 记录清理函数，供对话框关闭时主动移除残留监听器
  activeDragCleanup = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
}

/* ========== 参数操作方法 ========== */

function addParam() {
  config.params.push({ key: '', type: 'variable', value: 'card_key' })
}

function removeParam(index) {
  config.params.splice(index, 1)
}

function moveParam(index, direction) {
  const newIndex = index + direction
  if (newIndex >= 0 && newIndex < config.params.length) {
    const temp = config.params[index]
    config.params[index] = config.params[newIndex]
    config.params[newIndex] = temp
  }
}

function addResponseField() {
  config.response.push({ key: '', type: 'fixed', value: '' })
}

function removeResponseField(index) {
  config.response.splice(index, 1)
}

function moveResponseField(index, direction) {
  const newIndex = index + direction
  if (newIndex >= 0 && newIndex < config.response.length) {
    const temp = config.response[index]
    config.response[index] = config.response[newIndex]
    config.response[newIndex] = temp
  }
}

function restoreDefaultStatusCodes() {
  config.statusCodes = DEFAULT_STATUS_CODES.map(c => ({ ...c }))
}

/* ========== 预览计算 ========== */

const previewUrl = computed(() => {
  let url = config.url || ''
  if (!url) {
    url = generateDefaultUrl()
  }

  if (config.method === 'GET' && config.params.length > 0) {
    const hasApiKeyParam = config.params.some(p => p.type === 'variable' && p.value === 'api_key')
    if (hasApiKeyParam) {
      url = url.replace(/\/api\/custom\/[^/]+\/use/, '/api/custom/use')
    } else {
      if (url.endsWith('/api/custom/use')) {
        const key = props.apiKey?.key || '{api_key}'
        url = url.replace('/api/custom/use', `/api/custom/${key}/use`)
      }
    }

    const queryParts = config.params.map(p => {
      let val = p.value
      if (p.type === 'variable') {
        if (p.value === 'api_key') val = props.apiKey?.key || 'YOUR_API_KEY'
        else if (p.value === 'card_key') val = '{card_key}'
        else if (p.value === 'machine_code') val = '{machine_code}'
        else if (p.value === 'client_ip') val = '127.0.0.1'
        else if (p.value === 'time') val = Math.floor(Date.now() / 1000)
        else val = `{${p.value}}`
      }
      const key = p.key || 'key'
      return `${key}=${val}`
    })
    const queryString = queryParts.join('&')
    if (queryString) {
      return url.includes('?') ? `${url}&${queryString}` : `${url}?${queryString}`
    }
    return url
  }

  return url
})

const previewResponseJson = computed(() => {
  if (!config.response || config.response.length === 0) {
    return JSON.stringify({ success: true, message: 'Card used successfully' }, null, 2)
  }

  const entries = config.response
    .filter(p => p.key)
    .map(p => {
      let val = p.value
      if (p.type === 'variable') {
        if (p.value === 'remaining_time') val = '30天0小时0分钟'
        else if (p.value === 'remaining_count') val = '5次'
        else if (p.value === 'card_key') val = 'ABC123XYZ'
        else if (p.value === 'expire_time') val = '2026-01-01 12:00:00'
        else if (p.value === 'card_type') val = '时间卡'
        else if (p.value === 'card_status') val = 'no'
        else if (p.value === 'machine_code') val = 'MC-ABCDEF123456'
        else if (p.value === 'success') val = true
        else if (p.value === 'message') val = '验证成功'
        else if (p.value === 'status_code') {
          const successCode = config.statusCodes && config.statusCodes.find(c => c.key === 'success')
          val = successCode ? successCode.value : '200'
        }
        else val = `{${p.value}}`
      } else {
        if (val === 'true') val = true
        if (val === 'false') val = false
      }
      return `  "${p.key}": ${JSON.stringify(val)}`
    })

  return `{\n${entries.join(',\n')}\n}`
})
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

.modal-header-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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

.btn-doc {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.45rem 0.85rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.btn-doc:hover {
  background: #dbeafe;
  border-color: #93c5fd;
}

.interface-settings {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1rem 0;
}

.form-group {
  margin-bottom: 0;
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

/* 自定义URL开关卡片 */
.webhook-url-toggle-card {
  margin-bottom: 0.75rem;
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

/* 请求方式选择器 */
.method-selector-group {
  display: flex;
  gap: 15px;
}

.method-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
  justify-content: center;
  background: white;
}

.method-option:hover {
  border-color: #90caf9;
  background: #f5faff;
}

.method-option.active {
  border-color: #2196F3;
  background: #e3f2fd;
  color: #1976d2;
}

.radio-circle {
  width: 16px;
  height: 16px;
  border: 2px solid #ccc;
  border-radius: 50%;
  position: relative;
  transition: all 0.3s ease;
}

.method-option.active .radio-circle {
  border-color: #2196F3;
}

.method-option.active .radio-circle:after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 8px;
  height: 8px;
  background: #2196F3;
  border-radius: 50%;
}

.method-name {
  font-weight: bold;
  font-size: 0.95rem;
}

/* 参数配置区域 */
.params-config {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 1rem;
  background: #f9fafb;
}

.params-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.params-header label {
  font-weight: bold;
  color: #374151;
}

.params-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.param-row {
  display: grid;
  grid-template-columns: 2fr 1.5fr 3fr 1.5fr;
  gap: 0.5rem;
  align-items: center;
}

.param-row.header {
  font-weight: bold;
  font-size: 0.85rem;
  color: #6b7280;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 0.5rem;
}

.param-row input,
.param-row select {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.9rem;
  width: 100%;
  box-sizing: border-box;
}

.row-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.btn-icon {
  background: none;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
}

.btn-icon:hover:not(:disabled) {
  background: #f3f4f6;
  color: #374151;
}

.btn-icon:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-params {
  text-align: center;
  padding: 2rem;
  color: #9ca3af;
  font-style: italic;
  border: 1px dashed #d1d5db;
  border-radius: 4px;
}

/* 预览区域 */
.url-preview-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.url-preview-section label {
  display: block;
  font-weight: bold;
  margin-bottom: 0.5rem;
  color: #333;
}

.preview-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.preview-url {
  font-family: monospace;
  color: #475569;
  font-size: 0.9rem;
  word-break: break-all;
  white-space: pre-wrap;
}

/* 按钮 */
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

.btn-primary.small {
  padding: 0.4rem 0.75rem;
  font-size: 0.75rem;
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

.btn-secondary.small {
  padding: 0.4rem 0.75rem;
  font-size: 0.75rem;
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

.copy-btn.small {
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding-top: 1rem;
}

/* 浮动文档面板 */
.interface-doc-panel {
  display: flex;
  flex-direction: column;
  position: relative;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  border: 1px solid #e5e7eb;
  overflow: hidden;
  box-sizing: border-box;
}

.interface-doc-panel--fullscreen {
  border-radius: 0;
  border: none;
}

.interface-doc-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.75rem;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  cursor: move;
  user-select: none;
}

.interface-doc-panel--fullscreen .interface-doc-header {
  cursor: default;
}

.interface-doc-title {
  font-weight: 600;
  font-size: 0.95rem;
  color: #1e293b;
}

.interface-doc-toolbar {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.interface-doc-tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.interface-doc-tool-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.interface-doc-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 1rem 1.1rem 1.25rem;
  font-size: 0.875rem;
  line-height: 1.55;
  color: #334155;
}

.interface-doc-callout {
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  color: #1e40af;
}

.interface-doc-section {
  margin-bottom: 1.1rem;
}

.interface-doc-section h4 {
  margin: 0 0 0.5rem;
  font-size: 0.95rem;
  color: #0f172a;
}

.interface-doc-section p {
  margin: 0 0 0.5rem;
}

.interface-doc-section ul {
  margin: 0;
  padding-left: 1.25rem;
}

.interface-doc-section li {
  margin-bottom: 0.35rem;
}

.interface-doc-section code {
  font-size: 0.8rem;
  padding: 0.1rem 0.35rem;
  background: #f1f5f9;
  border-radius: 4px;
  color: #0f172a;
}

.interface-doc-muted {
  color: #64748b;
  font-size: 0.8rem;
}

.interface-doc-muted p {
  margin: 0;
}

.interface-doc-resize {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 18px;
  height: 18px;
  cursor: nwse-resize;
  background: linear-gradient(135deg, transparent 50%, #cbd5e1 50%, #cbd5e1 55%, transparent 55%, transparent 65%, #cbd5e1 65%, #cbd5e1 70%, transparent 70%);
  opacity: 0.85;
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
