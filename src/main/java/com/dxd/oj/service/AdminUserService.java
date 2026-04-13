package com.dxd.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dxd.oj.model.dto.admin.AdminUserPageQueryRequest;
import com.dxd.oj.model.dto.admin.AdminUserRolesUpdateRequest;
import com.dxd.oj.model.dto.admin.AdminUserStatusUpdateRequest;
import com.dxd.oj.model.dto.admin.AdminUserUpdateRequest;
import com.dxd.oj.model.vo.AdminRoleOptionVO;
import com.dxd.oj.model.vo.AdminUserVO;

import java.util.List;

public interface AdminUserService {

    IPage<AdminUserVO> pageUsers(AdminUserPageQueryRequest queryRequest, String token);

    List<AdminRoleOptionVO> listRoles(String token);

    boolean updateUserProfile(Long userId, AdminUserUpdateRequest request, String token);

    boolean updateUserStatus(Long userId, AdminUserStatusUpdateRequest request, String token);

    boolean updateUserRoles(Long userId, AdminUserRolesUpdateRequest request, String token);
}
