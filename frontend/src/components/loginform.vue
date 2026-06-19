<!-- 登录/注册/忘记密码/TOTP验证/OAuth第三方登录表单组件 -->
<template>
  <!-- OAuth注册模式：第三方登录后完善账号信息 -->
  <div v-if="isOAuthRegister" class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h3>完善账号信息</h3>
        <p>欢迎您，{{ oauthNickname }}！<br>请设置您的账号信息以完成注册。</p>
      </div>
      <form @submit.prevent="handleOAuthRegister" class="login-form">
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="oauthRegisterForm.username" required placeholder="设置登录用户名" :disabled="loading">
        </div>
        <div class="form-group">
          <label>设置密码</label>
          <input type="password" v-model="oauthRegisterForm.password" required placeholder="设置登录密码" :disabled="loading">
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input type="password" v-model="oauthRegisterForm.confirmPassword" required placeholder="再次输入密码" :disabled="loading">
        </div>
        <div class="form-group">
          <label>邮箱 (可选)</label>
          <input type="email" v-model="oauthRegisterForm.email" placeholder="用于找回密码" :disabled="loading">
        </div>
        <div class="form-actions">
          <button type="submit" class="login-button" :disabled="loading">
            {{ loading ? '注册中...' : '完成注册并登录' }}
          </button>
        </div>
        <div class="register-link">
          <button type="button" @click="cancelOAuthRegister" class="link-button">
            取消注册
          </button>
        </div>
      </form>
    </div>
  </div>

  <!-- 主登录卡片：区分管理员/用户主题样式 -->
  <div v-else class="login-container" :class="{ 'admin-theme': userType === 'admin' }">
    <div class="login-card" :class="{ 'admin-card': userType === 'admin' }">
      <!-- 管理员页顶部返回 -->
      <div v-if="userType === 'admin'" class="back-to-user" @click="switchToUser">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><polyline points="15 18 9 12 15 6"></polyline></svg>
        返回用户登录
      </div>

      <div class="login-header">
        <div class="brand-logo">
          <!-- 管理员：盾牌图标 -->
          <svg v-if="userType === 'admin'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" class="logo-icon admin-icon"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path><polyline points="9 12 11 14 15 10" stroke-width="2"></polyline></svg>
          <!-- 用户：原图标 -->
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="logo-icon user-icon"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"></path></svg>
        </div>
        <h2>{{ userType === 'admin' ? '管理员控制台' : '卡密管理系统' }}</h2>
        <p>{{ userType === 'admin' ? '仅限授权管理员登录' : '登录您的账号以继续' }}</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label :for="userType + '-username'">
            {{ userType === 'admin' ? '管理员账号' : '用户名/邮箱' }}
          </label>
          <input
            :id="userType + '-username'"
            type="text"
            v-model="loginForm.username"
            :placeholder="userType === 'admin' ? '请输入管理员账号' : '请输入用户名或邮箱'"
            required
            :disabled="loading"
          />
        </div>

        <div class="form-group">
          <label :for="userType + '-password'">密码</label>
          <div class="password-input">
            <input
              :id="userType + '-password'"
              :type="showPassword ? 'text' : 'password'"
              v-model="loginForm.password"
              placeholder="请输入密码"
              required
              :disabled="loading"
            />
            <button
              type="button"
              class="password-toggle"
              @click="showPassword = !showPassword"
              :disabled="loading"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            </button>
          </div>
          <div class="forgot-password-link">
            <a href="#" @click.prevent="showForgotPassword = true">忘记密码？</a>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="submit"
            class="login-button"
            :disabled="loading || !loginForm.username || !loginForm.password"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>

        <!-- 第三方OAuth登录区域（QQ/微信/支付宝） -->
        <!-- 第三方登录 -->
        <div v-if="oauthEnabled && userType === 'user'" class="oauth-section">
          <div class="divider">
            <span>或使用第三方账号登录</span>
          </div>
          <div class="oauth-buttons">
            <button v-if="oauthLoginTypes.qq" @click.prevent="handleOAuthLogin('qq')" class="oauth-btn qq" title="QQ登录" aria-label="QQ登录">
              <svg class="icon" viewBox="0 0 1024 1024" width="24" height="24"><path d="M824.8 613.2c-16-51.4-34.4-69.4-34.4-69.4 .6-10.8 .6-29.6 .6-43.9 0-106.3-75.9-196.9-163.7-227.1 -16-5.5-23.7-1.3-21 12.8 5 26.1-3.9 36.3-13 41s-16.5-12.8-23-41c-2.8-12-10.3-15.5-24.6-9.6 -22.4 9.2-46.6 15.6-72.3 15.6 -23.7 0-46-5.5-66.9-13.3 -14.7-5.5-22.1-1.3-24.8 11.1 -4.9 22.9-12.8 44.5-23.3 41.5 -11.1-3.1-17.7-16-12-42 2.7-12.3-3.6-17.4-18.2-12.8 -89.8 28.5-168.1 118.8-168.1 223.9 0 14.6 0 33.7 .6 44.7 0 0-19.1 17.9-36 71.7 -12.3 39.2-5.5 59.8 4.7 63 6.3 2 15.3-7.9 26.2-22.4 0 0 20.2 49.8 63 80.8 47.7 34.6 109.8 54 177.1 54 69.4 0 133.2-20.7 181.1-57.1 41.7-31.5 61.2-80.6 61.2-80.6 10.9 14.8 20.3 25 26.6 22.7 10.5-3.8 17.6-25 4.8-63.5z" fill="#38A1F3"></path></svg>
              <span>QQ</span>
            </button>
            <button v-if="oauthLoginTypes.wx" @click.prevent="handleOAuthLogin('wx')" class="oauth-btn wx" title="微信登录" aria-label="微信登录">
              <svg class="icon" viewBox="0 0 1024 1024" width="24" height="24"><path d="M667.6 657.3c15.2 0 28.7-2.3 40.2-6.5 13.5 24.3 54.3 84.8 54.3 84.8 -10.8-22.3-20.3-51.7-20.3-51.7 54-32.4 89.2-81.1 89.2-136.5 0-97.3-109.5-175.7-244.6-175.7 -135.1 0-244.6 78.4-244.6 175.7 0 97.3 109.5 175.7 244.6 175.7l81.2-65.8zM474.3 585.6c-15.2 0-27-11.8-27-27s11.8-27 27-27 27 11.8 27 27 -11.8 27-27 27zM713.5 585.6c-15.2 0-27-11.8-27-27s11.8-27 27-27 27 11.8 27 27 -11.8 27-27 27z" fill="#09BB07"></path><path d="M439.2 469.3c-20.3 0-38.5 2.7-55.4 7.4 -103.4-66.2-159.5-117.6-159.5-171.6 0-112.2 131.8-202.7 294.6-202.7 162.8 0 294.6 90.5 294.6 202.7 0 54.1-30.4 102.7-81.1 140.5 4.1 14.9 6.8 30.4 6.8 46 0 16.2-2.7 32.4-7.4 48 3.4 0 6.1 .3 9.5 .3 189.2 0 343.3-109.5 343.3-244.6C884.5 160.1 730.4 50.7 541.2 50.7 352 50.7 197.9 160.1 197.9 295.3c0 74.3 46.6 141.2 121.6 185.8 13.5 31.1 27 60.8 40.5 91.9 -67.6-32.4-121.6-86.5-139.2-113.5 -55.4 33.8-91.9 86.5-91.9 146 0 55.4 31.8 104.7 81.8 139.2l-21.6 62.8c0 0 52-25.7 93.2-56.1 28.4 8.1 59.5 12.8 91.2 12.8 13.5 0 26.4-1 39.2-2.3 -48.3-25.4-83.8-62.2-102.7-106.1 -21.6 8.1-45.9 13.5-70.9 13.5zM383.8 368c-20.3 0-36.5-16.2-36.5-36.5s16.2-36.5 36.5-36.5 36.5 16.2 36.5 36.5 -16.2 36.5-36.5 36.5zM678.4 368c-20.3 0-36.5-16.2-36.5-36.5s16.2-36.5 36.5-36.5 36.5 16.2 36.5 36.5 -16.2 36.5-36.5 36.5z" fill="#09BB07"></path></svg>
              <span>微信</span>
            </button>
            <button v-if="oauthLoginTypes.alipay" @click.prevent="handleOAuthLogin('alipay')" class="oauth-btn alipay" title="支付宝登录" aria-label="支付宝登录">

              <svg class="icon" viewBox="0 0 1024 1024" width="24" height="24"><path d="M912 256H632V160c0-17.67-14.33-32-32-32H424c-17.67 0-32 14.33-32 32v96H112c-17.67 0-32 14.33-32 32v32c0 17.67 14.33 32 32 32h281.33c16.51 77.42 58.15 146.49 116.51 198.86-77.96 46.25-171.13 74.07-270.36 76.84-21.72 0.61-37.95 19.8-35.32 41.35 2.45 20.08 19.46 34.95 39.52 34.95h1.96c138.89-3.86 266.39-48.46 368.17-122.61 36.94 14.65 76.51 25.17 117.46 30.73-35.84 94.62-114.77 167.31-215.15 186.27-21.57 4.07-35.88 24.97-31.81 46.54 3.73 19.72 20.93 33.64 40.59 33.64 3.09 0 6.22-0.35 9.35-1.07 141.6-32.96 248.97-148.06 274.63-288.76 6.16-33.81 18.72-66.37 36.87-95.89l56.88 47.9c13.62 11.47 33.91 9.68 45.38-3.93 11.47-13.62 9.68-33.91-3.93-45.38l-72.36-60.94c-17.34-14.6-26.6-36.96-25.32-59.61 2.38-42.23 37.98-75.12 80.29-73.96 22.06 0.61 38.54-18.74 35.87-40.63-2.45-20.08-19.46-34.95-39.52-34.95H744v-96h168c17.67 0 32-14.33 32-32v-32c0-17.67-14.33-32-32-32z m-356.12 364.55c-43.25-39.75-76.49-90.13-94.75-148.55h197.87c-21.16 63.81-58.42 115.86-103.12 148.55z" fill="#1677FF"></path></svg>
              <span>支付宝</span>
            </button>


        </div>
      </div>

      <div v-if="userType === 'user'" class="register-link">
          <span>还没有账号？</span>
          <button type="button" @click="showRegister = true" class="link-button">
            立即注册
          </button>
        </div>
      </form>

      <div v-if="errorMessage" class="message error-message">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
        {{ errorMessage }}
      </div>

      <div v-if="successMessage" class="message success-message">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
        {{ successMessage }}
      </div>
    </div>

    <!-- 用户注册弹窗（用户名/昵称/密码/手机号/邮箱/验证码） -->
    <div v-if="showRegister" class="modal-overlay" @keydown.escape="showRegister = false">
      <div class="modal-content" role="dialog" aria-modal="true" aria-label="用户注册">
        <div class="modal-header">
          <h3>用户注册</h3>
          <button class="close-button" @click="showRegister = false" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleRegister" class="register-form">
            <div class="form-group">
              <label>登录名</label>
              <input type="text" v-model="registerForm.username" required placeholder="设置登录用户名">
            </div>
            <div class="form-group">
              <label>昵称</label>
              <input type="text" v-model="registerForm.nickname" required placeholder="设置显示昵称">
            </div>
            <div class="form-group">
              <label>密码</label>
              <input type="password" v-model="registerForm.password" required placeholder="设置登录密码">
            </div>
            <div class="form-group">
              <label>确认密码</label>
              <input type="password" v-model="registerForm.confirmPassword" required placeholder="再次输入密码">
            </div>
            <div class="form-group">
              <label>手机号</label>
              <input type="tel" v-model="registerForm.phone" required placeholder="输入手机号">
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input type="email" v-model="registerForm.email" required placeholder="输入邮箱地址">
            </div>
            <div class="form-group">
              <label>验证码</label>
              <div class="code-input-wrapper">
                <input type="text" v-model="registerForm.code" required placeholder="输入邮箱验证码">
                <button type="button" @click="sendCode" :disabled="codeTimer > 0 || !registerForm.email" class="code-btn">
                  {{ codeTimer > 0 ? `${codeTimer}s后重发` : '获取验证码' }}
                </button>
              </div>
            </div>
            
            <div v-if="registerError" class="message error-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              {{ registerError }}
            </div>
            <div v-if="registerSuccess" class="message success-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
              {{ registerSuccess }}
            </div>

            <button type="submit" class="login-button" :disabled="registerLoading">
              {{ registerLoading ? '注册中...' : '立即注册' }}
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- 找回密码弹窗（用户名/邮箱/验证码/新密码） -->
    <div v-if="showForgotPassword" class="modal-overlay" @keydown.escape="showForgotPassword = false">
      <div class="modal-content" role="dialog" aria-modal="true" aria-label="找回密码">
        <div class="modal-header">
          <h3>找回密码</h3>
          <button class="close-button" @click="showForgotPassword = false" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleResetPassword" class="register-form">
            <div class="form-group">
              <label>用户名</label>
              <input type="text" v-model="forgotPasswordForm.username" required placeholder="请输入您的用户名">
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input type="email" v-model="forgotPasswordForm.email" required placeholder="请输入注册时的邮箱">
            </div>
             <div class="form-group">
               <label>验证码</label>
               <div class="code-input-wrapper">
                 <input type="text" v-model="forgotPasswordForm.code" required placeholder="输入邮箱验证码">
                 <button type="button" @click="sendForgotCode" :disabled="forgotCodeTimer > 0 || !forgotPasswordForm.username || !forgotPasswordForm.email" class="code-btn">
                   {{ forgotCodeTimer > 0 ? `${forgotCodeTimer}s后重发` : '获取验证码' }}
                 </button>
               </div>
             </div>
            <div class="form-group">
              <label>新密码</label>
              <input type="password" v-model="forgotPasswordForm.password" required placeholder="设置新密码">
            </div>
            <div class="form-group">
              <label>确认新密码</label>
              <input type="password" v-model="forgotPasswordForm.confirmPassword" required placeholder="再次输入新密码">
            </div>
            
            <div v-if="forgotPasswordError" class="message error-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              {{ forgotPasswordError }}
            </div>
            <div v-if="forgotPasswordSuccess" class="message success-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
              {{ forgotPasswordSuccess }}
            </div>

            <button type="submit" class="login-button" :disabled="forgotPasswordLoading">
              {{ forgotPasswordLoading ? '重置中...' : '确认重置' }}
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- TOTP双重验证弹窗（6位验证码输入） -->
    <div v-if="showTotpInput" class="modal-overlay" @keydown.escape="showTotpInput = false">
      <div class="modal-content" style="max-width: 400px;" role="dialog" aria-modal="true" aria-label="双重验证">
        <div class="modal-header">
          <h3>双重验证 (2FA)</h3>
          <button class="close-button" @click="showTotpInput = false" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
        <div class="modal-body">
          <p class="totp-instruction" style="margin-bottom: 15px; color: #666;">请输入您的 Authenticator 应用生成的 6 位验证码</p>
          <form @submit.prevent="handleTotpLogin" class="totp-form">
            <div class="form-group">
              <label>验证码</label>
              <input 
                ref="totpInputRef"
                type="text" 
                v-model="loginForm.totpCode" 
                required 
                placeholder="000000" 
                maxlength="6"
                pattern="\d{6}"
                class="totp-input"
                autocomplete="one-time-code"
                style="text-align: center; letter-spacing: 5px; font-size: 1.2em;"
              >
            </div>
            
            <div v-if="errorMessage" class="message error-message">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
              {{ errorMessage }}
            </div>

            <button type="submit" class="login-button" :disabled="loading">
              {{ loading ? '验证中...' : '验证' }}
            </button>
            
            <div class="totp-recovery-link" style="text-align: center; margin-top: 15px;">
               <a href="#" @click.prevent="showRecoveryModal = true; showTotpInput = false" style="color: #666; font-size: 0.9em;">忘记验证码？</a>
            </div>
          </form>
        </div>
      </div>
    </div>
    
    <!-- TOTP恢复弹窗（通过邮箱验证码关闭双重验证） -->
    <!-- TOTP 恢复弹窗 -->
    <div v-if="showRecoveryModal" class="modal-overlay" @keydown.escape="showRecoveryModal = false">
      <div class="modal-content" style="max-width: 400px;" role="dialog" aria-modal="true" aria-label="重置双重验证">
        <div class="modal-header">
          <h3>重置双重验证</h3>
          <button class="close-button" @click="showRecoveryModal = false" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
        <div class="modal-body">
          <p style="margin-bottom: 15px; color: #666; font-size: 0.9em;">将发送验证码到管理员绑定的邮箱，验证通过后将自动关闭双重验证。</p>
          <form @submit.prevent="handleRecovery" class="totp-form">
             <div class="form-group">
               <label>邮箱验证码</label>
               <div class="code-input-wrapper">
                 <input type="text" v-model="recoveryForm.code" required placeholder="输入邮箱验证码">
                 <button type="button" @click="sendRecoveryCode" :disabled="recoveryTimer > 0" class="code-btn">
                   {{ recoveryTimer > 0 ? `${recoveryTimer}s后重发` : '获取验证码' }}
                 </button>
               </div>
             </div>
             
             <div v-if="recoveryError" class="message error-message">
               <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
               {{ recoveryError }}
             </div>
             <div v-if="recoverySuccess" class="message success-message">
               <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
               {{ recoverySuccess }}
             </div>

             <button type="submit" class="login-button" :disabled="recoveryLoading">
               {{ recoveryLoading ? '验证并关闭TOTP' : '确认' }}
             </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onUnmounted } from 'vue'
import { authApi, settingsApi } from '../services/api.js'
import { ElMessage } from 'element-plus'
import logger from '../utils/logger'

// === 常量配置 ===
/** 验证码倒计时秒数 */
const VERIFICATION_CODE_COUNTDOWN_SEC = 60
/** 模态框自动关闭延迟（毫秒） */
const MODAL_AUTO_CLOSE_DELAY_MS = 1500
/** 消息提示自动消失时间（毫秒） */
const AUTO_DISMISS_MESSAGE_DELAY_MS = 3000
/** 登录成功后重置表单延迟（毫秒） */
const LOGIN_SUCCESS_RESET_DELAY_MS = 1000
/** TOTP 输入框自动聚焦延迟（毫秒） */
const TOTP_FOCUS_DELAY_MS = 100

/** 父组件传入的初始用户类型（user/admin） */
const props = defineProps({
  initialUserType: {
    type: String,
    default: 'user'
  }
})

/** 当前登录类型（user/admin） */
const userType = ref(props.initialUserType)
/** 向父组件发送：登录成功、切换到用户登录事件 */
const emit = defineEmits(['login-success', 'switch-to-user'])

/** 从管理员登录切换回用户登录模式 */
const switchToUser = () => {
  userType.value = 'user'
  errorMessage.value = ''
  successMessage.value = ''
  loginForm.username = ''
  loginForm.password = ''
  loginForm.totpCode = ''
  emit('switch-to-user')
}

/* ========== 通用登录状态 ========== */

/** 是否显示密码明文 */
const showPassword = ref(false)
/** 登录按钮加载状态 */
const loading = ref(false)
/** 错误提示信息 */
const errorMessage = ref('')
/** 成功提示信息 */
const successMessage = ref('')
/** 注册弹窗是否可见 */
const showRegister = ref(false)
/** 找回密码弹窗是否可见 */
const showForgotPassword = ref(false)
/** TOTP验证弹窗是否可见 */
const showTotpInput = ref(false)
/** TOTP输入框DOM引用（用于自动聚焦） */
const totpInputRef = ref(null)

/* ========== TOTP恢复相关 ========== */

const showRecoveryModal = ref(false)
const recoveryLoading = ref(false)
const recoveryError = ref('')
const recoverySuccess = ref('')
const recoveryTimer = ref(0)
let recoveryTimerInterval = null
let pendingTimeouts = []

/** TOTP恢复表单（邮箱验证码） */
const recoveryForm = reactive({
  code: ''
})

/** 发送TOTP恢复验证码到管理员邮箱 */
const sendRecoveryCode = async () => {
  if (!loginForm.username) {
    recoveryError.value = '请先输入用户名'
    return
  }

  try {
    const response = await authApi.sendRecoveryCode(loginForm.username)
    if (response.success) {
      recoverySuccess.value = '验证码已发送，请查收邮件'
      recoveryError.value = ''
      startRecoveryTimer()
    } else {
      recoveryError.value = response.message || '发送失败'
    }
  } catch (error) {
    recoveryError.value = '发送失败: ' + (error.message || '未知错误')
  }
}

/** 启动恢复验证码倒计时（60秒） */
const startRecoveryTimer = (initialValue = VERIFICATION_CODE_COUNTDOWN_SEC) => {
  recoveryTimer.value = initialValue
  if (recoveryTimerInterval) clearInterval(recoveryTimerInterval)
  recoveryTimerInterval = setInterval(() => {
    recoveryTimer.value--
    if (recoveryTimer.value <= 0) {
      clearInterval(recoveryTimerInterval)
    }
  }, 1000)
}

/** 提交TOTP恢复验证码，验证通过后自动关闭双重验证并重新登录 */
const handleRecovery = async () => {
  if (!recoveryForm.code) {
    recoveryError.value = '请输入验证码'
    return
  }

  recoveryLoading.value = true
  recoveryError.value = ''
  recoverySuccess.value = ''

  try {
    const response = await authApi.disableTotpByRecovery(loginForm.username, recoveryForm.code)
    
    if (response.success) {
      recoverySuccess.value = '双重验证已关闭，请重新登录'
      setTimeout(() => {
        showRecoveryModal.value = false
        recoveryForm.code = ''
        // Auto login or just close modal?
        // Let's try to login again automatically without TOTP
        handleLogin()
      }, MODAL_AUTO_CLOSE_DELAY_MS)
    } else {
      recoveryError.value = response.message || '验证失败'
    }
  } catch (error) {
    recoveryError.value = '请求失败: ' + (error.message || '未知错误')
  } finally {
    recoveryLoading.value = false
  }
}

/* ========== OAuth第三方登录相关 ========== */

/** 是否处于OAuth注册模式（第三方登录后补充账号信息） */
const isOAuthRegister = ref(false)
/** OAuth用户的昵称（来自第三方平台） */
const oauthNickname = ref('')
/** OAuth注册表单数据 */
const oauthRegisterForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

/** 是否启用OAuth登录 */
const oauthEnabled = ref(false)
/** 各OAuth平台的启用状态 */
const oauthLoginTypes = reactive({
  qq: false,
  wx: false,
  alipay: false,
  sina: false,
  baidu: false
})

onMounted(async () => {
  // Check for OAuth Register Mode
  const hash = window.location.hash
  if (hash.includes('mode=oauth_register')) {
      const token = sessionStorage.getItem('oauth_register_token');
      if (token) {
          isOAuthRegister.value = true;
          oauthNickname.value = sessionStorage.getItem('oauth_nickname') || '第三方用户';
          // Pre-fill username if possible?
          // oauthRegisterForm.username = ...
      }
  }

  // Check settings for OAuth
  try {
     const res = await settingsApi.getAllSettings()
     if (res.success && res.data) {
        if (res.data.aggregatedLogin === 'true') {
           oauthEnabled.value = true
           if (res.data.oauth_login_types) {
               const types = res.data.oauth_login_types.split(',')
               types.forEach(t => {
                   if (t in oauthLoginTypes) oauthLoginTypes[t] = true
               })
           }
        }
     }
  } catch (e) {
     logger.error('Failed to load settings:', e)
  }
})

// 组件卸载时清理所有定时器和 watcher，防止内存泄漏
onUnmounted(() => {
  if (recoveryTimerInterval) clearInterval(recoveryTimerInterval)
  if (timerInterval) clearInterval(timerInterval)
  if (forgotTimerInterval) clearInterval(forgotTimerInterval)
  pendingTimeouts.forEach(id => clearTimeout(id))
  pendingTimeouts = []
  // 清理 autoDismiss 创建的 watcher 和 setTimeout
  autoDismissCleanups.forEach(cleanup => cleanup())
})

/** 跳转到第三方OAuth登录页面（先获取 state 防止 session fixation） */
const handleOAuthLogin = async (type) => {
    try {
        const apiBase = import.meta.env.VITE_API_BASE_URL || '/api';
        const cleanBase = apiBase.endsWith('/') ? apiBase.slice(0, -1) : apiBase;
        // 获取 OAuth state nonce
        const stateRes = await fetch(`${cleanBase}/auth/oauth/state`, { credentials: 'include' });
        const stateData = await stateRes.json();
        if (stateData.success && stateData.data?.state) {
            sessionStorage.setItem('oauth_state', stateData.data.state);
        }
        window.location.href = `${cleanBase}/oauth/login/${type}`;
    } catch (e) {
        ElMessage.error('初始化 OAuth 登录失败，请重试');
    }
}

/* ========== 用户注册相关 ========== */

/** 注册表单数据 */
const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  code: ''
})

const registerError = ref('')
const registerSuccess = ref('')
const registerLoading = ref(false)
/** 邮箱验证码倒计时秒数 */
const codeTimer = ref(0)
let timerInterval = null

/** 发送注册邮箱验证码 */
const sendCode = async () => {
  if (!registerForm.email) {
    registerError.value = '请先输入邮箱'
    return
  }
  
  try {
    const response = await authApi.sendEmailCode(registerForm.email, 'register')
    if (response.success) {
      registerSuccess.value = '验证码已发送，请查收邮件'
      registerError.value = ''
      localStorage.setItem('lastRegisterSendTime_' + registerForm.email, Date.now())
      startTimer()
    } else {
      registerError.value = response.message || '发送失败'
    }
  } catch (error) {
    registerError.value = '发送失败，请稍后重试'
  }
}

/** 启动注册验证码倒计时（60秒） */
const startTimer = (initialValue = VERIFICATION_CODE_COUNTDOWN_SEC) => {
  codeTimer.value = initialValue
  if (timerInterval) clearInterval(timerInterval)
  timerInterval = setInterval(() => {
    codeTimer.value--
    if (codeTimer.value <= 0) {
      clearInterval(timerInterval)
    }
  }, 1000)
}

watch(() => registerForm.email, (newEmail) => {
  if (timerInterval) {
    clearInterval(timerInterval)
    codeTimer.value = 0
  }
  if (newEmail) {
    const lastTime = localStorage.getItem('lastRegisterSendTime_' + newEmail)
    if (lastTime) {
      const diff = Math.floor((Date.now() - parseInt(lastTime)) / 1000)
      if (diff < 60) {
        startTimer(VERIFICATION_CODE_COUNTDOWN_SEC - diff)
      }
    }
  }
})

/** 提交用户注册请求 */
const handleRegister = async () => {
  if (registerForm.password !== registerForm.confirmPassword) {
    registerError.value = '两次输入的密码不一致'
    return
  }
  
  registerLoading.value = true
  registerError.value = ''
  registerSuccess.value = ''
  
  try {
    const response = await authApi.register({
      username: registerForm.username,
      nickname: registerForm.nickname,
      password: registerForm.password,
      phone: registerForm.phone,
      email: registerForm.email,
      code: registerForm.code
    })
    
    if (response.success) {
      registerSuccess.value = '注册成功！请登录'
      pendingTimeouts.push(setTimeout(() => {
        showRegister.value = false
        // Reset form
        Object.keys(registerForm).forEach(key => registerForm[key] = '')
      }, MODAL_AUTO_CLOSE_DELAY_MS))
    } else {
      registerError.value = response.message || '注册失败'
    }
  } catch (error) {
    registerError.value = '注册请求失败'
  } finally {
    registerLoading.value = false
  }
}

/* ========== 找回密码相关 ========== */

/** 找回密码表单数据 */
const forgotPasswordForm = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const forgotPasswordError = ref('')
const forgotPasswordSuccess = ref('')
const forgotPasswordLoading = ref(false)

/**
 * 自动清除消息提示（3秒后淡出）
 * 统一应用于所有错误/成功消息ref
 * 使用 Map 存储每个 messageRef 的清理函数，防止 timer 泄漏
 */
const autoDismissCleanups = new Map()
const autoDismiss = (messageRef) => {
  let timer = null
  const stopWatch = watch(messageRef, (newVal) => {
    if (newVal) {
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => {
        messageRef.value = ''
      }, AUTO_DISMISS_MESSAGE_DELAY_MS)
    }
  })
  autoDismissCleanups.set(messageRef, () => {
    if (timer) clearTimeout(timer)
    stopWatch()
  })
}

[errorMessage, successMessage, registerError, registerSuccess, forgotPasswordError, forgotPasswordSuccess, recoveryError, recoverySuccess].forEach(autoDismiss)

/** 找回密码验证码倒计时秒数 */
const forgotCodeTimer = ref(0)
let forgotTimerInterval = null

/** 发送找回密码邮箱验证码 */
const sendForgotCode = async () => {
  if (!forgotPasswordForm.username || !forgotPasswordForm.email) {
    forgotPasswordError.value = '请先输入用户名和邮箱'
    return
  }
  
  try {
    const response = await authApi.sendResetCode(forgotPasswordForm.email)
    if (response.success) {
      forgotPasswordSuccess.value = '验证码已发送，请查收邮件'
      forgotPasswordError.value = ''
      localStorage.setItem('lastResetSendTime_' + forgotPasswordForm.email, Date.now())
      startForgotTimer()
    } else {
      forgotPasswordError.value = response.message || '发送失败'
    }
  } catch (error) {
    forgotPasswordError.value = '发送失败: ' + error.message
  }
}

/** 启动找回密码验证码倒计时（60秒） */
const startForgotTimer = (initialValue = VERIFICATION_CODE_COUNTDOWN_SEC) => {
  forgotCodeTimer.value = initialValue
  if (forgotTimerInterval) clearInterval(forgotTimerInterval)
  forgotTimerInterval = setInterval(() => {
    forgotCodeTimer.value--
    if (forgotCodeTimer.value <= 0) {
      clearInterval(forgotTimerInterval)
    }
  }, 1000)
}

watch(() => forgotPasswordForm.email, (newEmail) => {
  if (forgotTimerInterval) {
    clearInterval(forgotTimerInterval)
    forgotCodeTimer.value = 0
  }
  if (newEmail) {
    const lastTime = localStorage.getItem('lastResetSendTime_' + newEmail)
    if (lastTime) {
      const diff = Math.floor((Date.now() - parseInt(lastTime)) / 1000)
      if (diff < 60) {
        startForgotTimer(VERIFICATION_CODE_COUNTDOWN_SEC - diff)
      }
    }
  }
})

/** 提交密码重置请求 */
const handleResetPassword = async () => {
  if (forgotPasswordForm.password !== forgotPasswordForm.confirmPassword) {
    forgotPasswordError.value = '两次输入的密码不一致'
    return
  }
  
  forgotPasswordLoading.value = true
  forgotPasswordError.value = ''
  forgotPasswordSuccess.value = ''
  
  try {
    const response = await authApi.resetPassword({
      email: forgotPasswordForm.email,
      code: forgotPasswordForm.code,
      newPassword: forgotPasswordForm.password
    })
    
    if (response.success) {
      forgotPasswordSuccess.value = '密码重置成功！请使用新密码登录'
      pendingTimeouts.push(setTimeout(() => {
        showForgotPassword.value = false
        // Reset form
        Object.keys(forgotPasswordForm).forEach(key => forgotPasswordForm[key] = '')
      }, MODAL_AUTO_CLOSE_DELAY_MS))
    } else {
      forgotPasswordError.value = response.message || '重置失败'
    }
  } catch (error) {
    forgotPasswordError.value = '请求失败: ' + error.message
  } finally {
    forgotPasswordLoading.value = false
  }
}

const loginForm = reactive({
  username: '',
  password: '',
  totpCode: ''
})

watch(userType, () => {
  if (!loginForm.username || !loginForm.password) {
    return
  }
  
  errorMessage.value = ''
  successMessage.value = ''
  loginForm.totpCode = ''
  showTotpInput.value = false
})

// emit already declared above

const handleTotpLogin = async () => {
    if (!loginForm.totpCode || loginForm.totpCode.length !== 6) {
        errorMessage.value = '请输入6位验证码'
        return
    }
    // Re-trigger login with TOTP code
    await handleLogin()
}

const handleOAuthRegister = async () => {
  errorMessage.value = '';
  
  if (!oauthRegisterForm.username || !oauthRegisterForm.password) {
    errorMessage.value = '请填写用户名和密码';
    return;
  }
  
  if (oauthRegisterForm.password !== oauthRegisterForm.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致';
    return;
  }
  
  loading.value = true;
  try {
     const registerToken = sessionStorage.getItem('oauth_register_token');
     const payload = {
         username: oauthRegisterForm.username,
         password: oauthRegisterForm.password,
         email: oauthRegisterForm.email,
         registerToken: registerToken
     };
     
     // We need a new API endpoint for this
     const res = await authApi.registerBind(payload);
     
     if (res.success) {
         // Login success - Token 已通过 httpOnly Cookie 设置，无需存储到 localStorage
         localStorage.setItem('isLoggedIn', 'true');
         // 注册成功后清理临时 OAuth 注册令牌
         sessionStorage.removeItem('oauth_register_token');
         sessionStorage.removeItem('oauth_nickname');

         // Get User Info
         const userRes = await authApi.getUserInfo();
         if (userRes.success) {
             // 仅保留非敏感信息到 localStorage，role 字段不存储，避免被篡改
             // 权限判断统一通过后端 /auth/user-info 接口实时获取
             const minimalInfo = {
               id: userRes.data.id,
               username: userRes.data.username,
               nickname: userRes.data.nickname
             }
             localStorage.setItem('userInfo', JSON.stringify(minimalInfo));
             window.location.reload(); // Reload to refresh App state
         }
     } else {
         errorMessage.value = res.message || '注册失败';
     }
  } catch (e) {
      errorMessage.value = e.message || '注册失败';
  } finally {
      loading.value = false;
  }
}

const cancelOAuthRegister = () => {
    isOAuthRegister.value = false;
    sessionStorage.removeItem('oauth_register_token');
    sessionStorage.removeItem('oauth_nickname');
    window.location.hash = '#/login';
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    errorMessage.value = '请填写完整的登录信息'
    return
  }

  loading.value = true
  if (!showTotpInput.value) {
      errorMessage.value = ''
  }
  successMessage.value = ''

  try {
    let response;
    if (userType.value === 'admin') {
      response = await authApi.loginAdmin(loginForm.username, loginForm.password, loginForm.totpCode);
    } else {
      response = await authApi.loginUser(loginForm.username, loginForm.password);
    }

    if (response.success) {
      // Close TOTP modal if open
      showTotpInput.value = false;
      
      const resultData = response.data;
      successMessage.value = response.message || '登录成功！'
      
      if (resultData && resultData.userInfo) {
        // Token 已通过 httpOnly Cookie 设置，仅存储用户基本信息和登录状态
        // role 字段不存储到 localStorage，避免被篡改，权限判断通过后端接口实时获取
        const minimalInfo = {
          id: resultData.userInfo.id,
          username: resultData.userInfo.username,
          nickname: resultData.userInfo.nickname
        }
        localStorage.setItem('userInfo', JSON.stringify(minimalInfo))
        localStorage.setItem('isLoggedIn', 'true')
      }

      emit('login-success', {
        userInfo: resultData.userInfo
      })

      // 将 setTimeout 纳入 pendingTimeouts 统一管理，防止组件卸载后内存泄漏
      pendingTimeouts.push(setTimeout(() => {
        resetForm()
      }, LOGIN_SUCCESS_RESET_DELAY_MS))
    } else {
      // Check for TOTP requirement
      if (response.message === 'TOTP_REQUIRED') {
          showTotpInput.value = true;
          // Clear password or keep it? Keep it.
          // Focus TOTP input
          // 将 setTimeout 纳入 pendingTimeouts 统一管理，防止组件卸载后内存泄漏
          pendingTimeouts.push(setTimeout(() => {
             if (totpInputRef.value) totpInputRef.value.focus();
          }, TOTP_FOCUS_DELAY_MS));
          loading.value = false;
          return;
      }
      errorMessage.value = response.message || '登录失败，请检查用户名和密码'
      // If TOTP failed, maybe clear it
      if (showTotpInput.value) {
          loginForm.totpCode = '';
      }
    }
  } catch (error) {
    logger.error('登录请求失败:', error)
    errorMessage.value = '登录验证失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  loginForm.username = ''
  loginForm.password = ''
  loginForm.totpCode = ''
  errorMessage.value = ''
  successMessage.value = ''
  showPassword.value = false
  showTotpInput.value = false
  showRecoveryModal.value = false
  recoveryForm.code = ''
}
</script>

<style scoped>

/* ======================== 用户登录主题 ======================== */
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  box-sizing: border-box;
}

.login-card {
  background: white;
  border: none;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 2.5rem;
  width: 100%;
  max-width: 420px;
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
}

.logo-icon {
  width: 48px;
  height: 48px;
}

.logo-icon.user-icon {
  color: #2563eb;
}

.logo-icon.admin-icon {
  width: 52px;
  height: 52px;
  color: #dc2626;
}

.login-header h2 {
  color: #111827;
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.025em;
}

.login-header p {
  color: #6b7280;
  margin: 0;
  font-size: 0.875rem;
}

/* ======================== 管理员登录主题 ======================== */
.login-container.admin-theme {
  background: linear-gradient(135deg, #1e1e2f 0%, #2d1b3d 50%, #1a1a2e 100%);
}

.login-card.admin-card {
  background: #ffffff;
  border-top: 4px solid #dc2626;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35), 0 4px 20px rgba(220, 38, 38, 0.1);
}

.admin-card .login-header h2 {
  color: #1e1e2f;
}

.admin-card .login-header p {
  color: #9ca3af;
  font-size: 0.8rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.admin-card .login-button {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
}

.admin-card .login-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #b91c1c, #991b1b);
}

.back-to-user {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.8rem;
  color: #6b7280;
  cursor: pointer;
  margin-bottom: 1.25rem;
  padding: 0.35rem 0;
  transition: color 0.2s;
  user-select: none;
}

.back-to-user:hover {
  color: #dc2626;
}



.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #374151;
  font-weight: 500;
  font-size: 0.875rem;
}

.form-group input {
  width: 100%;
  padding: 0.7rem 0.85rem;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.875rem;
  transition: all 0.2s;
  box-sizing: border-box;
  background: #f9fafb;
  color: #111827;
}

.form-group input:focus {
  outline: none;
  border-color: #2563eb;
  background: white;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.admin-card .form-group input:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.08);
}

.form-group input:disabled {
  background-color: #f3f4f6;
  cursor: not-allowed;
}

.password-input {
  position: relative;
  display: flex;
}

.password-toggle {
  position: absolute;
  right: 0;
  top: 0;
  height: 100%;
  padding: 0 0.75rem;
  background: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.password-toggle svg {
  width: 16px;
  height: 16px;
}

.password-toggle:hover {
  color: #374151;
}

.login-button {
  width: 100%;
  padding: 0.75rem;
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.login-button:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
}

.login-button:disabled {
  background: #9ca3af;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.oauth-section {
  margin-top: 2rem;
}

.divider {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
  color: #9ca3af;
  font-size: 0.875rem;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-top: 1px solid #e5e7eb;
}

.divider span {
  padding: 0 1rem;
}

.oauth-buttons {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.oauth-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: white;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 100px;
}

.oauth-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.oauth-btn .icon {
  width: 20px;
  height: 20px;
}

.oauth-btn.qq:hover { color: #38A1F3; border-color: #38A1F3; background: #f0f9ff; }
.oauth-btn.wx:hover { color: #09BB07; border-color: #09BB07; background: #f0fdf4; }
.oauth-btn.alipay:hover { color: #1677FF; border-color: #1677FF; background: #e6f7ff; }


.register-link {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}

.forgot-password-link {
  text-align: right;
  margin-top: 0.5rem;
}

.forgot-password-link a {
  color: #6b7280;
  font-size: 0.875rem;
  text-decoration: none;
}

.forgot-password-link a:hover {
  color: #374151;
  text-decoration: underline;
}

.link-button {
  background: none;
  border: none;
  color: #111827;
  cursor: pointer;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 600;
  margin-left: 0.25rem;
  padding: 0;
}

.link-button:hover {
  text-decoration: underline;
}

.message {
  padding: 0.75rem;
  border-radius: 4px;
  margin-top: 1.5rem;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.message svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.error-message {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.success-message {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 4px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.modal-header {
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  color: #111827;
  font-size: 1.1rem;
  font-weight: 600;
}

.close-button {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-button:hover {
  background: #f3f4f6;
  color: #111827;
}

.close-button svg {
  width: 20px;
  height: 20px;
}

.modal-body {
  padding: 1.5rem;
}

.modal-body p {
  margin: 0 0 0.5rem 0;
  color: #4b5563;
  line-height: 1.5;
  font-size: 0.9rem;
}

.modal-footer {
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
}

.button {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  background: white;
  color: #374151;
  transition: all 0.2s;
}

.button:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

@media (max-width: 480px) {
  .login-card {
    padding: 1.5rem;
  }
}

.code-input-wrapper {
  display: flex;
  gap: 0.5rem;
}

.code-btn {
  white-space: nowrap;
  padding: 0 1rem;
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  color: #374151;
  transition: all 0.2s;
}

.code-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.code-btn:disabled {
  color: #9ca3af;
  cursor: not-allowed;
  background: #f9fafb;
}

.register-form .form-group {
  margin-bottom: 1rem;
}
</style>