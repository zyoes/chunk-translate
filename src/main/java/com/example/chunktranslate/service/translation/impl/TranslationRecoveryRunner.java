package com.example.chunktranslate.service.translation.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.chunktranslate.common.enums.ChunkStatus;
import com.example.chunktranslate.entity.DocumentChunk;
import com.example.chunktranslate.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时回滚卡住的翻译任务
 *
 * @author yezhiqiu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationRecoveryRunner implements ApplicationRunner {

    private final DocumentChunkMapper documentChunkMapper;

    /**
     * 启动时回滚卡住的翻译任务
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        // 回滚卡住的翻译任务
        LambdaUpdateWrapper<DocumentChunk> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DocumentChunk::getStatus, ChunkStatus.TRANSLATING.getCode())
                .set(DocumentChunk::getStatus, ChunkStatus.PENDING.getCode());

        // 更新状态为待翻译
        int updateCount = documentChunkMapper.update(null, wrapper);
        if (updateCount > 0) {
            log.info("启动时回滚卡住的翻译任务: 共 {} 个 chunk 已恢复为待翻译", updateCount);
        }
    }
}
