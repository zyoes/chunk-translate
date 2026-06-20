package com.example.chunktranslate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chunktranslate.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper，对应 sys_user 表。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
