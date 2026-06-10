import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import registerIcons from './utils/icons'

const app = createApp(App)

// 使用Element Plus
app.use(ElementPlus)

// 按需注册项目中使用的图标
registerIcons(app)

app.mount('#app')
