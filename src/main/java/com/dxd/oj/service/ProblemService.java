package com.dxd.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.model.dto.problem.ProblemCreateRequest;
import com.dxd.oj.model.dto.problem.ProblemPageQueryRequest;
import com.dxd.oj.model.dto.problem.ProblemUpdateRequest;
import com.dxd.oj.model.vo.ProblemCaseVO;
import com.dxd.oj.model.vo.ProblemVO;

import java.util.List;

public interface ProblemService {

    IPage<ProblemVO> pageProblems(ProblemPageQueryRequest queryRequest, String token);

    ProblemVO getProblemDetail(Long problemId, String token);

    Long createProblem(ProblemCreateRequest request, String token);

    boolean updateProblem(Long problemId, ProblemUpdateRequest request, String token);

    boolean deleteProblem(Long problemId, String token);

    List<ProblemCaseVO> listProblemCases(Long problemId, String token);
}
