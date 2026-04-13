package com.dxd.oj.model.dto.contest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "绑定比赛题目请求")
public class ContestBindProblemsRequest {

    @Schema(description = "题目列表")
    @Valid
    @NotEmpty(message = "题目列表不能为空")
    private List<ContestProblemBindItemDTO> problemList;
}
