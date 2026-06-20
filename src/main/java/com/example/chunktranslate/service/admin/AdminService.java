package com.example.chunktranslate.service.admin;

import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.TranslationTask;
import com.example.chunktranslate.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 后台管理服务接口，提供系统统计、用户管理、文档管理、任务管理等功能。
 */
public interface AdminService {

    /** 系统统计概览 */
    Map<String, Object> getStats();

    /** 用户列表（密码已脱敏） */
    List<User> listUsers();

    /** 启用/禁用用户 */
    void updateUserStatus(Long userId, Integer status);

    /** 重置用户密码（BCrypt 加密后写入） */
    void resetUserPassword(Long userId, String newPassword);

    /** 文档列表 */
    List<Document> listDocuments();

    /** 删除文档 */
    void deleteDocument(Long documentId);

    /** 翻译任务列表 */
    List<TranslationTask> listTasks();

    /** 删除翻译任务 */
    void deleteTask(Long taskId);
}
