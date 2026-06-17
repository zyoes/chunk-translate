package com.example.chunktranslate.service.document;

import com.example.chunktranslate.dto.DocumentDetailResponse;
import com.example.chunktranslate.dto.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理服务接口
 * <p>
 * 提供文档上传、解析、详情查询等核心能力，是 Phase 2 对外暴露的主要服务门面。
 * </p>
 *
 * <p>核心业务流程：</p>
 * <ol>
 *   <li>上传：校验文件类型/大小 → 持久化文件到磁盘 → 写入 document 表 → 异步触发解析</li>
 *   <li>解析：根据文件类型路由到对应解析器 → 将章节树存入 document_chunk 表 → 更新文档状态</li>
 *   <li>查询：从 document + document_chunk 组装返回给前端</li>
 * </ol>
 *
 * @see com.example.chunktranslate.service.document.impl.DocumentServiceImpl
 */
public interface DocumentService {

    /**
     * 上传并解析文档
     * <p>
     * 同步完成文件落盘和 DB 记录创建，解析在后台异步执行。
     * 调用方可通过返回的 documentId 轮询 {@link #getDocumentDetail(Long)} 查看解析进度。
     * </p>
     *
     * @param file 上传的文件
     * @return 上传响应（包含文档 ID 和初始状态「解析中」）
     */
    DocumentUploadResponse uploadAndParse(MultipartFile file);

    /**
     * 获取文档详情（含章节树）
     * <p>
     * 从 document 表取基本信息，从 document_chunk 表取所有章节，
     * 按 sequence 排序后组装为树节点列表返回。
     * </p>
     *
     * @param documentId 文档 ID
     * @return 文档详情响应（含章节树）
     * @throws com.example.chunktranslate.common.exception.BusinessException 文档不存在时抛出
     */
    DocumentDetailResponse getDocumentDetail(Long documentId);
}
