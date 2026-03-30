package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "题目信息")
public class ProblemVO implements Serializable {

    @Schema(description = "题目ID")
    private Long id;

    @Schema(description = "题目标题")
    private String title;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "题目描述")
    private String content;

    @Schema(description = "输入描述")
    private String inputDesc;

    @Schema(description = "输出描述")
    private String outputDesc;

    @Schema(description = "样例输入")
    private String sampleInput;

    @Schema(description = "样例输出")
    private String sampleOutput;

    @Schema(description = "提示信息")
    private String hint;

    @Schema(description = "难度：1=简单，2=中等，3=困难")
    private Integer difficulty;

    @Schema(description = "时间限制（毫秒）")
    private Integer timeLimit;

    @Schema(description = "内存限制（MB）")
    private Integer memoryLimit;

    @Schema(description = "判题模式：ACM/OI")
    private String judgeMode;

    @Schema(description = "提交次数")
    private Integer submitCount;

    @Schema(description = "通过次数")
    private Integer acceptCount;

    @Schema(description = "通过率（百分比，保留2位小数）")
    private Double acceptRate;

    @Schema(description = "状态：0=草稿，1=发布，2=下线")
    private Integer status;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人用户名")
    private String creatorUsername;

    @Schema(description = "创建人真实姓名")
    private String creatorRealName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
