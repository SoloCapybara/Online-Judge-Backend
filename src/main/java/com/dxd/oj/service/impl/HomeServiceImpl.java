package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.mapper.ContestMapper;
import com.dxd.oj.mapper.ProblemMapper;
import com.dxd.oj.mapper.SubmissionMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.model.entity.Contest;
import com.dxd.oj.model.entity.Problem;
import com.dxd.oj.model.entity.Submission;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.vo.HomeOverviewVO;
import com.dxd.oj.model.vo.HomeRecentContestVO;
import com.dxd.oj.model.vo.HomeRecentSubmissionVO;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";

    private static final int CONTEST_STATUS_DRAFT = 0;
    private static final int CONTEST_STATUS_PUBLISHED = 1;
    private static final int CONTEST_STATUS_ENDED = 2;
    private static final int CONTEST_STATUS_CLOSED = 3;

    private static final String SUBMISSION_STATUS_WAITING = "WAITING";
    private static final String SUBMISSION_STATUS_JUDGING = "JUDGING";
    private static final String SUBMISSION_STATUS_ACCEPTED = "ACCEPTED";

    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final ContestMapper contestMapper;
    private final SysUserMapper sysUserMapper;
    private final AuthService authService;

    @Override
    public HomeOverviewVO getHomeOverview(String token) {
        LoginUserVO loginUser = authService.getLoginUser(token);
        boolean manager = isManager(loginUser);

        HomeOverviewVO overview = new HomeOverviewVO();
        overview.setProblemCount(safeLong(problemMapper.selectCount(new LambdaQueryWrapper<Problem>())));
        overview.setTodaySubmissionCount(queryTodaySubmissionCount());
        overview.setQueueTaskCount(queryQueueTaskCount());
        overview.setGlobalAcRate(queryGlobalAcRate());
        overview.setRecentContests(queryRecentContests(manager));
        overview.setRecentSubmissions(queryRecentSubmissions());
        overview.setServerTime(LocalDateTime.now());
        return overview;
    }

    private Long queryTodaySubmissionCount() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        return safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .ge(Submission::getSubmitTime, dayStart)
                .lt(Submission::getSubmitTime, dayEnd)));
    }

    private Long queryQueueTaskCount() {
        return safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .in(Submission::getStatus, SUBMISSION_STATUS_WAITING, SUBMISSION_STATUS_JUDGING)));
    }

    private Double queryGlobalAcRate() {
        long totalSubmissionCount = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()));
        if (totalSubmissionCount <= 0) {
            return 0D;
        }
        long acceptedSubmissionCount = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, SUBMISSION_STATUS_ACCEPTED)));
        double rate = acceptedSubmissionCount * 100D / totalSubmissionCount;
        return Math.round(rate * 10D) / 10D;
    }

    private List<HomeRecentContestVO> queryRecentContests(boolean manager) {
        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<>();
        if (!manager) {
            wrapper.in(Contest::getStatus, CONTEST_STATUS_PUBLISHED, CONTEST_STATUS_ENDED);
        }
        wrapper.orderByDesc(Contest::getStartTime, Contest::getId)
                .last("limit 6");
        List<Contest> contestList = contestMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(contestList)) {
            return List.of();
        }

        List<HomeRecentContestVO> result = new ArrayList<>(contestList.size());
        for (Contest contest : contestList) {
            HomeRecentContestVO item = new HomeRecentContestVO();
            item.setId(contest.getId());
            item.setTitle(contest.getTitle());
            item.setType(contest.getType());
            item.setAccessType(contest.getAccessType());
            item.setStatusText(resolveContestStatusText(contest));
            item.setStartTime(contest.getStartTime());
            item.setEndTime(contest.getEndTime());
            result.add(item);
        }
        return result;
    }

    private List<HomeRecentSubmissionVO> queryRecentSubmissions() {
        List<Submission> submissionList = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .orderByDesc(Submission::getSubmitTime, Submission::getId)
                .last("limit 5"));
        if (CollectionUtils.isEmpty(submissionList)) {
            return List.of();
        }

        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> problemIds = new LinkedHashSet<>();
        for (Submission submission : submissionList) {
            if (submission.getUserId() != null) {
                userIds.add(submission.getUserId());
            }
            if (submission.getProblemId() != null) {
                problemIds.add(submission.getProblemId());
            }
        }

        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        if (!userIds.isEmpty()) {
            sysUserMapper.selectBatchIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        }

        Map<Long, Problem> problemMap = new LinkedHashMap<>();
        if (!problemIds.isEmpty()) {
            problemMapper.selectBatchIds(problemIds).forEach(problem -> problemMap.put(problem.getId(), problem));
        }

        List<HomeRecentSubmissionVO> result = new ArrayList<>(submissionList.size());
        for (Submission submission : submissionList) {
            HomeRecentSubmissionVO item = new HomeRecentSubmissionVO();
            item.setId(submission.getId());
            item.setUserId(submission.getUserId());
            item.setProblemId(submission.getProblemId());
            item.setLanguage(submission.getLanguage());
            item.setStatus(submission.getStatus());
            item.setTimeUsed(submission.getTimeUsed());
            item.setMemoryUsed(submission.getMemoryUsed());
            item.setSubmitTime(submission.getSubmitTime());

            SysUser user = userMap.get(submission.getUserId());
            if (user != null) {
                item.setUsername(user.getUsername());
                item.setUserRealName(user.getRealName());
            }

            Problem problem = problemMap.get(submission.getProblemId());
            if (problem != null) {
                item.setProblemTitle(problem.getTitle());
            }
            result.add(item);
        }
        return result;
    }

    private String resolveContestStatusText(Contest contest) {
        if (contest == null || contest.getStatus() == null) {
            return "未知";
        }
        Integer status = contest.getStatus();
        if (Objects.equals(status, CONTEST_STATUS_DRAFT)) {
            return "未发布";
        }
        if (Objects.equals(status, CONTEST_STATUS_CLOSED)) {
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

    private boolean isManager(LoginUserVO loginUser) {
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles())) {
            return false;
        }
        return loginUser.getRoles().contains(ROLE_ADMIN) || loginUser.getRoles().contains(ROLE_TEACHER);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }
}
