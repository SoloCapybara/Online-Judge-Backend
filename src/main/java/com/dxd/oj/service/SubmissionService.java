package com.dxd.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.model.dto.submission.SubmissionCreateRequest;
import com.dxd.oj.model.dto.submission.SubmissionPageQueryRequest;
import com.dxd.oj.model.vo.JudgeOverviewVO;
import com.dxd.oj.model.vo.SubmissionCaseVO;
import com.dxd.oj.model.vo.SubmissionCreateVO;
import com.dxd.oj.model.vo.SubmissionDetailVO;
import com.dxd.oj.model.vo.SubmissionVO;

import java.util.List;

public interface SubmissionService {

    SubmissionCreateVO createSubmission(SubmissionCreateRequest request, String token);

    IPage<SubmissionVO> pageSubmissions(SubmissionPageQueryRequest queryRequest, String token);

    SubmissionDetailVO getSubmissionDetail(Long submissionId, String token);

    List<SubmissionCaseVO> listSubmissionCases(Long submissionId, String token);

    boolean rejudgeSubmission(Long submissionId, String token);

    JudgeOverviewVO getJudgeOverview();
}
