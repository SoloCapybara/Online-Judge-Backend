package com.dxd.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.model.dto.admin.AdminSystemConfigPageQueryRequest;
import com.dxd.oj.model.dto.admin.AdminSystemConfigUpdateRequest;
import com.dxd.oj.model.vo.AdminSystemConfigVO;

import java.util.List;

public interface AdminSystemConfigService {

    IPage<AdminSystemConfigVO> pageConfigs(AdminSystemConfigPageQueryRequest queryRequest, String token);

    List<String> listCategories(String token);

    boolean updateConfig(Long configId, AdminSystemConfigUpdateRequest request, String token);
}
