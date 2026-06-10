/**
 * Element Plus 图标按需导入
 * 只注册项目实际使用的图标，减少打包体积
 */
import {
  ArrowDown,
  Check,
  Clock,
  CreditCard,
  Document,
  Loading,
  Menu,
  Odometer,
  Search,
  Setting,
  ShoppingCart,
  SwitchButton,
  Tickets,
  Timer,
  User,
  Warning
} from '@element-plus/icons-vue'

const icons = [
  ArrowDown,
  Check,
  Clock,
  CreditCard,
  Document,
  Loading,
  Menu,
  Odometer,
  Search,
  Setting,
  ShoppingCart,
  SwitchButton,
  Tickets,
  Timer,
  User,
  Warning
]

export default function registerIcons(app) {
  icons.forEach(icon => {
    app.component(icon.name, icon)
  })
}
