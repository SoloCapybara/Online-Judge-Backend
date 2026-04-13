package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.ContestMapper;
import com.dxd.oj.mapper.ContestProblemMapper;
import com.dxd.oj.mapper.ContestUserMapper;
import com.dxd.oj.mapper.ProblemCaseMapper;
import com.dxd.oj.mapper.ProblemMapper;
import com.dxd.oj.mapper.SubmissionCaseMapper;
import com.dxd.oj.mapper.SubmissionMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.model.dto.run.RunCodeRequest;
import com.dxd.oj.model.dto.submission.SubmissionCreateRequest;
import com.dxd.oj.model.dto.submission.SubmissionPageQueryRequest;
import com.dxd.oj.model.entity.Contest;
import com.dxd.oj.model.entity.ContestProblem;
import com.dxd.oj.model.entity.ContestUser;
import com.dxd.oj.model.entity.Problem;
import com.dxd.oj.model.entity.ProblemCase;
import com.dxd.oj.model.entity.Submission;
import com.dxd.oj.model.entity.SubmissionCase;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.vo.JudgeOverviewVO;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.RunCodeResponse;
import com.dxd.oj.model.vo.SubmissionCaseVO;
import com.dxd.oj.model.vo.SubmissionCreateVO;
import com.dxd.oj.model.vo.SubmissionDetailVO;
import com.dxd.oj.model.vo.SubmissionVO;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.ContestRankService;
import com.dxd.oj.service.RunService;
import com.dxd.oj.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final int CONTEST_STATUS_PUBLISHED = 1;
    private static final int CONTEST_STATUS_ENDED = 2;

    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_JUDGING = "JUDGING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_PARTIAL_ACCEPTED = "PARTIAL_ACCEPTED";
    private static final String STATUS_WRONG_ANSWER = "WRONG_ANSWER";
    private static final String STATUS_COMPILE_ERROR = "COMPILE_ERROR";
    private static final String STATUS_RUNTIME_ERROR = "RUNTIME_ERROR";
    private static final String STATUS_TIMEOUT = "TIME_LIMIT_EXCEEDED";
    private static final String STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";

    private static final String RUN_STATUS_SUCCESS = "SUCCESS";
    private static final String RUN_STATUS_COMPILE_ERROR = "COMPILE_ERROR";
    private static final String RUN_STATUS_RUNTIME_ERROR = "RUNTIME_ERROR";
    private static final String RUN_STATUS_TIMEOUT = "TIME_LIMIT_EXCEEDED";
    private static final String RUN_STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("JAVA17", "PYTHON3142", "C17", "CPP17");
    private static final Pattern TRAILING_SPACE_PATTERN = Pattern.compile("\\s+$");

    private final SubmissionMapper submissionMapper;
    private final SubmissionCaseMapper submissionCaseMapper;
    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestUserMapper contestUserMapper;
    private final ProblemMapper problemMapper;
    private final ProblemCaseMapper problemCaseMapper;
    private final SysUserMapper sysUserMapper;
    private final AuthService authService;
    private final ContestRankService contestRankService;
    private final RunService runService;
    private final JdbcTemplate jdbcTemplate;

    private static final int MAX_SOURCE_CODE_LENGTH = 200000;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmissionCreateVO createSubmission(SubmissionCreateRequest request, String token) {
        LoginUserVO loginUser = getLoginUser(token);
        validateCreateRequest(request);

        Problem problem = problemMapper.selectById(request.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        checkProblemVisible(problem, loginUser);
        validateContestSubmissionContext(request, loginUser, problem);

        List<ProblemCase> cases = listProblemCases(problem.getId());
        if (CollectionUtils.isEmpty(cases)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目未配置测试用例，无法判题");
        }

        Submission submission = new Submission();
        submission.setUserId(loginUser.getId());
        submission.setProblemId(problem.getId());
        submission.setContestId(request.getContestId());
        submission.setLanguage(normalizeLanguage(request.getLanguage()));
        submission.setSourceCode(request.getSourceCode());
        submission.setStatus(STATUS_WAITING);
        submission.setScore(0);

        int inserted = submissionMapper.insert(submission);
        if (inserted <= 0 || submission.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建提交记录失败");
        }
        increaseProblemSubmitCount(problem.getId());

        JudgeSummary summary = doJudge(submission, problem, cases, token);
        if (STATUS_ACCEPTED.equals(summary.finalStatus())) {
            increaseProblemAcceptCount(problem.getId());
        }
        if (submission.getContestId() != null && submission.getContestId() > 0) {
            contestRankService.refreshUserRank(submission.getContestId(), submission.getUserId());
        }

        SubmissionCreateVO response = new SubmissionCreateVO();
        response.setSubmissionId(submission.getId());
        response.setStatus(summary.finalStatus());
        return response;
    }

    @Override
    public IPage<SubmissionVO> pageSubmissions(SubmissionPageQueryRequest queryRequest, String token) {
        LoginUserVO loginUser = getLoginUser(token);
        SubmissionPageQueryRequest safeQuery = queryRequest == null ? new SubmissionPageQueryRequest() : queryRequest;
        long current = safeQuery.getCurrent() <= 0 ? 1 : safeQuery.getCurrent();
        long pageSize = safeQuery.getPageSize() <= 0 ? 10 : Math.min(safeQuery.getPageSize(), 100);
        boolean manager = isManager(loginUser);

        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(safeQuery.getProblemId() != null, Submission::getProblemId, safeQuery.getProblemId())
                .eq(safeQuery.getContestId() != null, Submission::getContestId, safeQuery.getContestId());

        String status = normalizeStatus(safeQuery.getStatus());
        if (StringUtils.hasText(status)) {
            wrapper.eq(Submission::getStatus, status);
        }
        String language = normalizeLanguageNullable(safeQuery.getLanguage());
        if (StringUtils.hasText(language)) {
            wrapper.eq(Submission::getLanguage, language);
        }

        if (manager) {
            if (Boolean.TRUE.equals(safeQuery.getMine())) {
                wrapper.eq(Submission::getUserId, loginUser.getId());
            } else if (safeQuery.getUserId() != null && safeQuery.getUserId() > 0) {
                wrapper.eq(Submission::getUserId, safeQuery.getUserId());
            }
        } else {
            wrapper.eq(Submission::getUserId, loginUser.getId());
        }
        wrapper.orderByDesc(Submission::getSubmitTime, Submission::getId);

        Page<Submission> page = submissionMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Page<SubmissionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SubmissionVO> records = page.getRecords().stream().map(this::toSubmissionVO).toList();
        fillSubmissionExtraInfo(records);
        result.setRecords(records);
        return result;
    }

    @Override
    public SubmissionDetailVO getSubmissionDetail(Long submissionId, String token) {
        if (submissionId == null || submissionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }
        checkSubmissionViewPermission(submission, loginUser);

        SubmissionDetailVO detail = toSubmissionDetailVO(submission);
        fillSubmissionDetailExtra(detail);
        return detail;
    }

    @Override
    public List<SubmissionCaseVO> listSubmissionCases(Long submissionId, String token) {
        if (submissionId == null || submissionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }
        checkSubmissionViewPermission(submission, loginUser);

        List<SubmissionCase> caseList = submissionCaseMapper.selectList(new LambdaQueryWrapper<SubmissionCase>()
                .eq(SubmissionCase::getSubmissionId, submissionId)
                .orderByAsc(SubmissionCase::getCaseNo));
        if (CollectionUtils.isEmpty(caseList)) {
            return List.of();
        }
        return caseList.stream().map(this::toSubmissionCaseVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejudgeSubmission(Long submissionId, String token) {
        if (submissionId == null || submissionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        if (!isManager(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可重判");
        }

        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }
        Problem problem = problemMapper.selectById(submission.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在，无法重判");
        }
        List<ProblemCase> cases = listProblemCases(problem.getId());
        if (CollectionUtils.isEmpty(cases)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目未配置测试用例，无法重判");
        }

        boolean oldAccepted = STATUS_ACCEPTED.equalsIgnoreCase(submission.getStatus());
        JudgeSummary summary = doJudge(submission, problem, cases, token);
        boolean newAccepted = STATUS_ACCEPTED.equals(summary.finalStatus());
        if (oldAccepted != newAccepted) {
            adjustProblemAcceptCount(problem.getId(), newAccepted ? 1 : -1);
        }
        if (submission.getContestId() != null && submission.getContestId() > 0) {
            contestRankService.refreshUserRank(submission.getContestId(), submission.getUserId());
        }
        return true;
    }

    @Override
    public JudgeOverviewVO getJudgeOverview() {
        Long queueCount = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .in(Submission::getStatus, STATUS_WAITING, STATUS_JUDGING));
        long queueTaskCount = queueCount == null ? 0L : queueCount;
        long onlineNodeCount = safeQueryCount(
                "select count(1) from judge_service where service_status in ('ONLINE','UP','NORMAL')"
        );
        long todaySubmissionCount = safeQueryCount(
                "select count(1) from submission where date(submit_time) = curdate()"
        );

        JudgeOverviewVO overview = new JudgeOverviewVO();
        overview.setQueueTaskCount(queueTaskCount);
        overview.setOnlineNodeCount(onlineNodeCount);
        overview.setTodaySubmissionCount(todaySubmissionCount);
        if (onlineNodeCount <= 0) {
            overview.setServiceStatus("DOWN");
        } else if (queueTaskCount > 200) {
            overview.setServiceStatus("BUSY");
        } else {
            overview.setServiceStatus("NORMAL");
        }
        return overview;
    }

    private JudgeSummary doJudge(Submission submission, Problem problem, List<ProblemCase> cases, String token) {
        submission.setStatus(STATUS_JUDGING);
        submission.setJudgeTime(null);
        submission.setErrorMessage(null);
        submissionMapper.updateById(submission);
        submissionCaseMapper.delete(new LambdaQueryWrapper<SubmissionCase>()
                .eq(SubmissionCase::getSubmissionId, submission.getId()));

        boolean oiMode = "OI".equalsIgnoreCase(problem.getJudgeMode());
        int acceptedCaseCount = 0;
        int score = 0;
        int totalConfiguredScore = cases.stream().mapToInt(this::resolveCaseScore).sum();
        int maxTimeUsed = 0;

        boolean hasWrongAnswer = false;
        boolean hasCompileError = false;
        boolean hasRuntimeError = false;
        boolean hasTimeout = false;
        boolean hasSystemError = false;
        String firstErrorMessage = null;

        List<SubmissionCase> caseResults = new ArrayList<>();
        for (ProblemCase problemCase : cases) {
            CaseJudgeOutcome outcome = judgeSingleCase(submission, problem, problemCase, token);

            SubmissionCase caseRow = new SubmissionCase();
            caseRow.setSubmissionId(submission.getId());
            caseRow.setCaseNo(problemCase.getCaseNo());
            caseRow.setStatus(outcome.status());
            caseRow.setTimeUsed(outcome.timeUsedMs());
            caseRow.setMemoryUsed(null);
            caseRow.setMessage(outcome.message());
            caseResults.add(caseRow);

            maxTimeUsed = Math.max(maxTimeUsed, Math.max(0, outcome.timeUsedMs()));
            if (STATUS_ACCEPTED.equals(outcome.status())) {
                acceptedCaseCount++;
                if (oiMode && totalConfiguredScore > 0) {
                    score += resolveCaseScore(problemCase);
                }
            } else {
                if (firstErrorMessage == null) {
                    firstErrorMessage = outcome.message();
                }
                switch (outcome.status()) {
                    case STATUS_COMPILE_ERROR -> hasCompileError = true;
                    case STATUS_RUNTIME_ERROR -> hasRuntimeError = true;
                    case STATUS_TIMEOUT -> hasTimeout = true;
                    case STATUS_SYSTEM_ERROR -> hasSystemError = true;
                    default -> hasWrongAnswer = true;
                }
                if (!oiMode) {
                    break;
                }
                if (hasCompileError || hasSystemError) {
                    break;
                }
            }
        }
        if (!CollectionUtils.isEmpty(caseResults)) {
            caseResults.forEach(submissionCaseMapper::insert);
        }

        boolean allAccepted = acceptedCaseCount == cases.size();
        String finalStatus;
        if (allAccepted) {
            finalStatus = STATUS_ACCEPTED;
        } else if (hasCompileError) {
            finalStatus = STATUS_COMPILE_ERROR;
        } else if (hasSystemError) {
            finalStatus = STATUS_SYSTEM_ERROR;
        } else if (hasRuntimeError) {
            finalStatus = STATUS_RUNTIME_ERROR;
        } else if (hasTimeout) {
            finalStatus = STATUS_TIMEOUT;
        } else if (oiMode && acceptedCaseCount > 0) {
            finalStatus = STATUS_PARTIAL_ACCEPTED;
        } else if (hasWrongAnswer || acceptedCaseCount < cases.size()) {
            finalStatus = STATUS_WRONG_ANSWER;
        } else {
            finalStatus = STATUS_SYSTEM_ERROR;
        }

        if (oiMode) {
            if (totalConfiguredScore <= 0) {
                score = cases.isEmpty() ? 0 : Math.round(acceptedCaseCount * 100f / cases.size());
            }
        } else {
            score = STATUS_ACCEPTED.equals(finalStatus) ? 100 : 0;
        }

        submission.setStatus(finalStatus);
        submission.setTimeUsed(maxTimeUsed);
        submission.setMemoryUsed(null);
        submission.setScore(score);
        submission.setErrorMessage(truncateText(firstErrorMessage, 500));
        submission.setJudgeTime(LocalDateTime.now());
        submissionMapper.updateById(submission);
        log.info("提交判题完成：submissionId={}, userId={}, problemId={}, status={}, score={}",
                submission.getId(), submission.getUserId(), submission.getProblemId(), finalStatus, score);
        return new JudgeSummary(finalStatus);
    }

    private CaseJudgeOutcome judgeSingleCase(Submission submission, Problem problem, ProblemCase problemCase, String token) {
        RunCodeRequest runRequest = new RunCodeRequest();
        runRequest.setLanguage(submission.getLanguage());
        runRequest.setSourceCode(submission.getSourceCode());
        runRequest.setInputData(problemCase.getInputData());
        if (problem.getTimeLimit() != null && problem.getTimeLimit() > 0) {
            runRequest.setTimeLimitMs(problem.getTimeLimit());
        }

        try {
            RunCodeResponse runResult = runService.runCode(runRequest, token);
            String runStatus = normalizeStatus(runResult.getStatus());
            int timeUsedMs = resolveTimeUsed(runResult);
            if (RUN_STATUS_SUCCESS.equals(runStatus)) {
                String expected = normalizeOutput(problemCase.getOutputData());
                String actual = normalizeOutput(runResult.getStdout());
                if (Objects.equals(expected, actual)) {
                    return new CaseJudgeOutcome(STATUS_ACCEPTED, timeUsedMs, "通过");
                }
                return new CaseJudgeOutcome(STATUS_WRONG_ANSWER, timeUsedMs, "输出不匹配");
            }
            if (RUN_STATUS_COMPILE_ERROR.equals(runStatus)) {
                return new CaseJudgeOutcome(STATUS_COMPILE_ERROR, timeUsedMs, firstNonBlank(
                        truncateText(runResult.getStderr(), 500),
                        "编译错误"
                ));
            }
            if (RUN_STATUS_RUNTIME_ERROR.equals(runStatus)) {
                return new CaseJudgeOutcome(STATUS_RUNTIME_ERROR, timeUsedMs, firstNonBlank(
                        truncateText(runResult.getStderr(), 500),
                        "运行时错误"
                ));
            }
            if (RUN_STATUS_TIMEOUT.equals(runStatus)) {
                return new CaseJudgeOutcome(STATUS_TIMEOUT, timeUsedMs, firstNonBlank(
                        truncateText(runResult.getStderr(), 500),
                        "运行超时"
                ));
            }
            return new CaseJudgeOutcome(STATUS_SYSTEM_ERROR, timeUsedMs, firstNonBlank(
                    truncateText(runResult.getStderr(), 500),
                    "系统错误"
            ));
        } catch (BusinessException e) {
            return new CaseJudgeOutcome(STATUS_SYSTEM_ERROR, 0, firstNonBlank(e.getMessage(), "系统错误"));
        } catch (Exception e) {
            log.error("执行测试点异常：submissionId={}, caseNo={}", submission.getId(), problemCase.getCaseNo(), e);
            return new CaseJudgeOutcome(STATUS_SYSTEM_ERROR, 0, firstNonBlank(e.getMessage(), "系统错误"));
        }
    }

    private int resolveTimeUsed(RunCodeResponse runResult) {
        long execute = runResult.getExecuteTimeMs() == null ? 0L : runResult.getExecuteTimeMs();
        long compatible = runResult.getTimeUsedMs() == null ? 0L : runResult.getTimeUsedMs();
        long value = Math.max(execute, compatible);
        if (value <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private void fillSubmissionExtraInfo(List<SubmissionVO> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> problemIds = new LinkedHashSet<>();
        Set<Long> contestIds = new LinkedHashSet<>();
        records.forEach(item -> {
            if (item.getUserId() != null) {
                userIds.add(item.getUserId());
            }
            if (item.getProblemId() != null) {
                problemIds.add(item.getProblemId());
            }
            if (item.getContestId() != null) {
                contestIds.add(item.getContestId());
            }
        });

        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        if (!userIds.isEmpty()) {
            sysUserMapper.selectBatchIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        }
        Map<Long, Problem> problemMap = new LinkedHashMap<>();
        if (!problemIds.isEmpty()) {
            problemMapper.selectBatchIds(problemIds).forEach(problem -> problemMap.put(problem.getId(), problem));
        }
        Map<Long, Contest> contestMap = new LinkedHashMap<>();
        if (!contestIds.isEmpty()) {
            contestMapper.selectBatchIds(contestIds).forEach(contest -> contestMap.put(contest.getId(), contest));
        }

        records.forEach(item -> {
            SysUser user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUsername(user.getUsername());
                item.setUserRealName(user.getRealName());
            }
            Problem problem = problemMap.get(item.getProblemId());
            if (problem != null) {
                item.setProblemTitle(problem.getTitle());
            }
            Contest contest = contestMap.get(item.getContestId());
            if (contest != null) {
                item.setContestTitle(contest.getTitle());
                item.setContestType(contest.getType());
            }
        });
    }

    private void fillSubmissionDetailExtra(SubmissionDetailVO detail) {
        if (detail == null) {
            return;
        }
        if (detail.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(detail.getUserId());
            if (user != null) {
                detail.setUsername(user.getUsername());
                detail.setUserRealName(user.getRealName());
            }
        }
        if (detail.getProblemId() != null) {
            Problem problem = problemMapper.selectById(detail.getProblemId());
            if (problem != null) {
                detail.setProblemTitle(problem.getTitle());
            }
        }
        if (detail.getContestId() != null) {
            Contest contest = contestMapper.selectById(detail.getContestId());
            if (contest != null) {
                detail.setContestTitle(contest.getTitle());
                detail.setContestType(contest.getType());
            }
        }
    }

    private SubmissionVO toSubmissionVO(Submission submission) {
        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setUserId(submission.getUserId());
        vo.setProblemId(submission.getProblemId());
        vo.setContestId(submission.getContestId());
        vo.setLanguage(submission.getLanguage());
        vo.setStatus(submission.getStatus());
        vo.setTimeUsed(submission.getTimeUsed());
        vo.setMemoryUsed(submission.getMemoryUsed());
        vo.setScore(submission.getScore());
        vo.setErrorMessage(submission.getErrorMessage());
        vo.setSubmitTime(submission.getSubmitTime());
        vo.setJudgeTime(submission.getJudgeTime());
        return vo;
    }

    private SubmissionDetailVO toSubmissionDetailVO(Submission submission) {
        SubmissionDetailVO detail = new SubmissionDetailVO();
        detail.setId(submission.getId());
        detail.setUserId(submission.getUserId());
        detail.setProblemId(submission.getProblemId());
        detail.setContestId(submission.getContestId());
        detail.setLanguage(submission.getLanguage());
        detail.setSourceCode(submission.getSourceCode());
        detail.setStatus(submission.getStatus());
        detail.setTimeUsed(submission.getTimeUsed());
        detail.setMemoryUsed(submission.getMemoryUsed());
        detail.setScore(submission.getScore());
        detail.setErrorMessage(submission.getErrorMessage());
        detail.setSubmitTime(submission.getSubmitTime());
        detail.setJudgeTime(submission.getJudgeTime());
        return detail;
    }

    private SubmissionCaseVO toSubmissionCaseVO(SubmissionCase submissionCase) {
        SubmissionCaseVO vo = new SubmissionCaseVO();
        vo.setCaseNo(submissionCase.getCaseNo());
        vo.setStatus(submissionCase.getStatus());
        vo.setTimeUsed(submissionCase.getTimeUsed());
        vo.setMemoryUsed(submissionCase.getMemoryUsed());
        vo.setMessage(submissionCase.getMessage());
        return vo;
    }

    private List<ProblemCase> listProblemCases(Long problemId) {
        return problemCaseMapper.selectList(new LambdaQueryWrapper<ProblemCase>()
                .eq(ProblemCase::getProblemId, problemId)
                .orderByAsc(ProblemCase::getCaseNo));
    }

    private int resolveCaseScore(ProblemCase problemCase) {
        if (problemCase == null || problemCase.getScore() == null || problemCase.getScore() < 0) {
            return 0;
        }
        return problemCase.getScore();
    }

    private void validateCreateRequest(SubmissionCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (request.getProblemId() == null || request.getProblemId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不合法");
        }
        if (!StringUtils.hasText(request.getLanguage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交语言不能为空");
        }
        if (!StringUtils.hasText(request.getSourceCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "源代码不能为空");
        }
        if (request.getSourceCode().length() > 200000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "源码长度不能超过200000字符");
        }
        String language = normalizeLanguage(request.getLanguage());
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该语言");
        }
    }

    private void checkProblemVisible(Problem problem, LoginUserVO loginUser) {
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        if (isManager(loginUser)) {
            return;
        }
        if (problem.getStatus() == null || problem.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前题目不可提交");
        }
    }

    private void validateContestSubmissionContext(SubmissionCreateRequest request, LoginUserVO loginUser, Problem problem) {
        if (request.getContestId() == null) {
            return;
        }
        if (request.getContestId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛ID不合法");
        }
        Long contestId = request.getContestId();
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        }
        Long includedCount = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problem.getId()));
        if (includedCount == null || includedCount <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该题目不在当前比赛题目集内");
        }

        boolean manager = isManager(loginUser);
        if (!manager) {
            if (contest.getStatus() == null || contest.getStatus() != CONTEST_STATUS_PUBLISHED) {
                if (contest.getStatus() != null && contest.getStatus() == CONTEST_STATUS_ENDED) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "比赛已结束，无法提交");
                }
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前比赛不可提交");
            }

            LocalDateTime now = LocalDateTime.now();
            if (contest.getStartTime() != null && now.isBefore(contest.getStartTime())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "比赛未开始，暂不能提交");
            }
            if (contest.getEndTime() != null && now.isAfter(contest.getEndTime())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "比赛已结束，无法提交");
            }

            Long joinCount = contestUserMapper.selectCount(new LambdaQueryWrapper<ContestUser>()
                    .eq(ContestUser::getContestId, contestId)
                    .eq(ContestUser::getUserId, loginUser.getId()));
            if (joinCount == null || joinCount <= 0) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先报名比赛后再提交");
            }
        }

        if ("CONTEST".equalsIgnoreCase(contest.getType())) {
            Long acceptedCount = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getContestId, contestId)
                    .eq(Submission::getUserId, loginUser.getId())
                    .eq(Submission::getProblemId, problem.getId())
                    .eq(Submission::getStatus, STATUS_ACCEPTED));
            if (acceptedCount != null && acceptedCount > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该题已通过，比赛模式下不可再次提交");
            }
        }

        if (contest.getSubmitLimit() != null && contest.getSubmitLimit() > 0) {
            Long submitCount = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getContestId, contestId)
                    .eq(Submission::getUserId, loginUser.getId())
                    .eq(Submission::getProblemId, problem.getId()));
            if (submitCount != null && submitCount >= contest.getSubmitLimit()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已达到该题目提交次数上限");
            }
        }
    }

    private void checkSubmissionViewPermission(Submission submission, LoginUserVO loginUser) {
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }
        if (isManager(loginUser)) {
            return;
        }
        if (!Objects.equals(submission.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该提交记录");
        }
    }

    private void increaseProblemSubmitCount(Long problemId) {
        jdbcTemplate.update(
                "update problem set submit_count = ifnull(submit_count, 0) + 1 where id = ?",
                problemId
        );
    }

    private void increaseProblemAcceptCount(Long problemId) {
        jdbcTemplate.update(
                "update problem set accept_count = ifnull(accept_count, 0) + 1 where id = ?",
                problemId
        );
    }

    private void adjustProblemAcceptCount(Long problemId, int delta) {
        if (delta == 0) {
            return;
        }
        if (delta > 0) {
            increaseProblemAcceptCount(problemId);
            return;
        }
        jdbcTemplate.update(
                "update problem set accept_count = greatest(ifnull(accept_count, 0) - 1, 0) where id = ?",
                problemId
        );
    }

    private long safeQueryCount(String sql) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class);
            return value == null ? 0L : Math.max(0L, value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String normalizeLanguage(String language) {
        return language.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLanguageNullable(String language) {
        if (!StringUtils.hasText(language)) {
            return null;
        }
        return normalizeLanguage(language);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }
        String normalized = output.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>(rawLines.length);
        for (String line : rawLines) {
            lines.add(TRAILING_SPACE_PATTERN.matcher(line).replaceAll(""));
        }
        int tail = lines.size() - 1;
        while (tail >= 0 && lines.get(tail).isEmpty()) {
            tail--;
        }
        if (tail < 0) {
            return "";
        }
        return String.join("\n", lines.subList(0, tail + 1));
    }

    private boolean isManager(LoginUserVO loginUser) {
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles())) {
            return false;
        }
        return loginUser.getRoles().contains(ROLE_ADMIN) || loginUser.getRoles().contains(ROLE_TEACHER);
    }

    private LoginUserVO getLoginUser(String token) {
        return authService.getLoginUser(token);
    }

    private String truncateText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String firstNonBlank(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        return fallback;
    }

    private record CaseJudgeOutcome(String status, int timeUsedMs, String message) {
    }

    private record JudgeSummary(String finalStatus) {
    }
}
