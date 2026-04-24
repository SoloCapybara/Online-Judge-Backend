package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.ProblemCaseMapper;
import com.dxd.oj.mapper.ProblemCategoryMapper;
import com.dxd.oj.mapper.ProblemMapper;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.model.dto.problem.ProblemCaseDTO;
import com.dxd.oj.model.dto.problem.ProblemCreateRequest;
import com.dxd.oj.model.dto.problem.ProblemPageQueryRequest;
import com.dxd.oj.model.dto.problem.ProblemUpdateRequest;
import com.dxd.oj.model.entity.Problem;
import com.dxd.oj.model.entity.ProblemCase;
import com.dxd.oj.model.entity.ProblemCategory;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.ProblemCaseVO;
import com.dxd.oj.model.vo.ProblemTagVO;
import com.dxd.oj.model.vo.ProblemVO;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final int MAX_TAG_COUNT = 10;
    private static final int MAX_TAG_NAME_LENGTH = 50;

    private final ProblemMapper problemMapper;
    private final ProblemCaseMapper problemCaseMapper;
    private final ProblemCategoryMapper problemCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final AuthService authService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public IPage<ProblemVO> pageProblems(ProblemPageQueryRequest queryRequest, String token) {
        ProblemPageQueryRequest safeQuery = queryRequest == null ? new ProblemPageQueryRequest() : queryRequest;
        long current = safeQuery.getCurrent() <= 0 ? 1 : safeQuery.getCurrent();
        long pageSize = safeQuery.getPageSize() <= 0 ? 10 : Math.min(safeQuery.getPageSize(), 100);
        LoginUserVO loginUser = getLoginUser(token);
        boolean manager = isManager(loginUser);
        String title = trimText(safeQuery.getTitle());

        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), Problem::getTitle, title)
                .eq(safeQuery.getCategoryId() != null, Problem::getCategoryId, safeQuery.getCategoryId())
                .eq(safeQuery.getDifficulty() != null, Problem::getDifficulty, safeQuery.getDifficulty())
                .inSql(safeQuery.getTagId() != null && safeQuery.getTagId() > 0,
                        Problem::getId,
                        "select problem_id from problem_tag_rel where tag_id = " + safeQuery.getTagId());

        if (manager) {
            wrapper.eq(safeQuery.getStatus() != null, Problem::getStatus, safeQuery.getStatus());
        } else {
            wrapper.eq(Problem::getStatus, STATUS_PUBLISHED);
        }
        wrapper.orderByDesc(Problem::getUpdateTime);

        Page<Problem> page = problemMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Page<ProblemVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ProblemVO> records = page.getRecords().stream().map(this::toProblemVO).toList();
        fillProblemExtraInfo(records);
        result.setRecords(records);
        return result;
    }

    @Override
    public ProblemVO getProblemDetail(Long problemId, String token) {
        if (problemId == null || problemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不合法");
        }
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }

        LoginUserVO loginUser = getLoginUser(token);
        if (!isManager(loginUser) && (problem.getStatus() == null || problem.getStatus() != STATUS_PUBLISHED)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该题目");
        }
        ProblemVO vo = toProblemVO(problem);
        fillProblemExtraInfo(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProblem(ProblemCreateRequest request, String token) {
        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);
        validateJudgeMode(request.getJudgeMode());
        validateCategory(request.getCategoryId());

        Problem problem = new Problem();
        problem.setTitle(trimText(request.getTitle()));
        problem.setCategoryId(request.getCategoryId());
        problem.setContent(trimText(request.getContent()));
        problem.setInputDesc(trimText(request.getInputDesc()));
        problem.setOutputDesc(trimText(request.getOutputDesc()));
        problem.setSampleInput(trimText(request.getSampleInput()));
        problem.setSampleOutput(trimText(request.getSampleOutput()));
        problem.setHint(trimText(request.getHint()));
        problem.setDifficulty(request.getDifficulty());
        problem.setTimeLimit(request.getTimeLimit());
        problem.setMemoryLimit(request.getMemoryLimit());
        problem.setJudgeMode(trimText(request.getJudgeMode()).toUpperCase());
        problem.setSubmitCount(0);
        problem.setAcceptCount(0);
        problem.setStatus(request.getStatus());
        problem.setCreatorId(loginUser.getId());

        int rows = problemMapper.insert(problem);
        if (rows <= 0 || problem.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建题目失败");
        }
        replaceCases(problem.getId(), request.getCaseList());
        replaceTags(problem.getId(), request.getTagNames());
        log.info("题库-创建题目成功：problemId={}, operator={}", problem.getId(), loginUser.getUsername());
        return problem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProblem(Long problemId, ProblemUpdateRequest request, String token) {
        if (problemId == null || problemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);
        validateJudgeMode(request.getJudgeMode());
        validateCategory(request.getCategoryId());

        Problem exists = problemMapper.selectById(problemId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        checkCreatorOrAdmin(loginUser, exists.getCreatorId());

        exists.setTitle(trimText(request.getTitle()));
        exists.setCategoryId(request.getCategoryId());
        exists.setContent(trimText(request.getContent()));
        exists.setInputDesc(trimText(request.getInputDesc()));
        exists.setOutputDesc(trimText(request.getOutputDesc()));
        exists.setSampleInput(trimText(request.getSampleInput()));
        exists.setSampleOutput(trimText(request.getSampleOutput()));
        exists.setHint(trimText(request.getHint()));
        exists.setDifficulty(request.getDifficulty());
        exists.setTimeLimit(request.getTimeLimit());
        exists.setMemoryLimit(request.getMemoryLimit());
        exists.setJudgeMode(trimText(request.getJudgeMode()).toUpperCase());
        exists.setStatus(request.getStatus());

        int rows = problemMapper.updateById(exists);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新题目失败");
        }
        replaceCases(problemId, request.getCaseList());
        replaceTags(problemId, request.getTagNames());
        log.info("题库-更新题目成功：problemId={}, operator={}", problemId, loginUser.getUsername());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProblem(Long problemId, String token) {
        if (problemId == null || problemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);

        Problem exists = problemMapper.selectById(problemId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        checkCreatorOrAdmin(loginUser, exists.getCreatorId());

        // 第一段删除：若未下线，则先逻辑下线
        if (exists.getStatus() == null || exists.getStatus() != STATUS_OFFLINE) {
            exists.setStatus(STATUS_OFFLINE);
            int rows = problemMapper.updateById(exists);
            if (rows <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下线题目失败");
            }
            log.info("题库-删除题目第一阶段(逻辑下线)成功：problemId={}, operator={}", problemId, loginUser.getUsername());
            return true;
        }

        // 第二段删除：已下线时可物理删除（需校验关键关联）
        long submissionCount = queryCount("select count(1) from submission where problem_id = ?", problemId);
        if (submissionCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已有提交记录，不能物理删除");
        }
        long contestCount = queryCount("select count(1) from contest_problem where problem_id = ?", problemId);
        if (contestCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已被比赛/作业引用，不能物理删除");
        }

        // 可安全删除的附属关系先清理
        jdbcTemplate.update("delete from problem_case where problem_id = ?", problemId);
        jdbcTemplate.update("delete from problem_tag_rel where problem_id = ?", problemId);

        int rows = problemMapper.deleteById(problemId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "物理删除题目失败");
        }
        log.info("题库-删除题目第二阶段(物理删除)成功：problemId={}, operator={}", problemId, loginUser.getUsername());
        return true;
    }

    @Override
    public List<ProblemCaseVO> listProblemCases(Long problemId, String token) {
        if (problemId == null || problemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不合法");
        }
        LoginUserVO loginUser = getLoginUser(token);
        checkManagerRole(loginUser);

        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        checkCreatorOrAdmin(loginUser, problem.getCreatorId());

        List<ProblemCase> cases = problemCaseMapper.selectList(new LambdaQueryWrapper<ProblemCase>()
                .eq(ProblemCase::getProblemId, problemId)
                .orderByAsc(ProblemCase::getCaseNo));
        if (CollectionUtils.isEmpty(cases)) {
            return List.of();
        }
        return cases.stream().map(this::toProblemCaseVO).toList();
    }

    @Override
    public List<ProblemTagVO> listProblemTags() {
        return jdbcTemplate.query(
                "select id, tag_name from problem_tag order by tag_name asc",
                (rs, rowNum) -> toProblemTagVO(rs.getLong("id"), rs.getString("tag_name"))
        );
    }

    private void replaceCases(Long problemId, List<ProblemCaseDTO> caseList) {
        problemCaseMapper.delete(new LambdaQueryWrapper<ProblemCase>().eq(ProblemCase::getProblemId, problemId));
        if (CollectionUtils.isEmpty(caseList)) {
            return;
        }

        List<ProblemCaseDTO> sortedCases = new ArrayList<>(caseList);
        sortedCases.sort(Comparator.comparing(dto -> dto.getCaseNo() == null ? Integer.MAX_VALUE : dto.getCaseNo()));

        int index = 1;
        for (ProblemCaseDTO dto : sortedCases) {
            ProblemCase problemCase = new ProblemCase();
            problemCase.setProblemId(problemId);
            problemCase.setCaseNo(dto.getCaseNo() == null ? index : dto.getCaseNo());
            problemCase.setInputData(dto.getInputData());
            problemCase.setOutputData(dto.getOutputData());
            problemCase.setScore(dto.getScore() == null ? 0 : dto.getScore());
            problemCase.setIsSample(dto.getIsSample() == null ? 0 : dto.getIsSample());
            problemCaseMapper.insert(problemCase);
            index++;
        }
    }

    private void replaceTags(Long problemId, List<String> tagNames) {
        jdbcTemplate.update("delete from problem_tag_rel where problem_id = ?", problemId);
        List<String> normalizedNames = normalizeTagNames(tagNames);
        if (normalizedNames.isEmpty()) {
            return;
        }

        Map<String, Long> tagIdMap = findTagIdMap(normalizedNames);
        for (String tagName : normalizedNames) {
            if (!tagIdMap.containsKey(tagName)) {
                try {
                    jdbcTemplate.update(
                            "insert into problem_tag (id, tag_name) values (?, ?)",
                            IdWorker.getId(),
                            tagName
                    );
                } catch (DuplicateKeyException ignored) {
                    // Another request may have created the same tag; re-query below.
                }
            }
        }

        tagIdMap = findTagIdMap(normalizedNames);
        for (String tagName : normalizedNames) {
            Long tagId = tagIdMap.get(tagName);
            if (tagId == null) {
                continue;
            }
            jdbcTemplate.update(
                    "insert ignore into problem_tag_rel (id, problem_id, tag_id) values (?, ?, ?)",
                    IdWorker.getId(),
                    problemId,
                    tagId
            );
        }
    }

    private List<String> normalizeTagNames(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return List.of();
        }
        List<String> names = tagNames.stream()
                .map(this::trimText)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (names.size() > MAX_TAG_COUNT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目标签最多10个");
        }
        for (String name : names) {
            if (name.length() > MAX_TAG_NAME_LENGTH) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称长度不能超过50位");
            }
        }
        return names;
    }

    private Map<String, Long> findTagIdMap(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return Map.of();
        }
        String placeholders = tagNames.stream().map(item -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
                "select id, tag_name from problem_tag where tag_name in (" + placeholders + ")",
                rs -> {
                    Map<String, Long> result = new java.util.LinkedHashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString("tag_name"), rs.getLong("id"));
                    }
                    return result;
                },
                tagNames.toArray()
        );
    }

    private void validateJudgeMode(String judgeMode) {
        if (!StringUtils.hasText(judgeMode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题模式不能为空");
        }
        String value = judgeMode.trim().toUpperCase();
        if (!"ACM".equals(value) && !"OI".equals(value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题模式仅支持 ACM 或 OI");
        }
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        ProblemCategory category = problemCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类不存在");
        }
        if (category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类已停用");
        }
    }

    private String trimText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
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
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅题目创建者可操作");
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

    private ProblemVO toProblemVO(Problem problem) {
        ProblemVO vo = new ProblemVO();
        vo.setId(problem.getId());
        vo.setTitle(problem.getTitle());
        vo.setCategoryId(problem.getCategoryId());
        vo.setContent(problem.getContent());
        vo.setInputDesc(problem.getInputDesc());
        vo.setOutputDesc(problem.getOutputDesc());
        vo.setSampleInput(problem.getSampleInput());
        vo.setSampleOutput(problem.getSampleOutput());
        vo.setHint(problem.getHint());
        vo.setDifficulty(problem.getDifficulty());
        vo.setTimeLimit(problem.getTimeLimit());
        vo.setMemoryLimit(problem.getMemoryLimit());
        vo.setJudgeMode(problem.getJudgeMode());
        vo.setSubmitCount(problem.getSubmitCount());
        vo.setAcceptCount(problem.getAcceptCount());
        vo.setAcceptRate(calcAcceptRate(problem.getSubmitCount(), problem.getAcceptCount()));
        vo.setStatus(problem.getStatus());
        vo.setCreatorId(problem.getCreatorId());
        vo.setCreateTime(problem.getCreateTime());
        vo.setUpdateTime(problem.getUpdateTime());
        return vo;
    }

    private void fillProblemExtraInfo(List<ProblemVO> problemList) {
        if (CollectionUtils.isEmpty(problemList)) {
            return;
        }
        List<Long> categoryIds = problemList.stream()
                .map(ProblemVO::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ProblemCategory> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : problemCategoryMapper.selectBatchIds(categoryIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(ProblemCategory::getId, Function.identity(), (a, b) -> a));

        List<Long> creatorIds = problemList.stream()
                .map(ProblemVO::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, SysUser> creatorMap = creatorIds.isEmpty()
                ? Map.of()
                : sysUserMapper.selectBatchIds(creatorIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));

        for (ProblemVO problemVO : problemList) {
            if (problemVO.getCategoryId() != null) {
                ProblemCategory category = categoryMap.get(problemVO.getCategoryId());
                if (category != null) {
                    problemVO.setCategoryName(category.getCategoryName());
                }
            }
            if (problemVO.getCreatorId() != null) {
                SysUser creator = creatorMap.get(problemVO.getCreatorId());
                if (creator != null) {
                    problemVO.setCreatorUsername(creator.getUsername());
                    problemVO.setCreatorRealName(creator.getRealName());
                }
            }
        }
        fillProblemTags(problemList);
    }

    private void fillProblemTags(List<ProblemVO> problemList) {
        List<Long> problemIds = problemList.stream()
                .map(ProblemVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (problemIds.isEmpty()) {
            return;
        }

        String placeholders = problemIds.stream().map(item -> "?").collect(Collectors.joining(","));
        Map<Long, List<ProblemTagVO>> tagMap = jdbcTemplate.query(
                "select r.problem_id, t.id, t.tag_name " +
                        "from problem_tag_rel r join problem_tag t on t.id = r.tag_id " +
                        "where r.problem_id in (" + placeholders + ") order by t.tag_name asc",
                rs -> {
                    Map<Long, List<ProblemTagVO>> result = new java.util.LinkedHashMap<>();
                    while (rs.next()) {
                        Long problemId = rs.getLong("problem_id");
                        result.computeIfAbsent(problemId, key -> new ArrayList<>())
                                .add(toProblemTagVO(rs.getLong("id"), rs.getString("tag_name")));
                    }
                    return result;
                },
                problemIds.toArray()
        );

        for (ProblemVO problemVO : problemList) {
            List<ProblemTagVO> tags = tagMap.getOrDefault(problemVO.getId(), List.of());
            problemVO.setTagList(tags);
            problemVO.setTagNames(tags.stream().map(ProblemTagVO::getTagName).toList());
        }
    }

    private ProblemTagVO toProblemTagVO(Long id, String tagName) {
        ProblemTagVO vo = new ProblemTagVO();
        vo.setId(id);
        vo.setTagName(tagName);
        return vo;
    }

    private ProblemCaseVO toProblemCaseVO(ProblemCase problemCase) {
        ProblemCaseVO vo = new ProblemCaseVO();
        vo.setId(problemCase.getId());
        vo.setCaseNo(problemCase.getCaseNo());
        vo.setInputData(problemCase.getInputData());
        vo.setOutputData(problemCase.getOutputData());
        vo.setScore(problemCase.getScore());
        vo.setIsSample(problemCase.getIsSample());
        return vo;
    }

    private Double calcAcceptRate(Integer submitCount, Integer acceptCount) {
        if (submitCount == null || submitCount <= 0 || acceptCount == null || acceptCount <= 0) {
            return 0D;
        }
        double rate = acceptCount * 100.0 / submitCount;
        return Math.round(rate * 100.0) / 100.0;
    }

    private long queryCount(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0L : count;
    }
}
