import request from './request'

/**
 * 更新分块译文（校对编辑）
 * @param {number} chunkId 分块ID
 * @param {string} translation 修改后的译文
 */
export function updateChunkTranslation(chunkId, translation) {
  return request.put(`/translation/chunk/${chunkId}`, { translation })
}

/**
 * 更新分块原文（校对编辑）
 * @param {number} chunkId 分块ID
 * @param {string} content 修改后的原文
 */
export function updateChunkSource(chunkId, content) {
  return request.put(`/translation/chunk/${chunkId}/source`, { content })
}
