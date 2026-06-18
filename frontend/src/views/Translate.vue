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
          type="success"
          :icon="VideoPlay"
          @click="handleStartTranslation"
          :disabled="!documentId || isTranslating"
          :loading="isTranslating"
        >
          {{ isTranslating ? '翻译中...' : '开始翻译' }}
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
        <!-- Left Panel: Source -->
        <div class="panel">
          <div class="panel-header">
            <span>📖 原文内容</span>
            <el-tag v-if="selectedChunk" type="info" size="small">
              {{ selectedChunk.title }}
            </el-tag>
          </div>
          <div class="panel-body">
            <template v-if="selectedChunk">
              <h2>{{ selectedChunk.title }}</h2>
              <p class="panel-text">{{ selectedChunk.content || '暂无内容' }}</p>
            </template>
            <el-empty v-else description="请从左侧目录选择章节" />
          </div>
        </div>

        <!-- Right Panel: Translation -->
        <div class="panel">
          <div class="panel-header">
            <span>🌍 翻译结果</span>
            <el-tag v-if="selectedChunk?.translation" type="success" size="small">
              已翻译
            </el-tag>
          </div>
          <div class="panel-body">
            <template v-if="selectedChunk?.translation">
              <h2>{{ selectedChunk.title }}</h2>
              <p class="panel-text">{{ selectedChunk.translation }}</p>
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
import { UploadFilled, VideoPlay, Download } from '@element-plus/icons-vue'
import { uploadDocument, getDocumentDetail } from '../api/document'
import { startTranslation, getProgress, exportFile } from '../api/translation'

// ==================== 状态 ====================
const documentId = ref(null)
const fileName = ref('')
const sourceLang = ref('en')
const targetLang = ref('zh')
const isTranslating = ref(false)
const treeData = ref([])
const chunks = ref([])
const selectedChunk = ref(null)
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

// ==================== 上传 ====================
async function handleFileChange(file) {
  const loading = ElLoading.service({ text: '上传中...' })
  try {
    const res = await uploadDocument(file.raw)
    const data = res.data
    documentId.value = data.id
    fileName.value = data.fileName
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

function startPolling() {
  stopPolling()
  progressTimer = setInterval(async () => {
    try {
      const res = await getProgress(documentId.value)
      const data = res.data
      chunks.value = data.chunks || []
      updateTreeStatus(data.chunks)
      if (data.progressPercent >= 100) {
        isTranslating.value = false
        stopPolling()
        ElMessage.success('翻译完成！')
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

// ==================== 目录树 ====================
function handleNodeClick(data) {
  const chunk = chunks.value.find(c => c.title === data.title)
  selectedChunk.value = {
    title: data.title,
    content: data.content,
    translation: chunk?.translation || null,
    status: chunk?.status
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
