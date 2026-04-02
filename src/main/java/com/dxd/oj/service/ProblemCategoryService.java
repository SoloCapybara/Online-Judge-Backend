package com.dxd.oj.service;

import com.dxd.oj.model.dto.problem.ProblemCategoryCreateRequest;
import com.dxd.oj.model.dto.problem.ProblemCategoryUpdateRequest;
import com.dxd.oj.model.vo.ProblemCategoryVO;

import java.util.List;

public interface ProblemCategoryService {

    List<ProblemCategoryVO> listCategories(Boolean onlyEnabled, String token);

    Long createCategory(ProblemCategoryCreateRequest request, String token);

    boolean updateCategory(Long categoryId, ProblemCategoryUpdateRequest request, String token);

    boolean deleteCategory(Long categoryId, String token);
}
