package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.ProblemCategoryMapper;
import com.dxd.oj.model.dto.problem.ProblemCategoryCreateRequest;
import com.dxd.oj.model.dto.problem.ProblemCategoryUpdateRequest;
import com.dxd.oj.model.entity.ProblemCategory;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.ProblemCategoryVO;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.ProblemCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemCategoryServiceImpl implements ProblemCategoryService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";

    private final ProblemCategoryMapper problemCategoryMapper;
    private final AuthService authService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ProblemCategoryVO> listCategories(Boolean onlyEnabled, String token) {
        LoginUserVO loginUser = authService.getLoginUser(token);
        boolean manager = isManager(loginUser);

        LambdaQueryWrapper<ProblemCategory> wrapper = new LambdaQueryWrapper<>();
        if (!manager || Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(ProblemCategory::getStatus, 1);
        }
        wrapper.orderByAsc(ProblemCategory::getSortNo).orderByAsc(ProblemCategory::getId);

        List<ProblemCategory> categoryList = problemCategoryMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(categoryList)) {
            return List.of();
        }

        Map<Long, ProblemCategory> categoryMap = categoryList.stream()
                .collect(Collectors.toMap(ProblemCategory::getId, Function.identity(), (a, b) -> a));

        return categoryList.stream().map(category -> {
            ProblemCategoryVO vo = new ProblemCategoryVO();
            vo.setId(category.getId());
            vo.setCategoryName(category.getCategoryName());
            vo.setParentId(category.getParentId());
            vo.setSortNo(category.getSortNo());
            vo.setStatus(category.getStatus());
            vo.setCreateTime(category.getCreateTime());
            vo.setUpdateTime(category.getUpdateTime());
            if (category.getParentId() != null) {
                ProblemCategory parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    vo.setParentName(parent.getCategoryName());
                }
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(ProblemCategoryCreateRequest request, String token) {
        LoginUserVO loginUser = authService.getLoginUser(token);
        checkManagerRole(loginUser);

        String categoryName = trimText(request.getCategoryName());
        checkCategoryNameUnique(categoryName, null);
        validateParentCategory(request.getParentId());

        ProblemCategory category = new ProblemCategory();
        category.setCategoryName(categoryName);
        category.setParentId(request.getParentId());
        category.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        int rows = problemCategoryMapper.insert(category);
        if (rows <= 0 || category.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建分类失败");
        }
        log.info("题库分类-创建成功：categoryId={}, operator={}", category.getId(), loginUser.getUsername());
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCategory(Long categoryId, ProblemCategoryUpdateRequest request, String token) {
        if (categoryId == null || categoryId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不合法");
        }
        LoginUserVO loginUser = authService.getLoginUser(token);
        checkManagerRole(loginUser);

        ProblemCategory exists = problemCategoryMapper.selectById(categoryId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在");
        }

        String categoryName = trimText(request.getCategoryName());
        checkCategoryNameUnique(categoryName, categoryId);
        validateParentCategory(request.getParentId());
        if (Objects.equals(categoryId, request.getParentId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分类不能是自己");
        }

        exists.setCategoryName(categoryName);
        exists.setParentId(request.getParentId());
        exists.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        exists.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        int rows = problemCategoryMapper.updateById(exists);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新分类失败");
        }
        log.info("题库分类-更新成功：categoryId={}, operator={}", categoryId, loginUser.getUsername());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long categoryId, String token) {
        if (categoryId == null || categoryId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不合法");
        }
        LoginUserVO loginUser = authService.getLoginUser(token);
        checkManagerRole(loginUser);

        ProblemCategory exists = problemCategoryMapper.selectById(categoryId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在");
        }
        // 第一段删除：若未停用，则先逻辑停用
        if (exists.getStatus() == null || exists.getStatus() != 0) {
            exists.setStatus(0);
            int rows = problemCategoryMapper.updateById(exists);
            if (rows <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "停用分类失败");
            }
            log.info("题库分类-删除第一阶段(逻辑停用)成功：categoryId={}, operator={}", categoryId, loginUser.getUsername());
            return true;
        }

        // 第二段删除：已停用时尝试物理删除（需校验无关联）
        long childCount = queryCount("select count(1) from problem_category where parent_id = ?", categoryId);
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "存在子分类，不能物理删除");
        }
        long problemCount = queryCount("select count(1) from problem where category_id = ?", categoryId);
        if (problemCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "分类下仍有关联题目，不能物理删除");
        }

        int rows = problemCategoryMapper.deleteById(categoryId);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "物理删除分类失败");
        }
        log.info("题库分类-删除第二阶段(物理删除)成功：categoryId={}, operator={}", categoryId, loginUser.getUsername());
        return true;
    }

    private void checkCategoryNameUnique(String categoryName, Long excludeId) {
        LambdaQueryWrapper<ProblemCategory> wrapper = new LambdaQueryWrapper<ProblemCategory>()
                .eq(ProblemCategory::getCategoryName, categoryName);
        if (excludeId != null) {
            wrapper.ne(ProblemCategory::getId, excludeId);
        }
        Long count = problemCategoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称已存在");
        }
    }

    private void validateParentCategory(Long parentId) {
        if (parentId == null) {
            return;
        }
        ProblemCategory parent = problemCategoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分类不存在");
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

    private boolean isManager(LoginUserVO loginUser) {
        return loginUser != null
                && !CollectionUtils.isEmpty(loginUser.getRoles())
                && (loginUser.getRoles().contains(ROLE_ADMIN) || loginUser.getRoles().contains(ROLE_TEACHER));
    }

    private long queryCount(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0L : count;
    }
}
