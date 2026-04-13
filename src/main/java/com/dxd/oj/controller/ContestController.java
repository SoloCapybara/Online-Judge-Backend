package com.dxd.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.common.BaseResponse;
import com.dxd.oj.common.ResultUtils;
import com.dxd.oj.model.dto.contest.ContestBindProblemsRequest;
import com.dxd.oj.model.dto.contest.ContestCreateRequest;
import com.dxd.oj.model.dto.contest.ContestPageQueryRequest;
import com.dxd.oj.model.dto.contest.ContestRankPageQueryRequest;
import com.dxd.oj.model.dto.contest.ContestRegisterRequest;
import com.dxd.oj.model.dto.contest.ContestStatusUpdateRequest;
import com.dxd.oj.model.vo.ContestCreateVO;
import com.dxd.oj.model.vo.ContestDetailVO;
import com.dxd.oj.model.vo.ContestRankVO;
import com.dxd.oj.model.vo.ContestVO;
import com.dxd.oj.model.vo.PageVO;
import com.dxd.oj.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "比赛/作业")
@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @Operation(summary = "创建比赛/作业", description = "仅教师/管理员可创建。")
    @PostMapping
    public BaseResponse<ContestCreateVO> createContest(@Valid @RequestBody ContestCreateRequest request,
                                                       HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        ContestCreateVO response = contestService.createContest(request, token);
        return ResultUtils.success(response);
    }

    @Operation(summary = "比赛分页查询", description = "学生仅查看已发布比赛；教师/管理员可查看全部并支持 mine 筛选。")
    @GetMapping("/page")
    public BaseResponse<PageVO<ContestVO>> pageContests(ContestPageQueryRequest queryRequest,
                                                         HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        IPage<ContestVO> pageResult = contestService.pageContests(queryRequest, token);
        return ResultUtils.success(PageVO.of(pageResult));
    }

    @Operation(summary = "比赛详情", description = "返回比赛基础信息、题目列表和参与状态。")
    @GetMapping("/{id}")
    public BaseResponse<ContestDetailVO> getContestDetail(@Parameter(description = "比赛ID", required = true)
                                                          @PathVariable("id") Long id,
                                                          HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        ContestDetailVO detail = contestService.getContestDetail(id, token);
        return ResultUtils.success(detail);
    }

    @Operation(summary = "绑定比赛题目", description = "仅比赛创建者(教师)或管理员可操作，提交列表将覆盖原题目集。")
    @PostMapping("/{id}/problems")
    public BaseResponse<Boolean> bindContestProblems(@Parameter(description = "比赛ID", required = true)
                                                     @PathVariable("id") Long id,
                                                     @Valid @RequestBody ContestBindProblemsRequest request,
                                                     HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = contestService.bindProblems(id, request, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "报名比赛", description = "当前用户报名参与比赛。私有比赛需提供访问密码。")
    @PostMapping("/{id}/register")
    public BaseResponse<Boolean> registerContest(@Parameter(description = "比赛ID", required = true)
                                                 @PathVariable("id") Long id,
                                                 @RequestBody(required = false) ContestRegisterRequest request,
                                                 HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = contestService.registerContest(id, request, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "更新比赛状态", description = "仅比赛创建者(教师)或管理员可操作，支持未发布/已发布/已关闭。")
    @PutMapping("/{id}/status")
    public BaseResponse<Boolean> updateContestStatus(@Parameter(description = "比赛ID", required = true)
                                                     @PathVariable("id") Long id,
                                                     @Valid @RequestBody ContestStatusUpdateRequest request,
                                                     HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = contestService.updateContestStatus(id, request, token);
        return ResultUtils.success(result);
    }

    @Operation(summary = "比赛榜单分页", description = "按榜单规则返回排名、通过题数、总分和罚时。")
    @GetMapping("/{id}/rank")
    public BaseResponse<PageVO<ContestRankVO>> pageContestRank(@Parameter(description = "比赛ID", required = true)
                                                               @PathVariable("id") Long id,
                                                               ContestRankPageQueryRequest queryRequest,
                                                               HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        IPage<ContestRankVO> pageResult = contestService.pageContestRank(id, queryRequest, token);
        return ResultUtils.success(PageVO.of(pageResult));
    }

    @Operation(summary = "删除比赛/作业", description = "仅比赛创建者(教师)或管理员可操作。删除后将级联清理题目绑定、报名、提交记录与榜单缓存。")
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteContest(@Parameter(description = "比赛ID", required = true)
                                               @PathVariable("id") Long id,
                                               HttpServletRequest httpServletRequest) {
        String token = extractToken(httpServletRequest);
        boolean result = contestService.deleteContest(id, token);
        return ResultUtils.success(result);
    }

    @Parameter(hidden = true)
    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }
}
