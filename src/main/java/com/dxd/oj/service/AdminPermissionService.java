package com.dxd.oj.service;

import com.dxd.oj.model.dto.admin.AdminRolePermissionUpdateRequest;
import com.dxd.oj.model.vo.AdminPermissionVO;
import com.dxd.oj.model.vo.AdminRolePermissionVO;

import java.util.List;

public interface AdminPermissionService {

    List<AdminPermissionVO> listPermissions(String token);

    List<AdminRolePermissionVO> listRolePermissions(String token);

    boolean updateRolePermissions(AdminRolePermissionUpdateRequest request, String token);
}
