package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "管理员-系统配置")
public class AdminSystemConfigVO implements Serializable {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "值类型")
    private String valueType;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "状态：1启用，0停用")
    private Integer status;

    @Schema(description = "最后更新时间")
    private LocalDateTime updateTime;
}
