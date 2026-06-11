<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import HomePage from './components/HomePage.vue'
import OnlineUnbindPage from './components/OnlineUnbindPage.vue'
import LoginForm from './components/loginform.vue'
import Dashboard from './components/Dashboard.vue'
import UserPage from './components/UserPage.vue'
import NotificationPage from './components/NotificationPage.vue'
import { authApi, maintenanceApi, userProfileApi } from './services/api.js'
import { ElMessage } from 'element-plus'
import logger from './utils/logger'

// 响应式数据
const currentPage = ref('home') // 默认为首页
const loginType = ref('user')
const isLoggedIn = ref(false)
const userInfo = ref(null)
const loading = ref(true)
const maintenanceData = ref({ enabled: false, content: '', maintenanceTime: '' })

// 检查登录状态
// <p>不再依赖 localStorage 中的 token，改为通过后端接口验证 Cookie 中的登录状态。</p>
const checkLoginStatus = async () => {
  try {
    const storedUserInfo = localStorage.getItem('userInfo')
    const storedIsLoggedIn = localStorage.getItem('isLoggedIn')

    // 1. 如果 localStorage 中有登录标记，尝试通过后端验证 Cookie 是否仍然有效
    if (storedUserInfo && storedIsLoggedIn === 'true') {
      let parsedUserInfo;
      try {
        parsedUserInfo = JSON.parse(storedUserInfo);
      } catch (parseError) {
        logger.error('localStorage userInfo 数据损坏，清除登录状态:', parseError);
        clearLocalAuthState();
        return;
      }

      // 通过后端接口验证 Cookie 中的 Token 是否有效
      try {
        const res = await authApi.getUserInfo();
        if (res.success && res.data) {
          // Cookie 有效，使用后端返回的最新用户信息
          isLoggedIn.value = true;
          userInfo.value = res.data;
          const minimalInfo = {
            id: res.data.id,
            username: res.data.username,
            role: res.data.role,
            nickname: res.data.nickname
          }
          localStorage.setItem('userInfo', JSON.stringify(minimalInfo));

          if (res.data.role === 'admin') {
            currentPage.value = 'dashboard';
          } else {
            currentPage.value = 'user';
          }
        } else {
          // Cookie 已失效，清除本地状态
          clearLocalAuthState();
        }
      } catch (e) {
        // 请求失败（如 401），Cookie 已失效
        logger.error('验证登录状态失败:', e);
        clearLocalAuthState();
      }
    } else {
      // 2. 如果未登录，再检查是否是管理员登录路径
      const path = window.location.pathname
      const hash = window.location.hash
      if (path.includes('/admin') || hash.includes('admin')) {
        loginType.value = 'admin'
        currentPage.value = 'login'
      } else {
        // 默认状态
        clearLocalAuthState();
      }
    }
  } catch (error) {
    logger.error('检查登录状态失败:', error)
    isLoggedIn.value = false
    userInfo.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 清除前端本地认证状态（不涉及 Token，Token 由 httpOnly Cookie 管理）
 */
const clearLocalAuthState = () => {
  isLoggedIn.value = false;
  userInfo.value = null;
  localStorage.removeItem('userInfo');
  localStorage.removeItem('isLoggedIn');
}

// 显示登录页面
const showLogin = () => {
  currentPage.value = 'login'
  loginType.value = 'user'
}

// 处理登录成功
const handleLoginSuccess = (data) => {
  isLoggedIn.value = true
  userInfo.value = data.userInfo
  // 持久化登录状态到 localStorage
  localStorage.setItem('isLoggedIn', 'true')
  const minimalInfo = {
    id: data.userInfo.id,
    username: data.userInfo.username,
    role: data.userInfo.role,
    nickname: data.userInfo.nickname
  }
  localStorage.setItem('userInfo', JSON.stringify(minimalInfo))
  // 根据用户角色跳转到对应页面
  if (data.userInfo.role === 'admin') {
    currentPage.value = 'dashboard'
  } else {
    currentPage.value = 'user'
  }
}

// 处理登出
const handleLogout = async () => {
  try {
    if (userInfo.value) {
      // 尝试调用后端登出API
      await authApi.logout(userInfo.value.id, userInfo.value.role)
    }
  } catch (error) {
    logger.error('登出失败:', error)
  } finally {
    // 无论后端是否成功，前端都清除状态
    // Token 存储在 httpOnly Cookie 中，由后端 logout 接口清除
    isLoggedIn.value = false
    userInfo.value = null
    localStorage.removeItem('userInfo')
    localStorage.removeItem('isLoggedIn')

    currentPage.value = 'login'

    // 根据当前 URL 判断是否显示管理员登录
    const path = window.location.pathname
    const hash = window.location.hash
    if (path.includes('/admin') || hash.includes('admin')) {
      loginType.value = 'admin'
    } else {
      loginType.value = 'user'
    }
  }
}

// 检查维护状态
const checkMaintenance = async () => {
  try {
    const res = await maintenanceApi.getStatus()
    if (res.success && res.data) {
      maintenanceData.value = {
        ...res.data,
        enabled: Boolean(res.data.enabled)
      }
    }
  } catch (error) {
    logger.error('检查维护状态失败:', error)
  }
}

// 处理 OAuth 回调
const handleOAuthCallback = async () => {
  const isBinding = sessionStorage.getItem('binding_mode') === 'true';

  // Check both search (query) and hash for params
  let urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get('token');
  let refreshToken = urlParams.get('refreshToken');

  // If not found in search, try hash (e.g. #/oauth/callback?token=...)
  if (!token && window.location.hash.includes('?')) {
      const hashQuery = window.location.hash.split('?')[1];
      if (hashQuery) {
          const hashParams = new URLSearchParams(hashQuery);
          token = hashParams.get('token');
          refreshToken = hashParams.get('refreshToken');
      }
  }

  // If still not found, check if it's in the path (some routers might do this)

  if (token && refreshToken) {
    if (isBinding) {
        ElMessage.warning('该社交账号已被其他用户绑定！');
        sessionStorage.removeItem('binding_mode');
        window.history.replaceState({}, document.title, window.location.pathname);
        return true;
    }

    try {
      // OAuth 回调：Token 通过 URL 参数传递，需要通过后端接口设置 httpOnly Cookie
      // 携带 state 参数防止 session fixation 攻击
      const oauthState = sessionStorage.getItem('oauth_state');
      sessionStorage.removeItem('oauth_state');
      const setCookieRes = await fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}/auth/oauth/set-cookies`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ token, refreshToken, state: oauthState })
      });

      // 如果后端没有实现 OAuth set-cookies 接口，回退到直接使用 getUserInfo
      // （后端 OAuth 回调可能已直接设置了 Cookie）
      // 获取用户信息（通过 Cookie 认证）
      const res = await authApi.getUserInfo()
      if (res.success) {
        userInfo.value = res.data
        isLoggedIn.value = true
        localStorage.setItem('isLoggedIn', 'true')
        const minimalInfo = {
          id: res.data.id,
          username: res.data.username,
          role: res.data.role,
          nickname: res.data.nickname
        }
        localStorage.setItem('userInfo', JSON.stringify(minimalInfo))
        // Clean URL
        window.history.replaceState({}, document.title, window.location.pathname);
        currentPage.value = 'user'
      } else {
          logger.error('OAuth login failed:', res);
          handleLogout();
      }
    } catch (e) {
      logger.error('OAuth callback error:', e)
      handleLogout()
    }
  } else if (window.location.pathname.includes('/oauth/callback') || window.location.hash.includes('/oauth/callback')) {
      // Check for needRegister param
      const hashQueryString = window.location.hash.includes('?') ? window.location.hash.split('?')[1] : '';
      const needRegister = urlParams.get('needRegister') || (hashQueryString ? new URLSearchParams(hashQueryString).get('needRegister') : null);
      if (needRegister === 'true') {
           const hashQs = window.location.hash.includes('?') ? window.location.hash.split('?')[1] : '';
           const registerToken = urlParams.get('registerToken') || (hashQs ? new URLSearchParams(hashQs).get('registerToken') : null);
           const nickname = urlParams.get('nickname') || (hashQs ? new URLSearchParams(hashQs).get('nickname') : null);

           if (isBinding && registerToken) {
               try {
                   const res = await userProfileApi.bindSocial(registerToken);
                   if (res.success) {
                       ElMessage.success('绑定成功！');
                   } else {
                       ElMessage.error(res.message || '绑定失败');
                   }
               } catch (e) {
                   ElMessage.error('绑定失败: ' + e.message);
               } finally {
                   sessionStorage.removeItem('binding_mode');
                   window.history.replaceState({}, document.title, window.location.pathname);
                   currentPage.value = 'user';
                   // Reload to ensure UserPage refreshes
                   window.location.reload();
               }
               return true;
           }

           // We are in register mode
           if (registerToken) {
               // Store temp token
               sessionStorage.setItem('oauth_register_token', registerToken);
               sessionStorage.setItem('oauth_nickname', nickname || '');
               // Redirect to login page with register mode
               currentPage.value = 'login';
               // Pass a flag to login component to show register-bind form?
               // Or we can just use a query param 'mode=oauth_register'
               window.location.hash = '#/login?mode=oauth_register';
               // Actually, since we use currentPage='login', we can pass props or state.
               // But LoginForm is a component.
               // We need to tell LoginForm to show OAuth Register.
               // Let's use a global event or store, or simply url param.
               // App.vue manages currentPage.
               return true;
           }
      } else {
          const hashQs2 = window.location.hash.includes('?') ? window.location.hash.split('?')[1] : '';
          const error = urlParams.get('error') || (hashQs2 ? new URLSearchParams(hashQs2).get('error') : null);
          if (error) {
              logger.error('OAuth Error from provider:', error);
              const errorMessages = {
                'access_denied': '用户取消授权',
                'invalid_token': '无效的授权令牌',
                'token_expired': '授权已过期',
                'server_error': '服务器错误'
              };
              ElMessage.error('登录失败: ' + (errorMessages[error] || '未知错误'));
              currentPage.value = 'login';
              return true;
          }
      }
  }
  return false;
}

// 组件挂载时检查登录状态
let maintenanceInterval = null

onMounted(async () => {
  await checkMaintenance()

  // 先检查 OAuth 回调
  const oauthSuccess = await handleOAuthCallback()
  if (!oauthSuccess) {
    await checkLoginStatus()
  } else {
    loading.value = false
  }

  // 每60秒检查一次维护状态（仅在未登录或非管理员时需要）
  maintenanceInterval = setInterval(() => {
    if (!isLoggedIn.value || userInfo.value?.role !== 'admin') {
      checkMaintenance()
    }
  }, 60000)
})

onUnmounted(() => {
  if (maintenanceInterval) {
    clearInterval(maintenanceInterval)
    maintenanceInterval = null
  }
})
</script>

<template>
  <div id="app">
    <!-- 系统维护遮罩层 -->
    <div v-if="maintenanceData && maintenanceData.enabled && (!isLoggedIn || userInfo?.role !== 'admin') && currentPage !== 'login' && currentPage !== 'dashboard' && currentPage !== 'online-unbind'" class="maintenance-overlay">
      <div class="maintenance-content">
        <div class="maintenance-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
        </div>
        <h1>系统维护中</h1>
        <div class="maintenance-message">
          <p>{{ maintenanceData.content || '系统正在进行升级维护，请稍后访问。' }}</p>
          <p v-if="maintenanceData.maintenanceTime" class="maintenance-time">
            预计维护时间：{{ maintenanceData.maintenanceTime }}
          </p>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载...</p>
    </div>

    <!-- 首页 -->
    <HomePage
      v-else-if="currentPage === 'home'"
      @show-login="showLogin"
      @go-online-unbind="currentPage = 'online-unbind'"
    />

    <OnlineUnbindPage
      v-else-if="currentPage === 'online-unbind'"
      @back-home="currentPage = 'home'"
      @show-login="showLogin"
    />

    <!-- 登录界面 -->
    <LoginForm
      v-else-if="currentPage === 'login'"
      :initial-user-type="loginType"
      @login-success="handleLoginSuccess"
      @switch-to-user="loginType = 'user'"
    />

    <!-- 管理员界面 -->
    <Dashboard
      v-else-if="currentPage === 'dashboard'"
      :user-info="userInfo"
      @logout="handleLogout"
    />

    <!-- 普通用户界面 -->
    <UserPage
      v-else-if="currentPage === 'user'"
      :user-info="userInfo"
      @logout="handleLogout"
    />
  </div>
</template>

<style scoped>
#app {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top: 4px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-container p {
  font-size: 1.2rem;
  margin: 0;
}

.main-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.app-header {
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h1 {
  color: #333;
  margin: 0;
  font-size: 1.5rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.welcome-text {
  color: #666;
  font-size: 0.9rem;
}

.user-type-badge {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  margin-left: 0.5rem;
}

.user-type-badge.admin {
  background: #e3f2fd;
  color: #1976d2;
}

.user-type-badge.user {
  background: #f3e5f5;
  color: #7b1fa2;
}

.logout-button {
  padding: 0.5rem 1rem;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.3s;
}

.logout-button:hover {
  background: #d32f2f;
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.dashboard {
  display: grid;
  gap: 2rem;
}

.welcome-card {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.welcome-card h2 {
  color: #333;
  margin-bottom: 0.5rem;
}

.welcome-card > p {
  color: #666;
  margin-bottom: 2rem;
}

.user-details {
  margin-bottom: 2rem;
}

.user-details h3 {
  color: #333;
  margin-bottom: 1rem;
  font-size: 1.1rem;
}

.info-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 4px;
}

.info-item label {
  font-weight: 500;
  color: #555;
}

.info-item span {
  color: #333;
}

.quick-actions h3 {
  color: #333;
  margin-bottom: 1rem;
  font-size: 1.1rem;
}

.action-buttons {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.action-button {
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: transform 0.2s, box-shadow 0.2s;
}

.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 1rem;
    flex-direction: column;
    gap: 1rem;
    text-align: center;
  }

  .app-main {
    padding: 1rem;
  }

  .welcome-card {
    padding: 1.5rem;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    grid-template-columns: 1fr;
  }
}

/* 维护页面样式 */
.maintenance-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(243, 244, 246, 0.98); /* 不透明背景防止透视 */
  backdrop-filter: blur(10px);
  z-index: 99999; /* 确保层级最高 */
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.maintenance-content {
  background: white;
  padding: 3rem;
  border-radius: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  max-width: 500px;
  width: 90%;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.maintenance-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 1.5rem;
  color: #f59e0b;
}

.maintenance-content h1 {
  font-size: 2rem;
  font-weight: 800;
  color: #111827;
  margin-bottom: 1.5rem;
}

.maintenance-message {
  color: #4b5563;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.maintenance-time {
  margin-top: 1rem;
  font-weight: 600;
  color: #4f46e5;
  background: #eef2ff;
  padding: 0.5rem;
  border-radius: 4px;
}

.maintenance-actions {
  border-top: 1px solid #e5e7eb;
  padding-top: 1.5rem;
}

.admin-login-btn {
  background: transparent;
  border: 1px solid #d1d5db;
  color: #6b7280;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.admin-login-btn:hover {
  border-color: #9ca3af;
  color: #374151;
}
</style>
