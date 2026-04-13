package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dxd.oj.mapper.ContestMapper;
import com.dxd.oj.mapper.ContestUserMapper;
import com.dxd.oj.mapper.SubmissionMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.model.entity.Contest;
import com.dxd.oj.model.entity.ContestUser;
import com.dxd.oj.model.entity.Submission;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.vo.ContestRankVO;
import com.dxd.oj.service.ContestRankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestRankServiceImpl implements ContestRankService {

    private static final String STATUS_ACCEPTED = "ACCEPTED";

    private final ContestMapper contestMapper;
    private final ContestUserMapper contestUserMapper;
    private final SubmissionMapper submissionMapper;
    private final SysUserMapper sysUserMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${oj.contest.rank-key-prefix:oj:contest:rank:}")
    private String contestRankKeyPrefix;

    @Value("${oj.contest.rank-penalty-key-prefix:oj:contest:rank:penalty:}")
    private String contestRankPenaltyKeyPrefix;

    @Override
    public void refreshUserRank(Long contestId, Long userId) {
        if (contestId == null || contestId <= 0 || userId == null || userId <= 0) {
            return;
        }
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            return;
        }

        RankStats stats = calcRankStats(contestId, userId, contest.getStartTime());
        ContestUser contestUser = contestUserMapper.selectOne(new LambdaQueryWrapper<ContestUser>()
                .eq(ContestUser::getContestId, contestId)
                .eq(ContestUser::getUserId, userId)
                .last("limit 1"));
        if (contestUser == null) {
            contestUser = new ContestUser();
            contestUser.setContestId(contestId);
            contestUser.setUserId(userId);
            contestUser.setJoinTime(LocalDateTime.now());
            contestUser.setTotalScore(stats.totalScore());
            contestUser.setAcCount(stats.acCount());
            contestUserMapper.insert(contestUser);
        } else {
            contestUser.setTotalScore(stats.totalScore());
            contestUser.setAcCount(stats.acCount());
            if (stats.acCount() > 0 && contestUser.getFinishTime() == null) {
                contestUser.setFinishTime(LocalDateTime.now());
            }
            contestUserMapper.updateById(contestUser);
        }

        double rankScore = calcRedisRankScore(stats.acCount(), stats.totalScore(), stats.penaltyMinutes());
        try {
            stringRedisTemplate.opsForZSet().add(rankKey(contestId), String.valueOf(userId), rankScore);
            stringRedisTemplate.opsForHash().put(rankPenaltyKey(contestId), String.valueOf(userId), String.valueOf(stats.penaltyMinutes()));
        } catch (Exception e) {
            log.warn("刷新比赛排行榜缓存失败: contestId={}, userId={}", contestId, userId, e);
        }
    }

    @Override
    public IPage<ContestRankVO> pageRank(Long contestId, long current, long pageSize) {
        long safeCurrent = current <= 0 ? 1 : current;
        long safePageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);

        try {
            String rankKey = rankKey(contestId);
            Long total = stringRedisTemplate.opsForZSet().zCard(rankKey);
            long totalCount = total == null ? 0L : total;
            if (totalCount <= 0) {
                rebuildRankCache(contestId);
                total = stringRedisTemplate.opsForZSet().zCard(rankKey);
                totalCount = total == null ? 0L : total;
            }
            if (totalCount <= 0) {
                return emptyPage(safeCurrent, safePageSize);
            }

            long start = (safeCurrent - 1) * safePageSize;
            if (start >= totalCount) {
                return emptyPage(safeCurrent, safePageSize, totalCount);
            }
            long end = Math.min(totalCount - 1, start + safePageSize - 1);

            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(rankKey, start, end);
            if (CollectionUtils.isEmpty(tuples)) {
                return emptyPage(safeCurrent, safePageSize, totalCount);
            }

            List<Long> orderedUserIds = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple == null || tuple.getValue() == null) {
                    continue;
                }
                try {
                    orderedUserIds.add(Long.parseLong(tuple.getValue()));
                } catch (NumberFormatException ignored) {
                }
            }
            if (orderedUserIds.isEmpty()) {
                return emptyPage(safeCurrent, safePageSize, totalCount);
            }

            Map<Long, ContestUser> contestUserMap = contestUserMapper.selectList(new LambdaQueryWrapper<ContestUser>()
                            .eq(ContestUser::getContestId, contestId)
                            .in(ContestUser::getUserId, orderedUserIds))
                    .stream()
                    .collect(Collectors.toMap(ContestUser::getUserId, item -> item, (a, b) -> a));

            Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(orderedUserIds)
                    .stream()
                    .collect(Collectors.toMap(SysUser::getId, item -> item, (a, b) -> a));

            List<Object> hashKeys = orderedUserIds.stream()
                    .map(String::valueOf)
                    .map(value -> (Object) value)
                    .toList();
            List<Object> penaltyValues = stringRedisTemplate.opsForHash().multiGet(
                    rankPenaltyKey(contestId),
                    hashKeys
            );
            Map<Long, Integer> penaltyMap = new HashMap<>();
            for (int i = 0; i < orderedUserIds.size(); i++) {
                Object raw = (penaltyValues == null || penaltyValues.size() <= i) ? null : penaltyValues.get(i);
                int value = 0;
                if (raw != null) {
                    try {
                        value = Integer.parseInt(String.valueOf(raw));
                    } catch (NumberFormatException ignored) {
                    }
                }
                penaltyMap.put(orderedUserIds.get(i), value);
            }

            List<ContestRankVO> records = new ArrayList<>();
            for (int i = 0; i < orderedUserIds.size(); i++) {
                Long userId = orderedUserIds.get(i);
                ContestUser contestUser = contestUserMap.get(userId);
                SysUser user = userMap.get(userId);

                ContestRankVO vo = new ContestRankVO();
                vo.setRank(start + i + 1);
                vo.setUserId(userId);
                vo.setUsername(user == null ? null : user.getUsername());
                vo.setRealName(user == null ? null : user.getRealName());
                vo.setAcCount(contestUser == null || contestUser.getAcCount() == null ? 0 : contestUser.getAcCount());
                vo.setTotalScore(contestUser == null || contestUser.getTotalScore() == null ? 0 : contestUser.getTotalScore());
                vo.setPenaltyMinutes(penaltyMap.getOrDefault(userId, 0));
                records.add(vo);
            }

            Page<ContestRankVO> page = new Page<>(safeCurrent, safePageSize, totalCount);
            page.setRecords(records);
            return page;
        } catch (Exception e) {
            log.warn("从 Redis 读取比赛榜单失败，降级为数据库查询: contestId={}", contestId, e);
            return pageRankFromDb(contestId, safeCurrent, safePageSize);
        }
    }

    @Override
    public void rebuildRankCache(Long contestId) {
        if (contestId == null || contestId <= 0) {
            return;
        }
        String rankKey = rankKey(contestId);
        String penaltyKey = rankPenaltyKey(contestId);

        try {
            stringRedisTemplate.delete(rankKey);
            stringRedisTemplate.delete(penaltyKey);
        } catch (Exception e) {
            log.warn("清理比赛排行榜缓存失败: contestId={}", contestId, e);
        }

        List<ContestUser> contestUsers = contestUserMapper.selectList(new LambdaQueryWrapper<ContestUser>()
                .eq(ContestUser::getContestId, contestId));
        if (CollectionUtils.isEmpty(contestUsers)) {
            return;
        }

        for (ContestUser contestUser : contestUsers) {
            Long userId = contestUser.getUserId();
            if (userId == null || userId <= 0) {
                continue;
            }
            refreshUserRank(contestId, userId);
        }
    }

    private Page<ContestRankVO> pageRankFromDb(Long contestId, long current, long pageSize) {
        Page<ContestUser> page = contestUserMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<ContestUser>()
                        .eq(ContestUser::getContestId, contestId)
                        .orderByDesc(ContestUser::getAcCount)
                        .orderByDesc(ContestUser::getTotalScore)
                        .orderByAsc(ContestUser::getJoinTime));

        List<ContestUser> userRecords = page.getRecords();
        if (CollectionUtils.isEmpty(userRecords)) {
            return emptyPage(current, pageSize, page.getTotal());
        }

        Set<Long> userIds = userRecords.stream().map(ContestUser::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, item -> item, (a, b) -> a));

        List<ContestRankVO> records = new ArrayList<>();
        for (int i = 0; i < userRecords.size(); i++) {
            ContestUser contestUser = userRecords.get(i);
            ContestRankVO vo = new ContestRankVO();
            vo.setRank((current - 1) * pageSize + i + 1);
            vo.setUserId(contestUser.getUserId());
            SysUser user = userMap.get(contestUser.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
            vo.setRealName(user == null ? null : user.getRealName());
            vo.setAcCount(contestUser.getAcCount() == null ? 0 : contestUser.getAcCount());
            vo.setTotalScore(contestUser.getTotalScore() == null ? 0 : contestUser.getTotalScore());
            vo.setPenaltyMinutes(0);
            records.add(vo);
        }

        Page<ContestRankVO> result = new Page<>(current, pageSize, page.getTotal());
        result.setRecords(records);
        return result;
    }

    private RankStats calcRankStats(Long contestId, Long userId, LocalDateTime contestStartTime) {
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getContestId, contestId)
                .eq(Submission::getUserId, userId)
                .orderByAsc(Submission::getSubmitTime, Submission::getId));
        if (CollectionUtils.isEmpty(submissions)) {
            return new RankStats(0, 0, 0);
        }

        Map<Long, ProblemRankStats> problemMap = new HashMap<>();
        for (Submission submission : submissions) {
            Long problemId = submission.getProblemId();
            if (problemId == null) {
                continue;
            }
            ProblemRankStats stats = problemMap.computeIfAbsent(problemId, ignore -> new ProblemRankStats());
            int score = submission.getScore() == null ? 0 : submission.getScore();
            stats.maxScore = Math.max(stats.maxScore, score);

            boolean accepted = STATUS_ACCEPTED.equalsIgnoreCase(submission.getStatus());
            if (!stats.accepted) {
                if (accepted) {
                    stats.accepted = true;
                    stats.firstAcceptedTime = submission.getSubmitTime();
                } else {
                    stats.wrongBeforeAccepted++;
                }
            }
        }

        int acCount = 0;
        int totalScore = 0;
        int penaltyMinutes = 0;
        for (ProblemRankStats stats : problemMap.values()) {
            totalScore += stats.maxScore;
            if (!stats.accepted) {
                continue;
            }
            acCount++;
            int wrongPenalty = Math.max(0, stats.wrongBeforeAccepted) * 20;
            int timePenalty = 0;
            if (contestStartTime != null && stats.firstAcceptedTime != null) {
                long minutes = Duration.between(contestStartTime, stats.firstAcceptedTime).toMinutes();
                timePenalty = (int) Math.max(0, minutes);
            }
            penaltyMinutes += wrongPenalty + timePenalty;
        }

        return new RankStats(acCount, totalScore, penaltyMinutes);
    }

    private double calcRedisRankScore(int acCount, int totalScore, int penaltyMinutes) {
        int safeAc = Math.max(0, acCount);
        int safeScore = Math.max(0, totalScore);
        int safePenalty = Math.max(0, Math.min(999_999, penaltyMinutes));
        int antiPenalty = 999_999 - safePenalty;
        return safeAc * 1_000_000_000_000d + safeScore * 1_000_000d + antiPenalty;
    }

    private Page<ContestRankVO> emptyPage(long current, long pageSize) {
        return emptyPage(current, pageSize, 0);
    }

    private Page<ContestRankVO> emptyPage(long current, long pageSize, long total) {
        Page<ContestRankVO> page = new Page<>(current, pageSize, total);
        page.setRecords(List.of());
        return page;
    }

    private String rankKey(Long contestId) {
        return contestRankKeyPrefix + contestId;
    }

    private String rankPenaltyKey(Long contestId) {
        return contestRankPenaltyKeyPrefix + contestId;
    }

    private record RankStats(int acCount, int totalScore, int penaltyMinutes) {
    }

    private static class ProblemRankStats {
        private int maxScore;
        private boolean accepted;
        private int wrongBeforeAccepted;
        private LocalDateTime firstAcceptedTime;
    }
}
