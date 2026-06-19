import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import logger from './utils/logger'

// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import registerIcons from './utils/icons'

const app = createApp(App)

// 全局错误处理：使用统一 logger 输出，生产环境静默
app.config.errorHandler = (err, instance, info) => {
  logger.error('[Global Error]', err, info)
}

// 使用Element Plus
app.use(ElementPlus)

// 按需注册项目中使用的图标
registerIcons(app)

app.mount('#app')

document.documentElement.lang = 'zh-CN'
