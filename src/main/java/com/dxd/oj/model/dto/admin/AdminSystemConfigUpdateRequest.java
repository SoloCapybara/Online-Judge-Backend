package com.dxd.oj.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理员-系统配置更新请求")
public class AdminSystemConfigUpdateRequest {

    @Size(max = 500, message = "配置值长度不能超过500")
    @Schema(description = "配置值", example = "2000")
    private String configValue;

    @Size(max = 255, message = "配置说明长度不能超过255")
    @Schema(description = "配置说明")
    private String description;

    @Min(value = 0, message = "状态参数错误")
    @Max(value = 1, message = "状态参数错误")
    @Schema(description = "状态：1启用，0停用", example = "1")
    private Integer status;
}
