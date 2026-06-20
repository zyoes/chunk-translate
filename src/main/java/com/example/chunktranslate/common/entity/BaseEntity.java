package com.example.chunktranslate.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 实体基类，所有数据库实体均继承此类以复用通用字段。
 * <ul>
 *   <li>{@code id}：自增主键，由数据库自动生成</li>
 *   <li>{@code createdAt / updatedAt}：由 {@link com.example.chunktranslate.config.MyBatisPlusHandler MyBatisPlusHandler} 自动填充</li>
 *   <li>{@code createdBy / updatedBy}：预留字段，当前未自动填充</li>
 *   <li>{@code deleted / deletedAt / deletedBy}：逻辑删除字段，当前未启用（已注释 MyBatis-Plus @TableLogic）</li>
 * </ul>
 */
@Getter
@Setter
public abstract class BaseEntity {
    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    /** 创建时间，插入时由 MyBatisPlusHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 创建人ID（预留，当前未自动填充） */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createdBy;

    /** 更新时间，更新时由 MyBatisPlusHandler 自动填充 */
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 更新人ID（预留，当前未自动填充） */
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新人ID")
    private Long updatedBy;

    /** 逻辑删除标记（预留，当前未启用），0=正常，非0=已删除 */
    private Long deleted;

    /** 逻辑删除时间（预留） */
    private LocalDateTime deletedAt;

    /** 删除操作人ID（预留） */
    private Long deletedBy;
}
