<template>
  <div class="profile-container">
    <!-- Header -->
    <header class="header">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="24"><Document /></el-icon>
        <span>AI 文档翻译平台</span>
        <el-button
          v-if="userInfo?.role === 'admin'"
          type="warning"
          plain
          @click.stop="$router.push('/admin')"
        >
          后台管理
        </el-button>
      </div>
      <div class="header-right">
        <el-button @click="$router.push('/')">返回翻译</el-button>
        <el-button type="default" @click="handleLogout">退出</el-button>
      </div>
    </header>

    <div class="main-content">
      <!-- Sidebar: User Info Card -->
      <aside class="user-sidebar">
        <div class="user-card">
          <el-upload
            class="sidebar-avatar-upload"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleSidebarAvatarChange"
          >
            <div class="sidebar-avatar-box">
              <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" class="sidebar-avatar-img" />
              <el-avatar v-else :size="72" />
              <div class="sidebar-avatar-overlay">
                <el-icon :size="20" color="#fff"><Camera /></el-icon>
              </div>
            </div>
          </el-upload>
          <h2>{{ userInfo?.username }}</h2>
          <p class="user-email">{{ userInfo?.email }}</p>
          <el-tag
            :type="userInfo?.role === 'admin' ? 'danger' : 'info'"
            size="small"
          >
            {{ userInfo?.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
          <div class="profile-actions">
            <el-button type="primary" size="small" @click="openEditDialog">
              编辑资料
            </el-button>
            <el-button size="small" @click="openPasswordDialog">
              修改密码
            </el-button>
          </div>
        </div>
      </aside>

      <!-- History List -->
      <main class="history-main">
        <h3 class="section-title">翻译历史</h3>
        <div v-if="loading" class="loading-box">
          <el-skeleton :rows="3" animated />
        </div>
        <el-empty v-else-if="tasks.length === 0" description="暂无翻译记录" />
        <div v-else class="history-list">
          <div
            v-for="item in tasks"
            :key="item.taskId"
            class="history-item"
            @click="goToTask(item)"
          >
            <div class="history-left">
              <el-icon :size="20" color="#1677ff"><Document /></el-icon>
              <div class="history-info">
                <span class="history-name">{{ item.documentName || '未命名文档' }}</span>
                <span class="history-meta">
                  {{ item.sourceLang }} → {{ item.targetLang }}
                  · {{ item.completedChunks }}/{{ item.totalChunks }} chunks
                </span>
              </div>
            </div>
            <div class="history-right">
              <el-tag :type="getStatusType(item.status)" size="small">
                {{ getStatusLabel(item.status) }}
              </el-tag>
              <span class="history-time">{{ formatTime(item.createdAt) }}</span>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPwdDialog" :title="pwdStep === 1 ? '验证身份' : '设置新密码'" width="420px" destroy-on-close>
      <!-- Step 1: 邮箱验证 -->
      <template v-if="pwdStep === 1">
        <p class="pwd-hint">验证码将发送到您的注册邮箱</p>
        <el-form ref="codeFormRef" :model="pwdForm" label-position="top" @submit.prevent>
          <el-form-item label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model="pwdForm.code" placeholder="6位验证码" maxlength="6" />
              <el-button :disabled="codeCountdown > 0" :loading="sendingCode" @click="handleSendCode">
                {{ codeCountdown > 0 ? codeCountdown + 's' : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </template>
      <!-- Step 2: 修改密码 -->
      <template v-else>
        <p class="pwd-hint">验证通过，请设置新密码</p>
        <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-100个字符" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <template v-if="pwdStep === 1">
          <el-button @click="showPwdDialog = false">取消</el-button>
          <el-button type="primary" :loading="verifyingCode" @click="handleVerifyCode">下一步</el-button>
        </template>
        <template v-else>
          <el-button @click="pwdStep = 1">上一步</el-button>
          <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">确认修改</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑个人资料" width="420px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="3-50个字符" />
        </el-form-item>
        <el-form-item label="头像" class="avatar-form-item">
          <el-upload
            ref="uploadRef"
            class="avatar-uploader"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleAvatarChange"
          >
            <div class="avatar-box" :class="{ uploading }">
              <img v-if="editForm.avatarUrl" :src="editForm.avatarUrl" class="avatar-img" />
              <el-icon v-else :size="28" color="#c0c4cc"><Plus /></el-icon>
              <div v-if="editForm.avatarUrl" class="avatar-overlay">
                <el-icon :size="20" color="#fff"><Camera /></el-icon>
              </div>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Camera } from '@element-plus/icons-vue'
import { getCurrentUser, updateProfile, uploadAvatar, sendVerificationCode, changePassword } from '../api/auth'
import request from '../api/request'

const router = useRouter()
const loading = ref(true)
const userInfo = ref(null)
const tasks = ref([])

// ===== 编辑资料 =====
const showEditDialog = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ username: '', avatarUrl: '' })
const editRules = {}

const uploading = ref(false)

/** 侧边栏头像变更回调，委托给 {@link handleAvatarChange} */
async function handleSidebarAvatarChange(file) {
  await handleAvatarChange(file)
}

/**
 * 上传头像文件，成功后更新本地用户信息。
 * @param {Object} file - Element Plus Upload 组件传入的文件对象
 * @param {File} file.raw - 原始 File 对象
 * @returns {Promise<void>}
 */
async function handleAvatarChange(file) {
  uploading.value = true
  try {
    const res = await uploadAvatar(file.raw)
    editForm.avatarUrl = res.data.avatarUrl
    userInfo.value = res.data
    ElMessage.success('头像已更新')
  } catch (e) {
    console.error('头像上传失败', e)
  } finally {
    uploading.value = false
  }
}

// ===== 修改密码 =====
const showPwdDialog = ref(false)
const pwdStep = ref(1)
const changingPwd = ref(false)
const verifyingCode = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', code: '' })
const verifiedCode = ref('') // 已验证通过的验证码
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度6-100', trigger: 'blur' }
  ]
}

/** 打开修改密码对话框，重置所有表单状态 */
function openPasswordDialog() {
  pwdStep.value = 1
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.code = ''
  verifiedCode.value = ''
  codeCountdown.value = 0
  showPwdDialog.value = true
}

/**
 * 发送修改密码用的邮箱验证码，启动 60s 倒计时。
 * @returns {Promise<void>}
 */
async function handleSendCode() {
  if (!pwdForm.code) {
    // 发验证码不用校验，直接发
  }
  sendingCode.value = true
  try {
    await sendVerificationCode()
    ElMessage.success('验证码已发送到您的邮箱')
    codeCountdown.value = 60
    const timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    console.error('发送验证码失败', e)
  } finally {
    sendingCode.value = false
  }
}

/** 验证码输入后进入第二步（设置新密码） */
function handleVerifyCode() {
  if (!pwdForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  pwdStep.value = 2
}

/**
 * 提交修改密码请求，成功后关闭对话框。
 * @returns {Promise<void>}
 */
async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      code: pwdForm.code
    })
    showPwdDialog.value = false
    ElMessage.success('密码修改成功')
  } catch (e) {
    console.error('修改密码失败', e)
  } finally {
    changingPwd.value = false
  }
}

/** 打开编辑资料对话框，预填当前用户信息 */
function openEditDialog() {
  editForm.username = userInfo.value?.username || ''
  editForm.avatarUrl = userInfo.value?.avatarUrl || ''
  showEditDialog.value = true
}

/**
 * 保存个人资料更新。
 * @returns {Promise<void>}
 */
async function handleSaveProfile() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = await updateProfile(editForm)
    userInfo.value = res.data
    showEditDialog.value = false
    ElMessage.success('资料已更新')
  } catch (e) {
    console.error('保存失败', e)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  fetchUserInfo()
  fetchHistory()
})

/** 获取当前登录用户信息，401 时跳转登录页 */
async function fetchUserInfo() {
  try {
    const res = await getCurrentUser()
    userInfo.value = res.data
  } catch {
    router.replace('/login')
  }
}

/** 获取当前用户的翻译历史记录 */
async function fetchHistory() {
  try {
    const res = await request.get('/translation/history')
    tasks.value = res.data || []
  } catch (e) {
    console.error('获取历史记录失败', e)
  } finally {
    loading.value = false
  }
}

/** 退出登录：清除本地 token 并跳转到登录页 */
function handleLogout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  router.replace('/login')
}

/**
 * 点击历史记录项跳转到对应文档的翻译页面。
 * @param {Object} item - 翻译任务记录
 * @param {number} item.documentId - 文档 ID
 */
function goToTask(item) {
  router.push({ path: '/', query: { documentId: item.documentId } })
}

/**
 * 将任务状态码映射为 Element Plus Tag 类型。
 * @param {number} status
 * @returns {string} Element Plus Tag type（warning/success/danger/info）
 */
function getStatusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

/**
 * 将任务状态码映射为中文标签。
 * @param {number} status
 * @returns {string}
 */
function getStatusLabel(status) {
  return { 0: '进行中', 1: '已完成', 2: '失败' }[status] || '未知'
}

/**
 * 格式化时间为中文短日期格式。
 * @param {string|Date} time
 * @returns {string}
 */
function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN', {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}
</script>

<style scoped>
.profile-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fb;
}

/* ===== Header ===== */
.header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  flex-shrink: 0;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: #1677ff;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
/* ===== Main ===== */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ===== Sidebar ===== */
.user-sidebar {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  padding: 32px 24px;
  flex-shrink: 0;
}
.sidebar-avatar-upload :deep(.el-upload) {
  border: none;
  display: block;
}
.sidebar-avatar-box {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  margin: 0 auto;
}
.sidebar-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.sidebar-avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity .2s;
}
.profile-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.code-row :deep(.el-input) {
  flex: 1;
}
.code-row :deep(.el-button) {
  flex-shrink: 0;
  white-space: nowrap;
}
.pwd-hint {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 16px;
}
.sidebar-avatar-box:hover .sidebar-avatar-overlay {
  opacity: 1;
}
.user-card {
  text-align: center;
}
.user-card h2 {
  margin: 16px 0 4px;
  font-size: 18px;
  color: #1e293b;
}
.user-email {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 6px;
}

/* ===== History ===== */
.history-main {
  flex: 1;
  overflow-y: auto;
  padding: 28px 36px;
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}
.loading-box {
  padding: 20px 0;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 20px;
  border-radius: 10px;
  cursor: pointer;
  transition: all .2s;
}
.history-item:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,.06);
  transform: translateY(-1px);
}
.history-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.history-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.history-name {
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
}
.history-meta {
  font-size: 12px;
  color: #94a3b8;
}
.history-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.avatar-form-item :deep(.el-form-item__content) {
  justify-content: center;
}
.avatar-uploader :deep(.el-upload) {
  border: none;
}
.avatar-box {
  width: 100px;
  height: 100px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  transition: border-color .2s;
}
.avatar-box:hover {
  border-color: #1677ff;
}
.avatar-box.uploading {
  pointer-events: none;
  opacity: .6;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity .2s;
}
.avatar-box:hover .avatar-overlay {
  opacity: 1;
}
.history-time {
  font-size: 12px;
  color: #cbd5e1;
  white-space: nowrap;
}
</style>
