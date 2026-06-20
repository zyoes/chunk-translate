package com.example.chunktranslate.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.TranslationTask;
import com.example.chunktranslate.entity.User;
import com.example.chunktranslate.mapper.DocumentMapper;
import com.example.chunktranslate.mapper.TranslationTaskMapper;
import com.example.chunktranslate.mapper.UserMapper;
import com.example.chunktranslate.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final DocumentMapper documentMapper;
    private final TranslationTaskMapper translationTaskMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("documentCount", documentMapper.selectCount(null));
        data.put("taskCount", translationTaskMapper.selectCount(null));
        data.put("completedTaskCount", translationTaskMapper.selectCount(
                new LambdaQueryWrapper<TranslationTask>().eq(TranslationTask::getStatus, 1)));
        return data;
    }

    @Override
    public List<User> listUsers() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("管理员更新用户状态: userId={}, username={}, status={}",
                userId, user.getUsername(), status == 1 ? "启用" : "禁用");
    }

    @Override
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.warn("管理员重置用户密码: userId={}, username={}", userId, user.getUsername());
    }

    @Override
    public List<Document> listDocuments() {
        List<Document> docs = documentMapper.selectList(
                new LambdaQueryWrapper<Document>().orderByDesc(Document::getCreatedAt));
        // 关联查询上传者用户名
        Map<Long, String> userNames = new HashMap<>();
        for (Document doc : docs) {
            if (doc.getCreatedBy() != null) {
                userNames.put(doc.getCreatedBy(), null);
            }
        }
        if (!userNames.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userNames.keySet());
            for (User u : users) {
                userNames.put(u.getId(), u.getUsername());
            }
            for (Document doc : docs) {
                doc.setUploaderName(userNames.getOrDefault(doc.getCreatedBy(), "—"));
            }
        }
        return docs;
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        String fileName = doc.getFileName();
        documentMapper.deleteById(documentId);
        log.info("管理员删除文档: documentId={}, fileName={}", documentId, fileName);
    }

    @Override
    public List<TranslationTask> listTasks() {
        List<TranslationTask> tasks = translationTaskMapper.selectList(
                new LambdaQueryWrapper<TranslationTask>().orderByDesc(TranslationTask::getCreatedAt));
        if (tasks.isEmpty()) {
            return tasks;
        }
        // 收集所有涉及的文档 ID 和用户 ID
        Set<Long> docIds = new HashSet<>();
        for (TranslationTask t : tasks) {
            docIds.add(t.getDocumentId());
        }
        // 批量加载文档 → 得文档名 + createdBy
        Map<Long, Document> docMap = new HashMap<>();
        List<Document> docs = documentMapper.selectByIdsIncludeDeleted(docIds);
        Set<Long> userIds = new HashSet<>();
        for (Document d : docs) {
            docMap.put(d.getId(), d);
            if (d.getCreatedBy() != null) {
                userIds.add(d.getCreatedBy());
            }
        }
        // 批量加载用户 → 得用户名
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                userNameMap.put(u.getId(), u.getUsername());
            }
        }
        // 填充展示字段
        for (TranslationTask t : tasks) {
            Document d = docMap.get(t.getDocumentId());
            if (d == null) {
                t.setDocumentName("—");
                t.setUploaderName("—");
            } else {
                boolean deleted = d.getDeleted() != null && d.getDeleted() != 0;
                t.setDocumentName(deleted ? d.getFileName() + " (已删除)" : d.getFileName());
                t.setUploaderName(d.getCreatedBy() != null
                        ? userNameMap.getOrDefault(d.getCreatedBy(), "—") : "—");
            }
        }
        return tasks;
    }

    @Override
    public void deleteTask(Long taskId) {
        TranslationTask task = translationTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        translationTaskMapper.deleteById(taskId);
        log.info("管理员删除翻译任务: taskId={}, documentId={}", taskId, task.getDocumentId());
    }
}
