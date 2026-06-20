package com.example.chunktranslate.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.chunktranslate.security.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器。
 * <p>在插入时自动填充 createdAt，更新时自动填充 updatedAt。
 * createdBy/updatedBy 字段依赖 {@link com.example.chunktranslate.security.UserContext UserContext}，
 * 当前 UserContext 未启用，后续可打开填充。</p>
 */
@Component
public class MyBatisPlusHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.setFieldValByName("createdAt", LocalDateTime.now(), metaObject);
        Long userId = UserContext.getUserId();
        if (userId != null) {
            this.setFieldValByName("createdBy", userId, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
        Long userId = UserContext.getUserId();
        if (userId != null) {
            this.setFieldValByName("updatedBy", userId, metaObject);
        }
    }
}
