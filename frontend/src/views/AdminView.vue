<template>
  <div class="admin-container">
    <!-- Header -->
    <header class="header">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="24"><Document /></el-icon>
        <span>AI 文档翻译平台</span>
        <el-tag type="danger" size="small" class="admin-tag">后台管理</el-tag>
      </div>
      <div class="header-right">
        <el-button @click="$router.push('/')">返回翻译</el-button>
        <el-button type="default" @click="handleLogout">退出</el-button>
      </div>
    </header>

    <!-- Stats Cards -->
    <div class="main-content">
      <h2 class="page-title">系统概览</h2>
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.userCount ?? '-' }}</div>
            <div class="stat-label">用户总数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.documentCount ?? '-' }}</div>
            <div class="stat-label">文档总数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.taskCount ?? '-' }}</div>
            <div class="stat-label">翻译任务</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.completedTaskCount ?? '-' }}</div>
            <div class="stat-label">已完成</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="data-tabs">
        <!-- 用户管理 -->
        <el-tab-pane label="用户管理" name="users">
          <el-table :data="users" stripe v-loading="loadingUsers" max-height="500">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column label="角色" width="80">
              <template #default="{ row }">
                <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
                  {{ row.role === 'admin' ? '管理员' : '用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="登录方式" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ row.provider === 'github' ? 'GitHub' : '本地' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="最后登录" width="160">
              <template #default="{ row }">
                {{ formatTime(row.lastLoginAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button
                  :type="row.status === 1 ? 'warning' : 'success'"
                  size="small"
                  @click="handleToggleUser(row)"
                >
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button
                  v-if="row.provider === 'local'"
                  type="primary"
                  size="small"
                  @click="openResetPwd(row)"
                >
                  重置密码
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 文档管理 -->
        <el-tab-pane label="文档管理" name="documents">
          <el-table :data="documents" stripe v-loading="loadingDocs" max-height="500">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="fileName" label="文件名" min-width="180" />
            <el-table-column label="上传者" width="100">
              <template #default="{ row }">
                {{ row.uploaderName || '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="fileType" label="类型" width="90" />
            <el-table-column label="大小" width="100">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 2 ? 'success' : row.status === 3 ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ docStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-popconfirm
                  title="确定要删除此文档吗？"
                  confirm-button-text="删除"
                  @confirm="handleDeleteDocument(row)"
                >
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 翻译任务管理 -->
        <el-tab-pane label="翻译任务" name="tasks">
          <el-table :data="tasks" stripe v-loading="loadingTasks" max-height="500">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="文档" min-width="180">
              <template #default="{ row }">
                {{ row.documentName || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="上传者" width="100">
              <template #default="{ row }">
                {{ row.uploaderName || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="语言" width="90">
              <template #default="{ row }">
                {{ row.sourceLang || '-' }} → {{ row.targetLang || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="进度" width="100">
              <template #default="{ row }">
                {{ row.completedChunks ?? 0 }} / {{ row.totalChunks ?? 0 }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ taskStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-popconfirm
                  title="确定要删除此任务吗？"
                  confirm-button-text="删除"
                  @confirm="handleDeleteTask(row)"
                >
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="showResetPwd" title="重置密码" width="400px" append-to-body>
      <el-form :model="resetForm" :rules="pwdRules" ref="pwdFormRef" label-position="top">
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetForm.password"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetPwd = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 后台管理页面（仅管理员可访问）。
 * 提供系统统计概览、用户管理、文档管理、翻译任务管理功能。
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import request from '../api/request'

const router = useRouter()
const activeTab = ref('users')
const loadingUsers = ref(false)
const loadingDocs = ref(false)
const loadingTasks = ref(false)

const stats = ref({})
const users = ref([])
const documents = ref([])
const tasks = ref([])

// 重置密码
const showResetPwd = ref(false)
const resetting = ref(false)
const selectedUser = ref(null)
const pwdFormRef = ref(null)
const resetForm = reactive({ password: '' })
const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

onMounted(async () => {
  await Promise.all([fetchStats(), fetchUsers(), fetchDocuments(), fetchTasks()])
})

async function fetchStats() {
  try {
    const res = await request.get('/admin/stats')
    stats.value = res.data
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

async function fetchUsers() {
  loadingUsers.value = true
  try {
    const res = await request.get('/admin/users')
    users.value = res.data || []
  } catch (e) {
    console.error('获取用户列表失败', e)
  } finally {
    loadingUsers.value = false
  }
}

async function fetchDocuments() {
  loadingDocs.value = true
  try {
    const res = await request.get('/admin/documents')
    documents.value = res.data || []
  } catch (e) {
    console.error('获取文档列表失败', e)
  } finally {
    loadingDocs.value = false
  }
}

async function fetchTasks() {
  loadingTasks.value = true
  try {
    const res = await request.get('/admin/tasks')
    tasks.value = res.data || []
  } catch (e) {
    console.error('获取任务列表失败', e)
  } finally {
    loadingTasks.value = false
  }
}

async function handleToggleUser(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await request.put(`/admin/users/${row.id}/status`, { status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  } catch (e) {
    console.error('更新用户状态失败', e)
  }
}

function openResetPwd(row) {
  selectedUser.value = row
  resetForm.password = ''
  showResetPwd.value = true
}

async function handleResetPwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  resetting.value = true
  try {
    await request.put(`/admin/users/${selectedUser.value.id}/reset-password`, {
      password: resetForm.password
    })
    showResetPwd.value = false
    ElMessage.success('密码已重置')
  } catch (e) {
    console.error('重置密码失败', e)
  } finally {
    resetting.value = false
  }
}

async function handleDeleteDocument(row) {
  try {
    await request.delete(`/admin/documents/${row.id}`)
    documents.value = documents.value.filter(d => d.id !== row.id)
    ElMessage.success('文档已删除')
  } catch (e) {
    console.error('删除文档失败', e)
  }
}

async function handleDeleteTask(row) {
  try {
    await request.delete(`/admin/tasks/${row.id}`)
    tasks.value = tasks.value.filter(t => t.id !== row.id)
    ElMessage.success('任务已删除')
  } catch (e) {
    console.error('删除任务失败', e)
  }
}

function handleLogout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  router.replace('/login')
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function docStatusLabel(status) {
  return { 0: '已上传', 1: '解析中', 2: '已解析', 3: '解析失败', 4: '翻译中' }[status] || '未知'
}

function taskStatusLabel(status) {
  return { 0: '进行中', 1: '已完成', 2: '失败' }[status] || '未知'
}
</script>

<style scoped>
.admin-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fb;
}
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
.admin-tag {
  margin-left: 4px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 36px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 20px;
}
.stats-row {
  margin-bottom: 24px;
}
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1677ff;
}
.stat-label {
  font-size: 14px;
  color: #94a3b8;
  margin-top: 8px;
}
.data-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
</style>
