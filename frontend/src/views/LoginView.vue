<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo -->
      <div class="login-header">
        <el-icon :size="40" color="#1677ff"><Document /></el-icon>
        <h1>AI 文档翻译平台</h1>
        <p class="login-desc">AI 驱动的多语言文档翻译与协作平台</p>
      </div>

      <!-- Tab 切换 -->
      <div class="tab-bar">
        <button
          :class="['tab', { active: mode === 'login' }]"
          @click="mode = 'login'"
        >
          邮箱登录
        </button>
        <button
          :class="['tab', { active: mode === 'register' }]"
          @click="mode = 'register'"
        >
          注册账号
        </button>
      </div>

      <!-- 表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
        @submit.prevent="handleSubmit"
      >
        <!-- 注册特有：用户名 -->
        <el-form-item v-if="mode === 'register'" label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="3-50个字符"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <!-- 注册特有：邮箱 -->
        <el-form-item v-if="mode === 'register'" label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="example@mail.com"
            :prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <!-- 登录特有：忘记密码 -->
        <p v-if="mode === 'login'" class="forgot-link">
          <router-link to="/forgot-password">忘记密码？</router-link>
        </p>

        <!-- 登录特有：用户名/邮箱 -->
        <el-form-item v-if="mode === 'login'" label="用户名 / 邮箱" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名或邮箱"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6-100个字符"
            :prefix-icon="Lock"
            size="large"
          />
        </el-form-item>

        <!-- 注册特有：验证码 -->
        <el-form-item v-if="mode === 'register'" label="邮箱验证码" prop="code">
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="6位验证码"
              maxlength="6"
            />
            <el-button
              :disabled="regCodeCountdown > 0"
              :loading="regSendingCode"
              @click="handleSendRegCode"
            >
              {{ regCodeCountdown > 0 ? regCodeCountdown + 's' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="submit-btn"
          @click="handleSubmit"
        >
          {{ mode === 'login' ? '登 录' : '注 册' }}
        </el-button>
      </el-form>

      <!-- 第三方登录 -->
      <div class="third-party">
        <div class="divider"><span>其他登录方式</span></div>
        <div class="third-buttons">
          <button class="github-btn" @click="handleGithubLogin">
            <svg class="github-icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61-.546-1.385-1.335-1.755-1.335-1.755-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.605-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 21.795 24 17.295 24 12c0-6.63-5.37-12-12-12z"/>
            </svg>
            GitHub 登录
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { login, register, sendRegisterCode } from '../api/auth'

const router = useRouter()

const mode = ref('login')
const loading = ref(false)
const formRef = ref(null)

// ===== 表单 =====
const form = reactive({ username: '', email: '', password: '', code: '' })
const regCodeCountdown = ref(0)
const regSendingCode = ref(false)

const rules = computed(() => {
  if (mode.value === 'register') {
    return {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 50, message: '用户名长度3-50', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 100, message: '密码长度6-100', trigger: 'blur' }
      ],
      code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
    }
  }
  return {
    username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
})

/**
 * 提交登录或注册表单，成功后保存 token 并跳转到主页。
 * @returns {Promise<void>}
 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = mode.value === 'login'
      ? await login({ username: form.username, password: form.password })
      : await register(form)
    saveAuth(res.data)
  } catch (e) {
    console.error('认证失败', e)
  } finally {
    loading.value = false
  }
}

watch(mode, () => {
  formRef.value?.clearValidate()
  form.code = ''
  regCodeCountdown.value = 0
})

/**
 * 发送注册验证码到邮箱，启动 60s 倒计时。
 * @returns {Promise<void>}
 */
async function handleSendRegCode() {
  const valid = await formRef.value?.validateField('email').catch(() => false)
  if (!valid) return
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  regSendingCode.value = true
  try {
    await sendRegisterCode(form.email)
    ElMessage.success('验证码已发送到您的邮箱')
    regCodeCountdown.value = 60
    const timer = setInterval(() => {
      regCodeCountdown.value--
      if (regCodeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    console.error('发送验证码失败', e)
  } finally {
    regSendingCode.value = false
  }
}

/** 跳转到后端 GitHub OAuth2 授权页面 */
function handleGithubLogin() {
  window.location.href = 'http://localhost:8080/oauth2/authorization/github'
}

/**
 * 将认证响应中的 token 存入 localStorage 并跳转到主页。
 * @param {Object} data - 认证响应数据
 * @param {string} data.accessToken - JWT 访问令牌
 * @param {string} [data.refreshToken] - 刷新令牌
 */
function saveAuth(data) {
  localStorage.setItem('accessToken', data.accessToken)
  if (data.refreshToken) {
    localStorage.setItem('refreshToken', data.refreshToken)
  }
  ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
  router.replace('/')
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f5ff 0%, #f5f7fb 50%, #e6f4ff 100%);
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 40px 32px;
  box-shadow: 0 8px 40px rgba(0,0,0,.08);
}

/* ===== Header ===== */
.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 10px 0 4px;
}

.login-desc {
  font-size: 13px;
  color: #94a3b8;
}

/* ===== Tabs ===== */
.tab-bar {
  display: flex;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 24px;
}

.tab {
  flex: 1;
  padding: 10px 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all .2s;
}

.tab.active {
  background: #fff;
  color: #1677ff;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);
}

.tab:hover:not(.active) {
  color: #334155;
}

/* ===== Form ===== */
.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.forgot-link {
  text-align: right;
  margin-bottom: 16px;
  font-size: 13px;
}
.forgot-link a {
  color: #1677ff;
  text-decoration: none;
}
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  margin-top: 2px;
  font-weight: 600;
}

/* ===== Third-party ===== */
.third-party {
  margin-top: 24px;
}

.divider {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  color: #94a3b8;
  font-size: 13px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}

.divider span {
  padding: 0 14px;
  flex-shrink: 0;
}

.third-buttons {
  display: flex;
  gap: 12px;
}

.github-btn {
  flex: 1;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
  transition: all .2s;
}

.github-btn:hover {
  background: #24292e;
  color: #fff;
  border-color: #24292e;
}

.github-icon {
  width: 20px;
  height: 20px;
}
.code-row {
  display: flex;
  gap: 8px;
}
.code-row :deep(.el-input) {
  flex: 1;
}
.code-row :deep(.el-button) {
  flex-shrink: 0;
  white-space: nowrap;
}
</style>
