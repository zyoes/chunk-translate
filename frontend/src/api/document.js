import request from './request'

// 上传文档
export function uploadDocument(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/document/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取文档详情（含目录树）
export function getDocumentDetail(id) {
  return request.get(`/document/detail/${id}`)
}
