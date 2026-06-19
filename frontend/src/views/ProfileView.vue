<template>
  <div class="profile-container">
    <!-- Header -->
    <header class="header">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="24"><Document /></el-icon>
        <span>AI 文档翻译平台</span>
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
          <div style="margin-top: 8px;">
            <el-button type="primary" size="small" @click="openEditDialog">
              编辑资料
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
import { getCurrentUser, updateProfile, uploadAvatar } from '../api/auth'
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

async function handleSidebarAvatarChange(file) {
  await handleAvatarChange(file)
}

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

function openEditDialog() {
  editForm.username = userInfo.value?.username || ''
  editForm.avatarUrl = userInfo.value?.avatarUrl || ''
  showEditDialog.value = true
}

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

async function fetchUserInfo() {
  try {
    const res = await getCurrentUser()
    userInfo.value = res.data
  } catch {
    router.replace('/login')
  }
}

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

function handleLogout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  router.replace('/login')
}

function goToTask(item) {
  router.push({ path: '/', query: { documentId: item.documentId } })
}

function getStatusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

function getStatusLabel(status) {
  return { 0: '进行中', 1: '已完成', 2: '失败' }[status] || '未知'
}

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
