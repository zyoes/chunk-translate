<template>
  <div class="app-container">
    <!-- ================= Header ================= -->
    <header class="header">
      <div class="logo">
        <el-icon :size="24"><Document /></el-icon>
        <span>AI 文档翻译平台</span>
      </div>
      <div class="header-right">
        <template v-if="!isLoggedIn">
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
        </template>
        <template v-else>
          <el-dropdown @command="handleUserCommand">
            <span class="user-dropdown-trigger">
              <el-avatar :size="32" :src="userInfo?.avatarUrl" />
              <span class="user-name">{{ userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人主页
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-divider direction="vertical" />
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          accept=".pdf,.docx,.pptx,.txt,.md"
        >
          <el-button type="primary" :icon="UploadFilled">
            {{ documentId ? '重新上传' : '上传文档' }}
          </el-button>
        </el-upload>
        <el-button
          v-if="!isTranslating"
          type="success"
          :icon="VideoPlay"
          @click="handleStartTranslation"
          :disabled="!documentId"
        >
          开始翻译
        </el-button>
        <el-button
          v-if="isTranslating"
          type="danger"
          :icon="Close"
          @click="handleStopTranslation"
        >
          中止翻译
        </el-button>
        <el-dropdown @command="handleExport" :disabled="!documentId">
          <el-button :icon="Download">导出</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="docx">导出 DOCX</el-dropdown-item>
              <el-dropdown-item command="pdf">导出 PDF</el-dropdown-item>
              <el-dropdown-item command="markdown">导出 Markdown</el-dropdown-item>
              <el-dropdown-item command="txt">导出 TXT</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- ================= Main ================= -->
    <div class="main">
      <!-- Left: Document Tree -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <el-icon><FolderOpened /></el-icon>
          文档目录
        </div>
        <div v-if="!documentId" class="sidebar-empty">
          <el-empty description="请先上传文档" :image-size="60" />
        </div>
        <el-tree
          v-else
          :data="treeData"
          :props="{ label: 'title', children: 'children' }"
          highlight-current
          default-expand-all
          @node-click="handleNodeClick"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="tree-label">{{ data.title }}</span>
              <el-tag
                v-if="data.translationStatus !== undefined"
                :type="getStatusTagType(data.translationStatus)"
                size="small"
              >
                {{ getStatusText(data.translationStatus) }}
              </el-tag>
            </div>
          </template>
        </el-tree>
      </aside>

      <!-- Center: Dual Panels -->
      <div class="content">
        <!-- Left Panel: Source（支持点击校对） -->
        <div class="panel">
          <div class="panel-header">
            <span>📖 原文内容</span>
            <div class="panel-header-actions">
              <el-tag v-if="selectedChunk" type="info" size="small">
                {{ truncatedTitle }}
              </el-tag>
              <el-button
                v-if="selectedChunk && editingField !== 'source'"
                size="small"
                type="warning"
                :icon="Edit"
                @click="startEditSource"
              >
                校对
              </el-button>
              <template v-if="editingField === 'source'">
                <el-button size="small" type="success" :icon="Check" @click="saveEdit">保存</el-button>
                <el-button size="small" :icon="Close" @mousedown="cancelEdit">取消</el-button>
              </template>
            </div>
          </div>
          <div class="panel-body">
            <template v-if="selectedChunk">
              <h2 class="panel-title" :title="selectedChunk.title">{{ selectedChunk.title }}</h2>
              <textarea
                v-if="editingField === 'source'"
                v-model="editText"
                class="edit-textarea source-edit"
                placeholder="在此修改原文..."
                @blur="handleEditBlur"
              />
              <p
                v-else
                class="panel-text clickable-text"
                @click="startEditSource"
              >{{ selectedChunk.content || '暂无内容' }}</p>
            </template>
            <el-empty v-else description="请从左侧目录选择章节" />
          </div>
        </div>

        <!-- Right Panel: Translation（支持点击校对） -->
        <div class="panel">
          <div class="panel-header">
            <span>🌍 翻译结果</span>
            <div class="panel-header-actions">
              <el-tag v-if="hasTranslation" type="success" size="small">已翻译</el-tag>
              <el-button
                v-if="hasTranslation && editingField !== 'translation'"
                size="small"
                type="warning"
                :icon="Edit"
                @click="startEditTranslation"
              >
                校对
              </el-button>
              <template v-if="editingField === 'translation'">
                <el-button size="small" type="success" :icon="Check" @click="saveEdit">保存</el-button>
                <el-button size="small" :icon="Close" @mousedown="cancelEdit">取消</el-button>
              </template>
            </div>
          </div>
          <div class="panel-body">
            <template v-if="selectedChunk && hasTranslation">
              <h2 class="panel-title" :title="selectedChunk.title">{{ translatedTitle }}</h2>
              <textarea
                v-if="editingField === 'translation'"
                v-model="editText"
                class="edit-textarea"
                placeholder="在此修改译文..."
                @blur="handleEditBlur"
              />
              <p
                v-else
                class="panel-text clickable-text"
                @click="startEditTranslation"
              >{{ selectedChunk.translation || '(本分块原文为空，无需翻译)' }}</p>
            </template>
            <el-empty v-else-if="!selectedChunk" description="请从左侧目录选择章节" />
            <el-empty v-else description="暂未翻译，点击右上方「开始翻译」" />
          </div>
        </div>
      </div>

      <!-- Right: Config Panel -->
      <aside class="config">
        <div class="config-header">
          <el-icon><Setting /></el-icon>
          翻译配置
        </div>
        <div class="config-form">
          <div class="form-group">
            <label>源语言</label>
            <el-select v-model="sourceLang" placeholder="选择源语言">
              <el-option label="自动检测" value="auto" />
              <el-option label="中文" value="zh" />
              <el-option label="英文" value="en" />
              <el-option label="日文" value="ja" />
              <el-option label="韩文" value="ko" />
            </el-select>
          </div>
          <div class="form-group">
            <label>目标语言</label>
            <el-select v-model="targetLang" placeholder="选择目标语言">
              <el-option label="中文" value="zh" />
              <el-option label="英文" value="en" />
              <el-option label="日文" value="ja" />
              <el-option label="韩文" value="ko" />
            </el-select>
          </div>

          <!-- Document Info -->
          <div v-if="documentId" class="doc-info">
            <el-divider />
            <div class="doc-info-item">
              <span>文档：</span>
              <el-tag size="small">{{ fileName }}</el-tag>
            </div>
          </div>

          <!-- Chunk Status -->
          <div v-if="chunks.length > 0" class="chunk-box">
            <el-divider />
            <strong>Chunk 任务状态</strong>
            <div class="chunk-list">
              <div v-for="chunk in chunks" :key="chunk.chunkId" class="chunk-item">
                <span class="chunk-name">{{ chunk.title || `Chunk-${chunk.sequence}` }}</span>
                <el-tag :type="getStatusTagType(chunk.status)" size="small">
                  {{ chunk.statusDesc }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- ================= Footer ================= -->
    <footer class="footer">
      <div class="footer-top">
        <span>翻译总进度：{{ progressPercent }}%</span>
        <span v-if="documentId">{{ completedChunks }} / {{ totalChunks }} Chunk</span>
        <span v-else>未上传文档</span>
      </div>
      <el-progress
        :percentage="progressPercent"
        :stroke-width="10"
        :show-text="false"
        :color="progressPercent >= 100 ? '#52c41a' : '#1677ff'"
      />
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
import { ElMessage, ElLoading } from 'element-plus'
import { UploadFilled, VideoPlay, Download, Edit, Check, Close, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import request from '../api/request'
import { uploadDocument, getDocumentDetail } from '../api/document'
import { startTranslation, getProgress, exportFile, stopTranslation } from '../api/translation'
import { updateChunkTranslation, updateChunkSource } from '../api/edit'

// ==================== 状态 ====================
const documentId = ref(null)
const fileName = ref('')
const sourceLang = ref('auto')
const targetLang = ref('zh')
const isTranslating = ref(false)
const treeData = ref([])
const chunks = ref([])

// Fix1: 用 selectedNode 记录当前点击的树节点，selectedChunk 改为 computed 动态派生
const selectedNode = ref(null)

// 校对编辑状态：editingField = null | 'source' | 'translation'
const editingField = ref(null)
const editText = ref('')

let progressTimer = null
let editCancelled = false

// ==================== 认证状态 ====================
const isLoggedIn = ref(false)
const userInfo = ref(null)

onMounted(() => {
  if (localStorage.getItem('accessToken')) {
    isLoggedIn.value = true
    fetchUserInfo()
  }
  // 从历史记录跳转过来时，自动加载文档
  const docId = route.query.documentId
  if (docId) {
    documentId.value = Number(docId)
    loadDocumentDetail(Number(docId))
  }
})

async function fetchUserInfo() {
  try {
    const res = await request.get('/auth/me')
    userInfo.value = res.data
  } catch (e) {
    // token 失效，清空登录状态
    handleLogout()
  }
}

function handleUserCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    handleLogout()
  }
}

function handleLogout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  isLoggedIn.value = false
  userInfo.value = null
  documentId.value = null
  fileName.value = ''
  treeData.value = []
  chunks.value = []
  selectedNode.value = null
}


// ==================== 计算属性 ====================
const totalChunks = computed(() => chunks.value.length)
const completedChunks = computed(() =>
  chunks.value.filter(c => c.status === 2 || c.status === 3).length
)
const progressPercent = computed(() => {
  if (totalChunks.value === 0) return 0
  return Math.round((completedChunks.value / totalChunks.value) * 100)
})

/** 当前分块是否已完成翻译（status 2=已完成，3=失败） */
const hasTranslation = computed(() => {
  const chunk = selectedChunk.value
  return chunk && (chunk.status === 2 || chunk.status === 3)
})

/** 面板标题截断（el-tag 用，最多25字 + ...） */
const truncatedTitle = computed(() => {
  const t = selectedChunk.value?.title || ''
  return t.length > 25 ? t.slice(0, 25) + '...' : t
})

/** 译文面板段落标题：优先使用后端返回的翻译标题，否则本地生成 */
const translatedTitle = computed(() => {
  const chunk = selectedChunk.value
  // 后端返回的翻译标题
  if (chunk?.translatedTitle) {
    return chunk.translatedTitle
  }
  // 本地生成：从 chunks 数组中查找
  const chunkData = chunks.value.find(c => c.chunkId === chunk?.chunkId)
  if (chunkData?.translatedTitle) {
    return chunkData.translatedTitle
  }
  // 回退：提取段落编号
  const t = selectedChunk.value?.title || ''
  const match = t.match(/段落\s*(\d+)/)
  if (match) return 'Paragraph ' + match[1]
  return t.length > 30 ? t.slice(0, 30) + '...' : t
})

/**
 * Fix1 + Fix2: selectedChunk 从 selectedNode + chunks 动态计算
 * - 切换树节点 → selectedNode 变化 → selectedChunk 自动重新计算
 * - 轮询更新 chunks → selectedChunk 自动重新计算（译文实时刷新）
 */
const selectedChunk = computed(() => {
  const node = selectedNode.value
  if (!node) return null

  // 通过 nodeId 匹配 chunk（后端 nodeId 就是 chunkId 的字符串形式，如 "123"）
  let chunk = null
  if (node.nodeId) {
    const chunkId = parseInt(node.nodeId)
    chunk = chunks.value.find(c => c.chunkId === chunkId)
  }
  if (!chunk) {
    chunk = chunks.value.find(c => c.title === node.title)
  }

  return {
    title: node.title,
    content: node.content || '',
    // chunk 未找到时，直接用 nodeId 作为 chunkId（翻译前也能保存原文）
    chunkId: chunk?.chunkId || (node.nodeId ? parseInt(node.nodeId) : null),
    translation: chunk?.translation || null,
    translatedTitle: chunk?.translatedTitle || null,
    status: chunk?.status
  }
})

// ==================== 上传 ====================
async function handleFileChange(file) {
  const loading = ElLoading.service({ text: '上传中...' })
  try {
    const res = await uploadDocument(file.raw)
    const data = res.data
    documentId.value = data.id
    fileName.value = data.fileName
    // 重置选中状态
    selectedNode.value = null
    editingField.value = null
    chunks.value = []
    ElMessage.success('上传成功，正在解析文档...')
    setTimeout(() => loadDocumentDetail(data.id), 2000)
  } catch (e) {
    console.error('上传失败', e)
  } finally {
    loading.close()
  }
}

async function loadDocumentDetail(id) {
  try {
    const res = await getDocumentDetail(id)
    treeData.value = res.data.tree || []
    ElMessage.success('文档解析完成')
    // 同时加载翻译进度（填充 chunks 数组，使 selectedChunk.chunkId 可用）
    try {
      const progressRes = await getProgress(id)
      chunks.value = progressRes.data.chunks || []
    } catch (e) {
      // 进度查询失败不影响文档展示
    }
    // 自动选中第一个节点，立即显示原文内容
    if (treeData.value.length > 0) {
      selectedNode.value = treeData.value[0]
    }
  } catch (e) {
    console.error('加载文档详情失败', e)
  }
}

// ==================== 翻译 ====================
async function handleStartTranslation() {
  if (!documentId.value) return
  try {
    await startTranslation({
      documentId: documentId.value,
      sourceLang: sourceLang.value,
      targetLang: targetLang.value
    })
    isTranslating.value = true
    ElMessage.success('翻译任务已启动')
    startPolling()
  } catch (e) {
    console.error('启动翻译失败', e)
  }
}

// 中止翻译：调用后端停止接口 + 停止轮询
async function handleStopTranslation() {
  if (!documentId.value) return
  try {
    await stopTranslation(documentId.value)
    stopPolling()
    isTranslating.value = false
    ElMessage.warning('翻译已中止，未完成分块已回滚为待翻译')
    // 刷新进度，使回滚状态立刻显示
    const res = await getProgress(documentId.value)
    chunks.value = res.data.chunks || []
  } catch (e) {
    console.error('中止翻译失败', e)
  }
}

function startPolling() {
  stopPolling()
  progressTimer = setInterval(async () => {
    try {
      const res = await getProgress(documentId.value)
      const data = res.data
      // 更新 chunks 数组 → selectedChunk computed 自动重新计算 → 面板实时刷新
      chunks.value = data.chunks || []
      updateTreeStatus(data.chunks)
      if (data.progressPercent >= 100) {
        isTranslating.value = false
        stopPolling()
        ElMessage.success('翻译完成！')
        // 翻译完成后刷新文档详情（目录树可能更新）
        loadDocumentDetail(documentId.value)
      }
    } catch (e) {
      console.error('查询进度失败', e)
    }
  }, 3000)
}

function stopPolling() {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

function updateTreeStatus(chunkList) {
  if (!chunkList || !treeData.value.length) return
  treeData.value.forEach((node, index) => {
    const chunk = chunkList.find(c => c.sequence === index + 1)
    if (chunk) node.translationStatus = chunk.status
  })
}

// ==================== 导出 ====================
function handleExport(format) {
  if (!documentId.value) return
  exportFile(documentId.value, format)
  ElMessage.success(`正在导出 ${format.toUpperCase()}...`)
}

// ==================== 目录树点击 ====================
function handleNodeClick(data) {
  selectedNode.value = data
  // 切换节点时退出编辑模式
  editingField.value = null
}

// ==================== 校对编辑 ====================
function startEditSource() {
  editCancelled = false
  editText.value = selectedChunk.value?.content || ''
  editingField.value = 'source'
}

function startEditTranslation() {
  editCancelled = false
  editText.value = selectedChunk.value?.translation || ''
  editingField.value = 'translation'
}

function cancelEdit() {
  editCancelled = true
  editingField.value = null
}

// 失焦时自动保存（mousedown 于取消按钮先触发，设 editCancelled 标记避免误保存）
function handleEditBlur() {
  if (!editingField.value) return
  if (editCancelled) {
    editCancelled = false
    return
  }
  saveEdit()
}

async function saveEdit() {
  const chunk = selectedChunk.value
  if (!chunk?.chunkId) {
    ElMessage.warning('无法保存：分块ID不存在')
    return
  }

  try {
    if (editingField.value === 'translation') {
      // 保存译文
      await updateChunkTranslation(chunk.chunkId, editText.value)
      const idx = chunks.value.findIndex(c => c.chunkId === chunk.chunkId)
      if (idx !== -1) {
        chunks.value[idx] = { ...chunks.value[idx], translation: editText.value }
      }
      ElMessage.success('译文已保存')
    } else if (editingField.value === 'source') {
      // 保存原文
      await updateChunkSource(chunk.chunkId, editText.value)
      // 更新当前树节点的 content
      if (selectedNode.value) {
        selectedNode.value = { ...selectedNode.value, content: editText.value }
      }
      ElMessage.success('原文已保存')
    }
    editingField.value = null
  } catch (e) {
    console.error('保存失败', e)
  }
}

// ==================== 辅助 ====================
function getStatusTagType(status) {
  return { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[status] || 'info'
}
function getStatusText(status) {
  return { 0: '待翻译', 1: '翻译中', 2: '已完成', 3: '失败' }[status] || ''
}

onBeforeUnmount(() => stopPolling())
</script>

<style scoped>
/* ================= CSS 变量 ================= */
.app-container {
  --color-primary: #1677ff;
  --color-primary-light: #e6f4ff;
  --color-success: #52c41a;
  --color-success-light: #f6ffed;
  --color-warning: #fa8c16;
  --color-warning-light: #fffbe6;
  --color-danger: #ff4d4f;
  --color-text: #1e293b;
  --color-text-secondary: #64748b;
  --color-border: #e5e7eb;
  --color-border-light: #f0f0f0;
  --color-bg: #f5f7fb;
  --color-bg-card: #ffffff;
  --radius: 12px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,.04);
  --shadow-md: 0 4px 16px rgba(0,0,0,.06);
  --transition: all .25s cubic-bezier(.4,0,.2,1);

  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  font-family: "Microsoft YaHei", -apple-system, "PingFang SC", sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ================= Header ================= */
.header {
  height: 64px;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
  z-index: 10;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: .5px;
}
.logo :deep(.el-icon) {
  color: var(--color-primary);
}
.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background .2s;
}
.user-dropdown-trigger:hover {
  background: #f3f4f6;
}
.user-name {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.5;
}

/* ================= Main ================= */
.main {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

/* ================= Sidebar ================= */
.sidebar {
  width: 280px;
  background: var(--color-bg-card);
  border-right: 1px solid var(--color-border);
  overflow-y: auto;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
.sidebar-header {
  padding: 18px 20px;
  font-weight: 700;
  font-size: 15px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text);
  background: #fafbfc;
  position: sticky;
  top: 0;
  z-index: 1;
}
.sidebar-empty {
  padding: 48px 0;
}
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 6px;
  gap: 8px;
}
.tree-node :deep(.el-tag) {
  flex-shrink: 0;
}
.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
  font-size: 13px;
  flex-shrink: 1;
  transition: var(--transition);
}

/* ================= Content Panels ================= */
.content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}
.panel {
  flex: 1;
  background: var(--color-bg-card);
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  box-shadow: var(--shadow-md);
  transition: var(--transition);
  border: 1px solid transparent;
}
.panel:hover {
  border-color: var(--color-border);
}
.panel-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  font-weight: 600;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  background: #fafbfc;
  color: var(--color-text);
  font-size: 14px;
}
.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  line-height: 1.9;
}
.panel-body :deep(h2) {
  margin-bottom: 20px;
  color: var(--color-text);
  font-size: 20px;
  position: relative;
  padding-bottom: 12px;
}
.panel-body :deep(h2)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 40px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 2px;
}
.panel-title {
  margin-bottom: 20px;
  color: var(--color-text);
  font-size: 20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  position: relative;
  padding-bottom: 12px;
}
.panel-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 40px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 2px;
}
.panel-text {
  white-space: pre-wrap;
  color: #334155;
  font-size: 15px;
}
.clickable-text {
  cursor: pointer;
  padding: 4px 0;
  border-radius: 6px;
  transition: var(--transition);
}
.clickable-text:hover {
  background: var(--color-primary-light);
  padding: 4px 8px;
  margin: 0 -8px;
}

/* ================= 编辑区 ================= */
.edit-textarea {
  width: 100%;
  min-height: 320px;
  height: calc(100% - 80px);
  border: 2px solid #e5a000;
  border-radius: 10px;
  padding: 16px;
  font-size: 15px;
  line-height: 1.9;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  color: #334155;
  background: var(--color-warning-light);
  resize: vertical;
  outline: none;
  white-space: pre-wrap;
  transition: var(--transition);
}
.edit-textarea:focus {
  border-color: #e5a000;
  box-shadow: 0 0 0 4px rgba(229,160,0,.12);
}
.edit-textarea.source-edit {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}
.edit-textarea.source-edit:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(22,119,255,.12);
}

/* ================= Config Panel ================= */
.config {
  width: 320px;
  background: var(--color-bg-card);
  border-left: 1px solid var(--color-border);
  overflow-y: auto;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
.config-header {
  padding: 18px 20px;
  font-weight: 700;
  font-size: 15px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text);
  background: #fafbfc;
  position: sticky;
  top: 0;
  z-index: 1;
}
.config-form {
  padding: 20px;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text);
  font-weight: 600;
  font-size: 13px;
  letter-spacing: .3px;
}
.form-group :deep(.el-select) {
  width: 100%;
}
.doc-info {
  margin-top: 8px;
}
.doc-info :deep(.el-divider) {
  margin: 16px 0;
}
.doc-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 6px 0;
}
.chunk-box {
  margin-top: 8px;
}
.chunk-box :deep(.el-divider) {
  margin: 16px 0;
}
.chunk-box > strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: .3px;
}
.chunk-list {
  margin-top: 14px;
  max-height: 320px;
  overflow-y: auto;
  background: #fafbfc;
  border-radius: 8px;
  padding: 4px 12px;
}
.chunk-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 4px;
  border-bottom: 1px solid var(--color-border-light);
  transition: var(--transition);
}
.chunk-item:last-child {
  border-bottom: none;
}
.chunk-item:hover {
  background: #f0f5ff;
  margin: 0 -8px;
  padding: 8px 12px;
  border-radius: 6px;
}
.chunk-name {
  font-size: 13px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

/* ================= Footer ================= */
.footer {
  padding: 16px 28px;
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
  box-shadow: 0 -1px 3px rgba(0,0,0,.03);
}
.footer-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
}
.footer-top span:first-child {
  color: var(--color-text);
  font-weight: 600;
  font-size: 14px;
}
.footer :deep(.el-progress) {
  line-height: 1;
}

/* ================= 滚动条美化 ================= */
.sidebar::-webkit-scrollbar,
.panel-body::-webkit-scrollbar,
.config::-webkit-scrollbar,
.chunk-list::-webkit-scrollbar {
  width: 5px;
}
.sidebar::-webkit-scrollbar-thumb,
.panel-body::-webkit-scrollbar-thumb,
.config::-webkit-scrollbar-thumb,
.chunk-list::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 10px;
}
.sidebar::-webkit-scrollbar-thumb:hover,
.panel-body::-webkit-scrollbar-thumb:hover,
.config::-webkit-scrollbar-thumb:hover {
  background: #b0b7c3;
}

/* ================= Element Plus 微调 ================= */
.sidebar :deep(.el-tree-node__content) {
  padding: 6px 12px;
  margin: 2px 4px;
  border-radius: 8px;
  transition: var(--transition);
}
.sidebar :deep(.el-tree-node__content:hover) {
  background: #f3f4f6;
}
.sidebar :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--color-primary-light) !important;
  color: var(--color-primary);
}
</style>
