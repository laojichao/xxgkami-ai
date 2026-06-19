/**
 * 卡密代码高亮工具函数
 * 为核销接口多语言代码示例提供语法高亮支持
 * 使用 highlight.js 按需加载各语言包，避免打包体积过大
 */
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import powershell from 'highlight.js/lib/languages/powershell'
import javascript from 'highlight.js/lib/languages/javascript'
import xml from 'highlight.js/lib/languages/xml'
import python from 'highlight.js/lib/languages/python'
import php from 'highlight.js/lib/languages/php'
import go from 'highlight.js/lib/languages/go'
import java from 'highlight.js/lib/languages/java'
import csharp from 'highlight.js/lib/languages/csharp'
import ruby from 'highlight.js/lib/languages/ruby'
import kotlin from 'highlight.js/lib/languages/kotlin'
import dart from 'highlight.js/lib/languages/dart'
import rust from 'highlight.js/lib/languages/rust'
import swift from 'highlight.js/lib/languages/swift'
// 引入 DOMPurify 对 highlight.js 输出进行净化，防止 XSS 攻击
import DOMPurify from 'dompurify'

// 注册各语言高亮支持（按项目实际需要的语言裁剪）
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('powershell', powershell)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('python', python)
hljs.registerLanguage('php', php)
hljs.registerLanguage('go', go)
hljs.registerLanguage('java', java)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('ruby', ruby)
hljs.registerLanguage('kotlin', kotlin)
hljs.registerLanguage('dart', dart)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('swift', swift)

/**
 * 示例ID到 highlight.js 语言标识的映射表
 * 与 apiUseCardCodeExamples.js 中各条目的 id 一一对应
 */
const EXAMPLE_ID_TO_LANG = {
  curl: 'bash',
  'curl-post-form': 'bash',
  wget: 'bash',
  httpie: 'bash',
  powershell: 'powershell',
  'node-fetch': 'javascript',
  'node-axios': 'javascript',
  'browser-js': 'javascript',
  html: 'xml',
  vue3: 'xml',
  'python-requests': 'python',
  'python-urllib': 'python',
  php: 'php',
  go: 'go',
  java: 'java',
  csharp: 'csharp',
  ruby: 'ruby',
  kotlin: 'kotlin',
  dart: 'dart',
  rust: 'rust',
  swift: 'swift'
}

/**
 * @param {string} code
 * @param {string} exampleId
 * @returns {string} 已转义并由 highlight.js 生成的 HTML
 */
/**
 * 对核销接口代码示例进行语法高亮
 * 先按示例ID查找对应语言，找不到则自动检测，最终回退为HTML转义
 * 所有 highlight.js 输出均经过 DOMPurify 净化，确保 v-html 渲染安全
 * @param {string} code - 待高亮的源代码字符串
 * @param {string} exampleId - 示例ID（对应 EXAMPLE_ID_TO_LANG 中的键）
 * @returns {string} 高亮且净化后的 HTML 字符串
 */
export function highlightUseCardExample(code, exampleId) {
  if (!code) return ''
  const lang = EXAMPLE_ID_TO_LANG[exampleId] || 'bash'
  let highlighted = ''
  try {
    highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
  } catch {
    try {
      highlighted = hljs.highlightAuto(code).value
    } catch {
      highlighted = escapeHtml(code)
    }
  }
  // 使用 DOMPurify 净化 highlight.js 输出，仅允许 span 标签及其 class 属性
  // 防止恶意代码通过 v-html 注入，确保 XSS 安全
  return DOMPurify.sanitize(highlighted, {
    ALLOWED_TAGS: ['span'],
    ALLOWED_ATTR: ['class']
  })
}

/** 将纯文本中的HTML特殊字符转义，作为高亮失败时的兜底方案 */
function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
