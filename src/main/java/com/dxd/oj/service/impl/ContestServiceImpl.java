package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.ContestMapper;
import com.dxd.oj.mapper.ContestProblemMapper;
import com.dxd.oj.mapper.ContestUserMapper;
import com.dxd.oj.mapper.ProblemMapper;
import com.dxd.oj.mapper.SubmissionMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.model.dto.contest.ContestBindProblemsRequest;
import com.dxd.oj.model.dto.contest.ContestCreateRequest;
import com.dxd.oj.model.dto.contest.ContestPageQueryRequest;
import com.dxd.oj.model.dto.contest.ContestProblemBindItemDTO;
import com.dxd.oj.model.dto.contest.ContestRankPageQueryRequest;
import com.dxd.oj.model.dto.contest.ContestRegisterRequest;
import com.dxd.oj.model.dto.contest.ContestStatusUpdateRequest;
import com.dxd.oj.model.entity.Contest;
import com.dxd.oj.model.entity.ContestProblem;
import com.dxd.oj.model.entity.ContestUser;
import com.dxd.oj.model.entity.Problem;
import com.dxd.oj.model.entity.Submission;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.vo.ContestCreateVO;
import com.dxd.oj.model.vo.ContestDetailVO;
import com.dxd.oj.model.vo.ContestProblemVO;
import com.dxd.oj.model.vo.ContestRankVO;
import com.dxd.oj.model.vo.ContestVO;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.ContestRankService;
import com.dxd.oj.service.ContestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestServiceImpl implements ContestService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_ENDED = 2;
    private static final int STATUS_CLOSED = 3;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";

    private static final String TYPE_CONTEST = "CONTEST";
    private static final String TYPE_HOMEWORK = "HOMEWORK";

    private static final String ACCESS_PUBLIC = "PUBLIC";
    private static final String ACCESS_PRIVATE = "PRIVATE";

    private static final String RESULT_VISIBLE_REALTIME = "REALTIME";
    private static final String RESULT_VISIBLE_AFTER_END = "AFTER_END";

    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestUserMapper contestUserMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final SysUserMapper sysUserMapper;
    private final AuthService authService;
    private final ContestRankService contestRankService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContestCreateVO createContest(ContestCreateRequest request, String token) {
        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);

        Contest contest = new Contest();
        contest.setTitle(trimText(request.getTitle()));
        contest.setDescription(trimText(request.getDescription()));
        contest.setType(normalizeType(request.getType()));
        contest.setAccessType(normalizeAccessType(request.getAccessType()));
        contest.setAccessPassword(trimText(request.getAccessPassword()));
        contest.setSubmitLimit(request.getSubmitLimit());
        contest.setResultVisibleMode(normalizeResultVisibleMode(request.getResultVisibleMode()));
        contest.setStartTime(request.getStartTime());
        contest.setEndTime(request.getEndTime());
        contest.setStatus(resolveCreateStatus(request.getStatus()));
        contest.setCreatorId(loginUser.getId());

        validateContest(contest);

        int inserted = contestMapper.insert(contest);
        if (inserted <= 0 || contest.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建比赛失败");
        }
        log.info("比赛模块-创建比赛成功: contestId={}, operator={}", contest.getId(), loginUser.getUsername());

        ContestCreateVO response = new ContestCreateVO();
        response.setContestId(contest.getId());
        return response;
    }

    @Override
    public IPage<ContestVO> pageContests(ContestPageQueryRequest queryRequest, String token) {
        LoginUserVO loginUser = getLoginUser(token);
        ContestPageQueryRequest safeQuery = queryRequest == null ? new ContestPageQueryRequest() : queryRequest;
        long current = safeQuery.getCurrent() <= 0 ? 1 : safeQuery.getCurrent();
        long pageSize = safeQuery.getPageSize() <= 0 ? 10 : Math.min(safeQuery.getPageSize(), 100);
        boolean manager = isManager(loginUser);

        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<>();
        String title = trimText(safeQuery.getTitle());
        wrapper.like(StringUtils.hasText(title), Contest::getTitle, title)
                .eq(StringUtils.hasText(safeQuery.getType()), Contest::getType, normalizeTypeNullable(safeQuery.getType()));

        if (manager) {
            wrapper.eq(safeQuery.getStatus() != null, Contest::getStatus, safeQuery.getStatus())
                    .eq(Boolean.TRUE.equals(safeQuery.getMine()), Contest::getCreatorId, loginUser.getId());
        } else {
            wrapper.in(Contest::getStatus, STATUS_PUBLISHED, STATUS_ENDED);
        }

        wrapper.orderByDesc(Contest::getStartTime, Contest::getId);

        Page<Contest> page = contestMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Page<ContestVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ContestVO> records = page.getRecords().stream().map(this::toContestVO).toList();
        fillContestExtraInfo(records, loginUser.getId());
        result.setRecords(records);
        return result;
    }

    @Override
    public ContestDetailVO getContestDetail(Long contestId, String token) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        boolean manager = isManager(loginUser);

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        }
        if (!manager && !isContestVisibleForStudent(contest)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该比赛");
        }

        ContestDetailVO detail = toContestDetailVO(contest);
        fillContestDetailExtraInfo(detail, loginUser.getId());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindProblems(Long contestId, ContestBindProblemsRequest request, String token) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛ID不合法");
        }
        if (request == null || CollectionUtils.isEmpty(request.getProblemList())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目列表不能为空");
        }

        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        }
        checkCreatorOrAdmin(loginUser, contest.getCreatorId());
        if (!Objects.equals(contest.getStatus(), STATUS_DRAFT)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅未发布(草稿)状态可修改题目集");
        }

        Set<Long> deduplicateProblemIds = new HashSet<>();
        for (ContestProblemBindItemDTO item : request.getProblemList()) {
            if (item == null || item.getProblemId() == null || item.getProblemId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不合法");
            }
            if (!deduplicateProblemIds.add(item.getProblemId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目列表包含重复题目");
            }
            Problem problem = problemMapper.selectById(item.getProblemId());
            if (problem == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目不存在: " + item.getProblemId());
            }
        }

        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));

        int displayOrder = 1;
        for (ContestProblemBindItemDTO item : request.getProblemList()) {
            ContestProblem contestProblem = new ContestProblem();
            contestProblem.setContestId(contestId);
            contestProblem.setProblemId(item.getProblemId());
            contestProblem.setDisplayOrder(item.getDisplayOrder() == null ? displayOrder : item.getDisplayOrder());
            contestProblem.setScore(item.getScore() == null ? 100 : Math.max(0, item.getScore()));
            contestProblemMapper.insert(contestProblem);
            displayOrder++;
        }

        contestRankService.rebuildRankCache(contestId);
        log.info("比赛模块-绑定题目成功: contestId={}, operator={}", contestId, loginUser.getUsername());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerContest(Long contestId, ContestRegisterRequest request, String token) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        }

        if (!isManager(loginUser)) {
            if (!isContestVisibleForStudent(contest)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前比赛不可参与");
            }
            LocalDateTime now = LocalDateTime.now();
            if (contest.getEndTime() != null && now.isAfter(contest.getEndTime())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "比赛已结束，无法报名");
            }
        }

        if (ACCESS_PRIVATE.equalsIgnoreCase(contest.getAccessType())) {
            String inputPassword = request == null ? null : trimText(request.getAccessPassword());
            String storedPassword = trimText(contest.getAccessPassword());
            if (!StringUtils.hasText(storedPassword) || !Objects.equals(storedPassword, inputPassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛访问密码错误");
            }
        }

        ContestUser exists = contestUserMapper.selectOne(new LambdaQueryWrapper<ContestUser>()
                .eq(ContestUser::getContestId, contestId)
                .eq(ContestUser::getUserId, loginUser.getId())
                .last("limit 1"));
        if (exists != null) {
            return true;
        }

        ContestUser contestUser = new ContestUser();
        contestUser.setContestId(contestId);
        contestUser.setUserId(loginUser.getId());
        contestUser.setJoinTime(LocalDateTime.now());
        contestUser.setTotalScore(0);
        contestUser.setAcCount(0);
        contestUserMapper.insert(contestUser);

        contestRankService.refreshUserRank(contestId, loginUser.getId());
        log.info("比赛模块-报名成功: contestId={}, userId={}", contestId, loginUser.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContestStatus(Long contestId, ContestStatusUpdateRequest request, String token) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛ID不合法");
        }
        if (request == null || request.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不能为空");
        }

        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        }
        checkCreatorOrAdmin(loginUser, contest.getCreatorId());

        Integer targetStatus = request.getStatus();
        if (targetStatus != STATUS_DRAFT && targetStatus != STATUS_PUBLISHED && targetStatus != STATUS_CLOSED) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持更新为未发布、已发布或已关闭");
        }

        if (Objects.equals(contest.getStatus(), targetStatus)) {
            return true;
        }

        long problemCountBefore = queryProblemCount(contestId);
        if (targetStatus == STATUS_PUBLISHED) {
            if (problemCountBefore <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "发布前请先绑定题目集");
            }
        }

        Integer fromStatus = contest.getStatus();
        contest.setStatus(targetStatus);
        int updated = contestMapper.updateById(contest);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新比赛状态失败");
        }
        long problemCountAfter = queryProblemCount(contestId);
        if (problemCountAfter != problemCountBefore) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "状态更新异常：题目集发生变化，请重试");
        }
        log.info("比赛模块-状态更新成功: contestId={}, fromStatus={}, toStatus={}, operator={}",
                contestId, fromStatus, targetStatus, loginUser.getUsername());
        return true;
    }

    @Override
    public IPage<ContestRankVO> pageContestRank(Long contestId, ContestRankPageQueryRequest queryRequest, String token) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛ID不合法");
        }

        LoginUserVO loginUser = getLoginUser(token);
        boolean manager = isManager(loginUser);
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        }
        if (!manager && !isContestVisibleForStudent(contest)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该比赛榜单");
        }

        ContestRankPageQueryRequest safeQuery = queryRequest == null ? new ContestRankPageQueryRequest() : queryRequest;
        long current = safeQuery.getCurrent() <= 0 ? 1 : safeQuery.getCurrent();
        long pageSize = safeQuery.getPageSize() <= 0 ? 20 : Math.min(safeQuery.getPageSize(), 100);
        return contestRankService.pageRank(contestId, current, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteContest(Long contestId, String token) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛ID不合法");
        }

        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);

        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        }
        checkCreatorOrAdmin(loginUser, contest.getCreatorId());

        Map<Long, ProblemSubmissionStat> stats = queryContestSubmissionStats(contestId);

        jdbcTemplate.update(
                "delete sc from submission_case sc inner join submission s on sc.submission_id = s.id where s.contest_id = ?",
                contestId
        );
        submissionMapper.delete(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getContestId, contestId));
        contestUserMapper.delete(new LambdaQueryWrapper<ContestUser>()
                .eq(ContestUser::getContestId, contestId));
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));

        int deleted = contestMapper.deleteById(contestId);
        if (deleted <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除比赛失败");
        }

        decreaseProblemStats(stats);
        contestRankService.rebuildRankCache(contestId);
        log.info("比赛模块-删除比赛成功: contestId={}, operator={}", contestId, loginUser.getUsername());
        return true;
    }

    private ContestVO toContestVO(Contest contest) {
        ContestVO vo = new ContestVO();
        vo.setId(contest.getId());
        vo.setTitle(contest.getTitle());
        vo.setDescription(contest.getDescription());
        vo.setType(contest.getType());
        vo.setStatus(contest.getStatus());
        vo.setStatusText(resolveStatusText(contest));
        vo.setStartTime(contest.getStartTime());
        vo.setEndTime(contest.getEndTime());
        vo.setCreatorId(contest.getCreatorId());
        vo.setParticipantCount(0);
        vo.setProblemCount(0);
        vo.setJoined(false);
        return vo;
    }

    private ContestDetailVO toContestDetailVO(Contest contest) {
        ContestDetailVO vo = new ContestDetailVO();
        vo.setId(contest.getId());
        vo.setTitle(contest.getTitle());
        vo.setDescription(contest.getDescription());
        vo.setType(contest.getType());
        vo.setAccessType(contest.getAccessType());
        vo.setResultVisibleMode(contest.getResultVisibleMode());
        vo.setSubmitLimit(contest.getSubmitLimit());
        vo.setStatus(contest.getStatus());
        vo.setStatusText(resolveStatusText(contest));
        vo.setStartTime(contest.getStartTime());
        vo.setEndTime(contest.getEndTime());
        vo.setCreatorId(contest.getCreatorId());
        vo.setParticipantCount(0);
        vo.setProblemCount(0);
        vo.setJoined(false);
        vo.setProblemList(List.of());
        return vo;
    }

    private void fillContestExtraInfo(List<ContestVO> contestList, Long currentUserId) {
        if (CollectionUtils.isEmpty(contestList)) {
            return;
        }

        List<Long> creatorIds = contestList.stream()
                .map(ContestVO::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, SysUser> creatorMap = creatorIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, item -> item, (a, b) -> a));

        for (ContestVO contestVO : contestList) {
            Long contestId = contestVO.getId();
            if (contestId == null) {
                continue;
            }
            if (contestVO.getCreatorId() != null) {
                SysUser creator = creatorMap.get(contestVO.getCreatorId());
                if (creator != null) {
                    contestVO.setCreatorUsername(creator.getUsername());
                    contestVO.setCreatorRealName(creator.getRealName());
                }
            }

            long participantCount = queryCount(new LambdaQueryWrapper<ContestUser>()
                    .eq(ContestUser::getContestId, contestId));
            long problemCount = queryProblemCount(contestId);
            boolean joined = currentUserId != null && currentUserId > 0
                    && queryCount(new LambdaQueryWrapper<ContestUser>()
                    .eq(ContestUser::getContestId, contestId)
                    .eq(ContestUser::getUserId, currentUserId)) > 0;

            contestVO.setParticipantCount((int) participantCount);
            contestVO.setProblemCount((int) problemCount);
            contestVO.setJoined(joined);
        }
    }

    private void fillContestDetailExtraInfo(ContestDetailVO detail, Long currentUserId) {
        if (detail == null || detail.getId() == null) {
            return;
        }

        if (detail.getCreatorId() != null) {
            SysUser creator = sysUserMapper.selectById(detail.getCreatorId());
            if (creator != null) {
                detail.setCreatorUsername(creator.getUsername());
                detail.setCreatorRealName(creator.getRealName());
            }
        }

        long participantCount = queryCount(new LambdaQueryWrapper<ContestUser>()
                .eq(ContestUser::getContestId, detail.getId()));
        long problemCount = queryProblemCount(detail.getId());
        boolean joined = currentUserId != null && currentUserId > 0
                && queryCount(new LambdaQueryWrapper<ContestUser>()
                .eq(ContestUser::getContestId, detail.getId())
                .eq(ContestUser::getUserId, currentUserId)) > 0;

        detail.setParticipantCount((int) participantCount);
        detail.setProblemCount((int) problemCount);
        detail.setJoined(joined);

        List<ContestProblem> contestProblems = contestProblemMapper.selectList(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, detail.getId())
                .orderByAsc(ContestProblem::getDisplayOrder, ContestProblem::getId));
        if (CollectionUtils.isEmpty(contestProblems)) {
            detail.setProblemList(List.of());
            return;
        }

        List<Long> problemIds = contestProblems.stream().map(ContestProblem::getProblemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Problem> problemMap = problemIds.isEmpty() ? Map.of()
                : problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, item -> item, (a, b) -> a));

        List<ContestProblemVO> problemVOList = new ArrayList<>();
        for (ContestProblem contestProblem : contestProblems) {
            ContestProblemVO problemVO = new ContestProblemVO();
            problemVO.setProblemId(contestProblem.getProblemId());
            problemVO.setDisplayOrder(contestProblem.getDisplayOrder());
            problemVO.setScore(contestProblem.getScore());
            Problem problem = problemMap.get(contestProblem.getProblemId());
            if (problem != null) {
                problemVO.setProblemTitle(problem.getTitle());
            }
            problemVOList.add(problemVO);
        }
        detail.setProblemList(problemVOList);
    }

    private long queryProblemCount(Long contestId) {
        Long count = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
        return count == null ? 0L : count;
    }

    private long queryCount(LambdaQueryWrapper<ContestUser> wrapper) {
        Long count = contestUserMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    private Map<Long, ProblemSubmissionStat> queryContestSubmissionStats(Long contestId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        select
                            problem_id as problemId,
                            count(1) as submitCount,
                            sum(case when status = 'ACCEPTED' then 1 else 0 end) as acceptCount
                        from submission
                        where contest_id = ?
                        group by problem_id
                        """,
                contestId
        );
        if (CollectionUtils.isEmpty(rows)) {
            return Map.of();
        }

        Map<Long, ProblemSubmissionStat> result = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long problemId = toLong(row.get("problemId"));
            if (problemId == null || problemId <= 0) {
                continue;
            }
            long submitCount = toLongValue(row.get("submitCount"));
            long acceptCount = toLongValue(row.get("acceptCount"));
            result.put(problemId, new ProblemSubmissionStat(submitCount, acceptCount));
        }
        return result;
    }

    private void decreaseProblemStats(Map<Long, ProblemSubmissionStat> stats) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        stats.forEach((problemId, stat) -> {
            if (problemId == null || problemId <= 0 || stat == null) {
                return;
            }
            jdbcTemplate.update(
                    "update problem set submit_count = greatest(ifnull(submit_count, 0) - ?, 0), accept_count = greatest(ifnull(accept_count, 0) - ?, 0) where id = ?",
                    Math.max(0L, stat.submitCount()),
                    Math.max(0L, stat.acceptCount()),
                    problemId
            );
        });
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private long toLongValue(Object value) {
        Long parsed = toLong(value);
        return parsed == null ? 0L : Math.max(0L, parsed);
    }

    private String resolveStatusText(Contest contest) {
        Integer status = contest.getStatus();
        if (status == null) {
            return "未知";
        }
        if (status == STATUS_DRAFT) {
            return "未发布";
        }
        if (status == STATUS_CLOSED) {
            return "已关闭";
        }

        LocalDateTime now = LocalDateTime.now();
        if (contest.getStartTime() != null && now.isBefore(contest.getStartTime())) {
            return "未开始";
        }
        if (contest.getEndTime() != null && now.isAfter(contest.getEndTime())) {
            return "已结束";
        }
        return "进行中";
    }

    private Integer resolveCreateStatus(Integer status) {
        if (status == null) {
            return STATUS_DRAFT;
        }
        if (status != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建时仅支持未发布状态，请先绑定题目后再发布");
        }
        return status;
    }

    private void validateContest(Contest contest) {
        if (!StringUtils.hasText(contest.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        if (!StringUtils.hasText(contest.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛类型不能为空");
        }
        if (contest.getStartTime() == null || contest.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间和结束时间不能为空");
        }
        if (!contest.getEndTime().isAfter(contest.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间必须晚于开始时间");
        }
        if (contest.getSubmitLimit() != null && contest.getSubmitLimit() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交次数限制必须大于0");
        }
        if (ACCESS_PRIVATE.equalsIgnoreCase(contest.getAccessType()) && !StringUtils.hasText(contest.getAccessPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "私有比赛必须配置访问密码");
        }
    }

    private String normalizeType(String type) {
        String value = trimText(type);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛类型不能为空");
        }
        String upper = value.toUpperCase();
        if (!TYPE_CONTEST.equals(upper) && !TYPE_HOMEWORK.equals(upper)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛类型仅支持 CONTEST 或 HOMEWORK");
        }
        return upper;
    }

    private String normalizeTypeNullable(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return normalizeType(type);
    }

    private String normalizeAccessType(String accessType) {
        if (!StringUtils.hasText(accessType)) {
            return ACCESS_PUBLIC;
        }
        String value = accessType.trim().toUpperCase();
        if (!ACCESS_PUBLIC.equals(value) && !ACCESS_PRIVATE.equals(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参与方式仅支持 PUBLIC 或 PRIVATE");
        }
        return value;
    }

    private String normalizeResultVisibleMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return RESULT_VISIBLE_REALTIME;
        }
        String value = mode.trim().toUpperCase();
        if (!RESULT_VISIBLE_REALTIME.equals(value) && !RESULT_VISIBLE_AFTER_END.equals(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结果可见规则仅支持 REALTIME 或 AFTER_END");
        }
        return value;
    }

    private String trimText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private boolean isContestVisibleForStudent(Contest contest) {
        return contest != null
                && contest.getStatus() != null
                && (contest.getStatus() == STATUS_PUBLISHED || contest.getStatus() == STATUS_ENDED);
    }

    private void checkManagerRole(LoginUserVO loginUser) {
        if (!isManager(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可操作");
        }
    }

    private void checkCreatorOrAdmin(LoginUserVO loginUser, Long creatorId) {
        if (isAdmin(loginUser)) {
            return;
        }
        if (creatorId == null || !creatorId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅比赛创建者可操作");
        }
    }

    private boolean isManager(LoginUserVO loginUser) {
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles())) {
            return false;
        }
        return loginUser.getRoles().contains(ROLE_ADMIN) || loginUser.getRoles().contains(ROLE_TEACHER);
    }

    private boolean isAdmin(LoginUserVO loginUser) {
        return loginUser != null
                && !CollectionUtils.isEmpty(loginUser.getRoles())
                && loginUser.getRoles().contains(ROLE_ADMIN);
    }

    private LoginUserVO getLoginUser(String token) {
        return authService.getLoginUser(token);
    }

    private record ProblemSubmissionStat(long submitCount, long acceptCount) {
    }
}
