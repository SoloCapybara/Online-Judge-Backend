package com.dxd.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.model.dto.contest.ContestBindProblemsRequest;
import com.dxd.oj.model.dto.contest.ContestCreateRequest;
import com.dxd.oj.model.dto.contest.ContestPageQueryRequest;
import com.dxd.oj.model.dto.contest.ContestRankPageQueryRequest;
import com.dxd.oj.model.dto.contest.ContestRegisterRequest;
import com.dxd.oj.model.dto.contest.ContestScorePageQueryRequest;
import com.dxd.oj.model.dto.contest.ContestStatusUpdateRequest;
import com.dxd.oj.model.vo.ContestCreateVO;
import com.dxd.oj.model.vo.ContestDetailVO;
import com.dxd.oj.model.vo.ContestRankVO;
import com.dxd.oj.model.vo.ContestScoreVO;
import com.dxd.oj.model.vo.ContestVO;

public interface ContestService {

    ContestCreateVO createContest(ContestCreateRequest request, String token);

    IPage<ContestVO> pageContests(ContestPageQueryRequest queryRequest, String token);

    ContestDetailVO getContestDetail(Long contestId, String token);

    boolean bindProblems(Long contestId, ContestBindProblemsRequest request, String token);

    boolean registerContest(Long contestId, ContestRegisterRequest request, String token);

    boolean updateContestStatus(Long contestId, ContestStatusUpdateRequest request, String token);

    IPage<ContestRankVO> pageContestRank(Long contestId, ContestRankPageQueryRequest queryRequest, String token);

    IPage<ContestScoreVO> pageContestScore(Long contestId, ContestScorePageQueryRequest queryRequest, String token);

    byte[] exportContestScoreCsv(Long contestId, ContestScorePageQueryRequest queryRequest, String token);

    byte[] exportContestScoreExcel(Long contestId, ContestScorePageQueryRequest queryRequest, String token);

    boolean deleteContest(Long contestId, String token);
}
