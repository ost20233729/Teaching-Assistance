<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api/admin'
import * as authApi from '@/api/auth'

type LoginRole = 'teacher' | 'admin'

const router = useRouter()

const isRegister = ref(false)
const loginRole = ref<LoginRole>('teacher')
const username = ref('')
const password = ref('')
const email = ref('')
const role = ref('teacher')
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const showPassword = ref(false)

const mousePosition = reactive({
  x: 0,
  y: 0
})

const cardStyle = reactive({
  boxShadow: ''
})

const handleMouseMove = (event: MouseEvent) => {
  mousePosition.x = event.clientX
  mousePosition.y = event.clientY

  const container = document.querySelector('.auth-container')
  if (!container) {
    return
  }

  const rect = container.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  const offsetX = (mousePosition.x - centerX) / rect.width
  const offsetY = (mousePosition.y - centerY) / rect.height
  const intensity = 20

  const glowColor = 'rgba(255, 255, 255, 0.6)'
  const shadowColor = 'rgba(31, 38, 135, 0.2)'
  const borderGlow = `rgba(255, 255, 255, ${0.4 + offsetY * 0.2})`

  cardStyle.boxShadow = `
    ${-offsetX * intensity}px ${-offsetY * intensity}px 20px ${glowColor},
    ${offsetX * intensity}px ${offsetY * intensity}px 25px ${shadowColor},
    0 4px 8px rgba(0, 0, 0, 0.05),
    inset 0 0 0 1px ${borderGlow}
  `
}

const clearLoginFlags = () => {
  localStorage.removeItem('isAdmin')
  sessionStorage.removeItem('isAdmin')
}

const resetMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const resetForm = () => {
  username.value = ''
  password.value = ''
  email.value = ''
}

onMounted(() => {
  if (sessionStorage.getItem('access_token') || localStorage.getItem('access_token')) {
    router.push('/')
  } else {
    clearLoginFlags()
  }

  window.addEventListener('mousemove', handleMouseMove)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})

const toggleMode = () => {
  isRegister.value = !isRegister.value
  loginRole.value = 'teacher'
  resetMessages()
  clearLoginFlags()
  resetForm()
}

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const switchLoginRole = (nextRole: LoginRole) => {
  loginRole.value = nextRole
  resetMessages()
  password.value = ''
}

const handleTeacherLoginSuccess = async () => {
  successMessage.value = '登录成功'
  localStorage.setItem('isAdmin', 'false')
  sessionStorage.setItem('isAdmin', 'false')

  localStorage.removeItem('showFunctionSelect')
  localStorage.removeItem('showCourseInfo')
  localStorage.removeItem('showCourseDescription')
  localStorage.removeItem('showCourseOutline')
  localStorage.removeItem('showTeachingLecture')
  localStorage.removeItem('showCourseware')
  localStorage.removeItem('selectedCourseTitle')
  localStorage.removeItem('selectedCourseId')
  localStorage.removeItem('selectedModuleId')

  sessionStorage.setItem('fromLogin', 'true')
  window.dispatchEvent(new Event('login-state-changed'))

  setTimeout(() => {
    router.push('/')
  }, 300)
}

const handleAdminLoginSuccess = () => {
  localStorage.setItem('isAdmin', 'true')
  sessionStorage.setItem('isAdmin', 'true')
  successMessage.value = '管理员登录成功'

  window.dispatchEvent(new Event('login-state-changed'))

  setTimeout(() => {
    router.push('/admin')
  }, 300)
}

const submit = async () => {
  resetMessages()

  if (isRegister.value) {
    if (!username.value.trim() || !email.value.trim() || !password.value.trim()) {
      errorMessage.value = '请完整填写注册信息'
      return
    }
  } else if (loginRole.value === 'teacher') {
    if (!username.value.trim() || !password.value.trim()) {
      errorMessage.value = '请输入教师用户名和密码'
      return
    }
  } else if (!username.value.trim() || !password.value.trim()) {
    errorMessage.value = '请输入管理员用户名和密码'
    return
  }

  isLoading.value = true

  try {
    if (isRegister.value) {
      const response = await authApi.register(
        username.value.trim(),
        password.value,
        email.value.trim(),
        role.value
      )

      if (response.success) {
        successMessage.value = '注册成功'
        resetForm()

        setTimeout(() => {
          isRegister.value = false
          loginRole.value = 'teacher'
          successMessage.value = ''
        }, 1500)
      } else {
        errorMessage.value = response.message || '注册失败，请重试'
      }

      return
    }

    if (loginRole.value === 'admin') {
      await adminApi.login({
        username: username.value.trim(),
        password: password.value
      })

      handleAdminLoginSuccess()
      return
    }

    const response = await authApi.login(username.value.trim(), password.value)
    if (response.success && response.data) {
      await handleTeacherLoginSuccess()
    } else {
      errorMessage.value = response.message || '登录失败，请检查用户名和密码'
    }
  } catch (error: any) {
    if (error?.detail && Array.isArray(error.detail) && error.detail.length > 0) {
      errorMessage.value = error.detail[0].msg || '参数错误'
    } else {
      errorMessage.value = error?.message || '操作失败'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card" :style="cardStyle">
      <div class="auth-header">
        <h2>{{ isRegister ? '创建账号' : '欢迎登录' }}</h2>
        <p class="auth-subtitle">
          {{ isRegister ? '请填写以下信息完成注册' : '请选择登录身份并输入对应账号信息' }}
        </p>
      </div>

      <form @submit.prevent="submit" class="auth-form">
        <div v-if="!isRegister" class="form-group">
          <label>登录身份</label>
          <div class="role-selector">
            <label class="role-option">
              <input
                type="radio"
                :checked="loginRole === 'teacher'"
                :disabled="isLoading"
                @change="switchLoginRole('teacher')"
              />
              <span class="role-name">教师</span>
            </label>
            <label class="role-option">
              <input
                type="radio"
                :checked="loginRole === 'admin'"
                :disabled="isLoading"
                @change="switchLoginRole('admin')"
              />
              <span class="role-name">管理员</span>
            </label>
          </div>
        </div>

        <div class="form-group">
          <label for="username">
            用户名
          </label>
          <div class="input-wrapper">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </span>
            <input
              id="username"
              v-model="username"
              type="text"
              required
              autocomplete="username"
              :disabled="isLoading"
              :placeholder="isRegister ? '请设置用户名' : `请输入${loginRole === 'admin' ? '管理员' : '教师'}用户名`"
              class="with-icon"
            />
          </div>
        </div>

        <div v-if="isRegister" class="form-group">
          <label for="email">邮箱</label>
          <div class="input-wrapper">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                <polyline points="22,6 12,13 2,6"></polyline>
              </svg>
            </span>
            <input
              id="email"
              v-model="email"
              type="email"
              :required="isRegister || loginRole === 'teacher'"
              autocomplete="email"
              :disabled="isLoading"
              placeholder="请输入邮箱地址"
              class="with-icon"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <div class="input-wrapper">
            <span class="input-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
            </span>
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              required
              autocomplete="current-password"
              :disabled="isLoading"
              placeholder="请输入密码"
              class="with-icon"
            />
            <button type="button" class="toggle-password" @click="togglePasswordVisibility">
              <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
            </button>
          </div>
        </div>

        <div v-if="isRegister" class="form-group">
          <label>注册身份</label>
          <div class="role-selector">
            <label class="role-option">
              <input
                type="radio"
                v-model="role"
                value="teacher"
                :disabled="isLoading"
              />
              <span class="role-name">教师</span>
            </label>
            <label class="role-option">
              <input
                type="radio"
                v-model="role"
                value="admin"
                :disabled="isLoading"
              />
              <span class="role-name">管理员</span>
            </label>
          </div>
        </div>

        <div v-if="!isRegister && loginRole === 'admin'" class="login-hint">
          管理员登录使用“用户名 + 密码”，不再依赖特殊邮箱触发。
        </div>

        <div v-if="!isRegister && loginRole === 'teacher'" class="login-hint">
          教师登录使用“用户名 + 密码”，邮箱不参与登录，保留用于找回密码。
        </div>

        <button
          type="submit"
          :disabled="isLoading"
          class="submit-button"
          :class="{ loading: isLoading }"
        >
          <span v-if="!isLoading" class="button-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
              <polyline points="10 17 15 12 10 7"></polyline>
              <line x1="15" y1="12" x2="3" y2="12"></line>
            </svg>
          </span>
          {{ isLoading ? '处理中...' : (isRegister ? '注册' : '登录') }}
        </button>
      </form>

      <div class="auth-footer">
        <p v-if="isRegister">已有账号？<a href="#" @click.prevent="toggleMode">返回登录</a></p>
        <p v-else>没有账号？<a href="#" @click.prevent="toggleMode">立即注册</a></p>
      </div>

      <div v-if="errorMessage || successMessage" class="message-container">
        <p v-if="errorMessage" class="error-message">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="message-icon">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          {{ errorMessage }}
        </p>
        <p v-if="successMessage" class="success-message">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="message-icon">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
          {{ successMessage }}
        </p>
      </div>
    </div>
  </div>
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.auth-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  width: 100vw;
  margin: 0;
  padding: 0;
  position: fixed;
  inset: 0;
  z-index: 1;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 30px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: box-shadow 0.3s ease, transform 0.2s ease;
  position: relative;
  z-index: 1;
  background-clip: padding-box;
}

.auth-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.1), rgba(52, 152, 219, 0.3), rgba(255, 255, 255, 0.2));
  z-index: -1;
  pointer-events: none;
  transition: all 0.5s ease;
}

.auth-header,
.auth-form,
.auth-footer,
.message-container {
  position: relative;
  z-index: 2;
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--text-color);
  opacity: 0.7;
  margin-bottom: 0;
}

.auth-form {
  width: 100%;
  position: relative;
  z-index: 2;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.8);
  text-shadow: 0 1px 1px rgba(255, 255, 255, 0.3);
}

.input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(0, 0, 0, 0.7);
  opacity: 0.8;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

input {
  width: 100%;
  padding: 12px 12px 12px 40px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  font-size: 15px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: rgba(0, 0, 0, 0.8);
  transition: all 0.3s ease;
  box-shadow:
    0 4px 10px rgba(31, 38, 135, 0.1),
    inset 0 2px 4px rgba(255, 255, 255, 0.5);
  position: relative;
  z-index: 1;
  box-sizing: border-box;
}

input.with-icon {
  padding-left: 40px;
}

input:focus {
  outline: none;
  border-color: rgba(52, 152, 219, 0.8);
  box-shadow:
    0 4px 12px rgba(52, 152, 219, 0.2),
    inset 0 2px 4px rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.35);
}

.toggle-password {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-color);
  opacity: 0.8;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.toggle-password:hover {
  opacity: 1;
}

.toggle-password:focus {
  outline: none;
}

.role-selector {
  display: flex;
  gap: 20px;
  margin-top: 8px;
}

.role-option {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.role-option input[type="radio"] {
  width: auto;
  margin-right: 8px;
  padding: 0;
}

.role-name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.8);
}

.login-hint {
  margin-bottom: 18px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.12);
  color: rgba(30, 64, 175, 0.95);
  font-size: 13px;
  line-height: 1.5;
}

.submit-button {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(52, 152, 219, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow:
    0 4px 15px rgba(52, 152, 219, 0.3),
    inset 0 1px 2px rgba(255, 255, 255, 0.4);
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.submit-button:hover {
  background: rgba(52, 152, 219, 0.85);
  transform: translateY(-2px);
  box-shadow:
    0 6px 20px rgba(52, 152, 219, 0.4),
    inset 0 1px 3px rgba(255, 255, 255, 0.6);
}

.submit-button:active {
  transform: translateY(0);
  box-shadow:
    0 2px 10px rgba(52, 152, 219, 0.3),
    inset 0 1px 2px rgba(255, 255, 255, 0.4);
}

.button-icon {
  margin-right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-button.loading {
  opacity: 0.8;
  cursor: not-allowed;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
}

.auth-footer p {
  font-size: 14px;
  color: var(--text-color);
  opacity: 0.7;
}

.auth-footer a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.auth-footer a:hover {
  text-decoration: underline;
}

.message-container {
  margin-bottom: 20px;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
}

.error-message {
  background-color: var(--notification-error-bg);
  color: var(--notification-error-color);
  border: 1px solid rgba(244, 67, 54, 0.3);
}

.success-message {
  background-color: var(--notification-info-bg);
  color: var(--notification-info-color);
  border: 1px solid rgba(33, 150, 243, 0.3);
}

.message-icon {
  margin-right: 6px;
}

@media (max-width: 480px) {
  .auth-card {
    max-width: 90%;
    padding: 20px;
  }

  input {
    padding: 10px 10px 10px 36px;
    font-size: 14px;
  }

  .role-selector {
    gap: 12px;
  }
}
</style>
