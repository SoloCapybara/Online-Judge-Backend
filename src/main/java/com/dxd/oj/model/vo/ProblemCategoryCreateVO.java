package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建分类结果")
public class ProblemCategoryCreateVO implements Serializable {

    @Schema(description = "新创建分类ID")
    private Long categoryId;
}
