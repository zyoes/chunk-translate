import request from './request'

// 启动翻译
export function startTranslation(data) {
  return request.post('/translation/start', data)
}

// 查询翻译进度
export function getProgress(documentId) {
  return request.get(`/translation/progress/${documentId}`)
}

// 中止翻译
export function stopTranslation(documentId) {
  return request.post(`/translation/stop/${documentId}`)
}

// 导出文件（触发浏览器下载）
export function exportFile(documentId, format) {
  window.open(`/api/export/${format}/${documentId}`)
}
