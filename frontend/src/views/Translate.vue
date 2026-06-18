<template>
  <div class="app-container">
    <!-- ================= Header ================= -->
    <header class="header">
      <div class="logo">
        <el-icon :size="24"><Document /></el-icon>
        <span>AI 文档翻译平台</span>
      </div>
      <div class="header-right">
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
                {{ selectedChunk.title }}
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
                <el-button size="small" :icon="Close" @click="cancelEdit">取消</el-button>
              </template>
            </div>
          </div>
          <div class="panel-body">
            <template v-if="selectedChunk">
              <h2>{{ selectedChunk.title }}</h2>
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
              <el-tag v-if="selectedChunk?.translation" type="success" size="small">已翻译</el-tag>
              <el-button
                v-if="selectedChunk?.translation && editingField !== 'translation'"
                size="small"
                type="warning"
                :icon="Edit"
                @click="startEditTranslation"
              >
                校对
              </el-button>
              <template v-if="editingField === 'translation'">
                <el-button size="small" type="success" :icon="Check" @click="saveEdit">保存</el-button>
                <el-button size="small" :icon="Close" @click="cancelEdit">取消</el-button>
              </template>
            </div>
          </div>
          <div class="panel-body">
            <template v-if="selectedChunk?.translation">
              <h2>{{ selectedChunk.title }}</h2>
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
              >{{ selectedChunk.translation }}</p>
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
import { ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { UploadFilled, VideoPlay, Download, Edit, Check, Close } from '@element-plus/icons-vue'
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

// ==================== 计算属性 ====================
const totalChunks = computed(() => chunks.value.length)
const completedChunks = computed(() =>
  chunks.value.filter(c => c.status === 2 || c.status === 3).length
)
const progressPercent = computed(() => {
  if (totalChunks.value === 0) return 0
  return Math.round((completedChunks.value / totalChunks.value) * 100)
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
  editText.value = selectedChunk.value?.content || ''
  editingField.value = 'source'
}

function startEditTranslation() {
  editText.value = selectedChunk.value?.translation || ''
  editingField.value = 'translation'
}

function cancelEdit() {
  editingField.value = null
}

// 失焦时自动保存（如果点击的是取消按钮则不保存）
function handleEditBlur() {
  if (!editingField.value) return
  // 延迟检查：如果焦点转移到了取消按钮，cancelEdit 会先清空 editingField
  setTimeout(() => {
    if (editingField.value) {
      saveEdit()
    }
  }, 150)
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
.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fb;
  font-family: "Microsoft YaHei", sans-serif;
}

/* ================= Header ================= */
.header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: #1677ff;
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* ================= Main ================= */
.main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ================= Sidebar ================= */
.sidebar {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
  flex-shrink: 0;
}
.sidebar-header {
  padding: 16px 18px;
  font-weight: 700;
  font-size: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 6px;
}
.sidebar-empty {
  padding: 40px 0;
}
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}
.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ================= Content ================= */
.content {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}
.panel {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.panel-header {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  font-weight: 600;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  line-height: 1.8;
}
.panel-body h2 {
  margin-bottom: 16px;
  color: #1e293b;
}
.panel-text {
  white-space: pre-wrap;
  color: #334155;
}
/* 可点击编辑的文本 */
.clickable-text {
  cursor: pointer;
}

/* ================= 校对编辑（Fix3） ================= */
.edit-textarea {
  width: 100%;
  min-height: 300px;
  height: 100%;
  border: 2px solid #e5a000;
  border-radius: 8px;
  padding: 12px;
  font-size: 15px;
  line-height: 1.8;
  font-family: "Microsoft YaHei", sans-serif;
  color: #334155;
  background: #fffbe6;
  resize: vertical;
  outline: none;
  white-space: pre-wrap;
}
.edit-textarea:focus {
  border-color: #e5a000;
  box-shadow: 0 0 0 3px rgba(229, 160, 0, 0.15);
}
/* 原文编辑框：蓝色边框区分译文编辑 */
.edit-textarea.source-edit {
  border-color: #1677ff;
  background: #f0f5ff;
}
.edit-textarea.source-edit:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.15);
}

/* ================= Config ================= */
.config {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  overflow-y: auto;
  flex-shrink: 0;
}
.config-header {
  padding: 16px 18px;
  font-weight: 700;
  font-size: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 6px;
}
.config-form {
  padding: 16px;
}
.form-group {
  margin-bottom: 18px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #475569;
  font-weight: 500;
}
.form-group .el-select {
  width: 100%;
}
.doc-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}
.chunk-box {
  margin-top: 4px;
}
.chunk-list {
  margin-top: 12px;
  max-height: 320px;
  overflow-y: auto;
}
.chunk-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.chunk-item:last-child {
  border-bottom: none;
}
.chunk-name {
  font-size: 13px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
}

/* ================= Footer ================= */
.footer {
  padding: 14px 24px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.footer-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 14px;
}
</style>
