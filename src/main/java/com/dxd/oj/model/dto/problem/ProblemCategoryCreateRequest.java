package com.dxd.oj.model.dto.problem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建题目分类请求")
public class ProblemCategoryCreateRequest {

    @Schema(description = "分类名称（全局唯一）", example = "哈希表")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100位")
    private String categoryName;

    @Schema(description = "父分类ID，顶级分类可不传")
    private Long parentId;

    @Schema(description = "排序号（越小越靠前）", example = "10")
    private Integer sortNo;

    @Schema(description = "状态：1=启用，0=停用", example = "1")
    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 1, message = "状态取值不合法")
    private Integer status;
}
