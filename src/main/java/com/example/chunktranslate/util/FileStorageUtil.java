package com.example.chunktranslate.util;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 文件存储工具类。
 * <p>将上传文件按日期分目录存储在本地，文件名为 UUID 保留原始扩展名。
 * 根目录从 {@link com.example.chunktranslate.config.StorageConfig} 读取。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageUtil {

    private final StorageConfig storageConfig;

    /**
     * 存储上传文件
     *
     * @return 文件存储相对路径
     */
    public String store(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            // 生成唯一文件名
            String storedName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 按日期分目录存储
            String datePath = LocalDate.now().toString().replace("-", "/");
            Path dirPath = Paths.get(storageConfig.getBasePath(), datePath);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(storedName);
            file.transferTo(filePath.toFile());

            log.info("文件存储成功: {}", filePath);
            return datePath + "/" + storedName;

        } catch (IOException e) {
            log.error("文件存储失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAIL);
        }
    }

    /**
     * 获取文件完整路径
     */
    public Path getFullPath(String relativePath) {
        return Paths.get(storageConfig.getBasePath(), relativePath);
    }

    /**
     * 删除文件
     */
    public void delete(String relativePath) {
        try {
            Path path = getFullPath(relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", relativePath, e);
        }
    }
}
