package com.example.chunktranslate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chunktranslate.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 刷新令牌表 Mapper，对应 refresh_token 表。
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
}
