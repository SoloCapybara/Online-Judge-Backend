package com.dxd.oj.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "管理员-更新用户角色请求")
public class AdminUserRolesUpdateRequest {

    @NotEmpty(message = "角色列表不能为空")
    @Size(min = 1, max = 1, message = "角色仅支持单选")
    @Schema(description = "角色编码列表，例如 [STUDENT] / [TEACHER] / [ADMIN]")
    private List<String> roleCodes;
}
