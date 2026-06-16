package com.example.chunktranslate.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEntity {
    // 1. @TableId 注解：该注解用于标记实体类中的主键字段。如果主键字段名为 id，可以省略这个注解
    //   - https://baomidou.com/reference/annotation/#tableid
    // 2. @TableId 注解的 type 属性：指定主键的生成策略。默认使用雪花算法通过 IdentifierGenerator 的 nextId 实现
    //   - https://baomidou.com/reference/annotation/#type
    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createdBy;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新人ID")
    private Long updatedBy;

    // 软删除相关字段，暂时先不做
    //@TableLogic
    //@Schema(description = "逻辑删除标记 (0:未删除, id:已删除)")
    private Long deleted;

    //@TableField(fill = FieldFill.UPDATE)
    //@Schema(description = "逻辑删除时间")
    private LocalDateTime deletedAt;

    //@TableField(fill = FieldFill.UPDATE)
    //@Schema(description = "创建人ID")
    private Long deletedBy;
}
