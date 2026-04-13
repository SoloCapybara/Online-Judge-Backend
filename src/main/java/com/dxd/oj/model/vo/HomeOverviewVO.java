package com.dxd.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "首页概览数据")
public class HomeOverviewVO implements Serializable {

    @Schema(description = "题目总数")
    private Long problemCount;

    @Schema(description = "今日提交数")
    private Long todaySubmissionCount;

    @Schema(description = "评测队列任务数（WAITING + JUDGING）")
    private Long queueTaskCount;

    @Schema(description = "全站 AC 率（%）")
    private Double globalAcRate;

    @Schema(description = "最近比赛列表")
    private List<HomeRecentContestVO> recentContests;

    @Schema(description = "最近提交列表")
    private List<HomeRecentSubmissionVO> recentSubmissions;

    @Schema(description = "服务端时间")
    private LocalDateTime serverTime;
}
