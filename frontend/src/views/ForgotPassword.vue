<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <div class="forgot-header">
        <el-icon :size="36" color="#1677ff"><Lock /></el-icon>
        <h1>重置密码</h1>
        <p class="forgot-desc">{{ step === 1 ? '请输入注册邮箱获取验证码' : '请设置新密码' }}</p>
      </div>

      <!-- Step 1: 邮箱验证 -->
      <template v-if="step === 1">
        <el-form ref="emailFormRef" :model="form" :rules="emailRules" label-position="top">
          <el-form-item label="注册邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入注册邮箱" :prefix-icon="Message" size="large" />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model="form.code" placeholder="6位验证码" maxlength="6" />
              <el-button :disabled="codeCountdown > 0" :loading="sending" @click="handleSendCode">
                {{ codeCountdown > 0 ? codeCountdown + 's' : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </template>

      <!-- Step 2: 设置新密码 -->
      <template v-else>
        <el-form ref="pwdFormRef" :model="form" :rules="pwdRules" label-position="top">
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="6-100个字符" :prefix-icon="Lock" size="large" />
          </el-form-item>
        </el-form>
      </template>

      <template v-if="step === 1">
        <el-button type="primary" size="large" :loading="verifying" class="submit-btn" @click="handleNext">
          下一步
        </el-button>
      </template>
      <template v-else>
        <el-button type="primary" size="large" :loading="submitting" class="submit-btn" @click="handleReset">
          重置密码
        </el-button>
      </template>

      <p class="back-link">
        <router-link to="/login">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'
import { sendRegisterCode } from '../api/auth'
import request from '../api/request'

const router = useRouter()
const step = ref(1)
const sending = ref(false)
const verifying = ref(false)
const submitting = ref(false)
const codeCountdown = ref(0)
const emailFormRef = ref(null)
const pwdFormRef = ref(null)

const form = reactive({ email: '', code: '', newPassword: '' })

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}
const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度6-100', trigger: 'blur' }
  ]
}

async function handleSendCode() {
  const valid = await emailFormRef.value?.validateField('email').catch(() => false)
  if (!valid) return
  sending.value = true
  try {
    await sendRegisterCode(form.email)
    ElMessage.success('验证码已发送到您的邮箱')
    codeCountdown.value = 60
    const timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    console.error('发送验证码失败', e)
  } finally {
    sending.value = false
  }
}

async function handleNext() {
  const valid = await emailFormRef.value?.validate().catch(() => false)
  if (!valid) return
  step.value = 2
}

async function handleReset() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await request.post('/auth/reset-password', {
      email: form.email,
      code: form.code,
      newPassword: form.newPassword
    })
    ElMessage.success('密码重置成功，请登录')
    router.replace('/login')
  } catch (e) {
    console.error('重置密码失败', e)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.forgot-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f5ff 0%, #f5f7fb 50%, #e6f4ff 100%);
}
.forgot-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 40px 32px;
  box-shadow: 0 8px 40px rgba(0,0,0,.08);
}
.forgot-header {
  text-align: center;
  margin-bottom: 28px;
}
.forgot-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 10px 0 4px;
}
.forgot-desc {
  font-size: 13px;
  color: #94a3b8;
}
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
}
.back-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
}
.back-link a {
  color: #1677ff;
  text-decoration: none;
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
