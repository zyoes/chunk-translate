package com.example.chunktranslate.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
//import com.example.common.core.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyBatisPlusHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.setFieldValByName("createdAt", LocalDateTime.now(), metaObject);
//        Long userId = UserContext.get();
//        if (userId != null) {
//            this.setFieldValByName("createdBy", userId, metaObject);
//        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
//        Long userId = UserContext.get();
//        if (userId != null) {
//            this.setFieldValByName("updatedBy", userId, metaObject);
//        }
    }
}