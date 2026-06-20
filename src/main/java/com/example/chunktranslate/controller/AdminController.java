package com.example.chunktranslate.controller;

import com.example.chunktranslate.common.result.Result;
import com.example.chunktranslate.dto.admin.ResetUserPasswordRequest;
import com.example.chunktranslate.dto.admin.UpdateUserStatusRequest;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.TranslationTask;
import com.example.chunktranslate.entity.User;
import com.example.chunktranslate.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器，仅管理员可访问（SecurityConfig 中配置权限）。
 */
@Tag(name = "后台管理", description = "管理员统计与用户管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 系统统计概览，返回用户数、文档数、任务数、已完成任务数。
     */
    @GetMapping("/stats")
    @Operation(summary = "系统统计概览")
    public Result<Map<String, Object>> stats() {
        return Result.success(adminService.getStats());
    }

    /**
     * 获取所有用户列表，密码已脱敏。
     */
    @GetMapping("/users")
    @Operation(summary = "用户列表（密码已脱敏）")
    public Result<List<User>> users() {
        return Result.success(adminService.listUsers());
    }

    /**
     * 启用或禁用指定用户。
     *
     * @param id   用户 ID
     * @param body status=1 启用，status=0 禁用
     */
    @PutMapping("/users/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<?> updateUserStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateUserStatusRequest body) {
        adminService.updateUserStatus(id, body.getStatus());
        return Result.success();
    }

    /**
     * 管理员强制重置用户密码（无需旧密码）。
     *
     * @param id   用户 ID
     * @param body 新密码（BCrypt 加密后存储）
     */
    @PutMapping("/users/{id}/reset-password")
    @Operation(summary = "管理员重置用户密码（无需旧密码）")
    public Result<?> resetUserPassword(@PathVariable Long id,
                                       @Valid @RequestBody ResetUserPasswordRequest body) {
        adminService.resetUserPassword(id, body.getPassword());
        return Result.success();
    }

    /**
     * 获取所有文档列表，含上传者用户名。
     */
    @GetMapping("/documents")
    @Operation(summary = "文档列表（含上传者）")
    public Result<List<Document>> documents() {
        return Result.success(adminService.listDocuments());
    }

    /**
     * 删除指定文档（软删除）。
     *
     * @param id 文档 ID
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除文档（软删除）")
    public Result<?> deleteDocument(@PathVariable Long id) {
        adminService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 获取所有翻译任务列表，含关联的文档名和上传者用户名。
     */
    @GetMapping("/tasks")
    @Operation(summary = "翻译任务列表（含文档名和上传者）")
    public Result<List<TranslationTask>> tasks() {
        return Result.success(adminService.listTasks());
    }

    /**
     * 删除指定翻译任务（不影响文档和分块数据）。
     *
     * @param id 任务 ID
     */
    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除翻译任务（不影响文档）")
    public Result<?> deleteTask(@PathVariable Long id) {
        adminService.deleteTask(id);
        return Result.success();
    }
}
