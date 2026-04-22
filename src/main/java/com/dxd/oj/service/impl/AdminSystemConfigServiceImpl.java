package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.SysConfigMapper;
import com.dxd.oj.model.dto.admin.AdminSystemConfigPageQueryRequest;
import com.dxd.oj.model.dto.admin.AdminSystemConfigUpdateRequest;
import com.dxd.oj.model.entity.SysConfig;
import com.dxd.oj.model.vo.AdminSystemConfigVO;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.service.AdminSystemConfigService;
import com.dxd.oj.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminSystemConfigServiceImpl implements AdminSystemConfigService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final SysConfigMapper sysConfigMapper;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<AdminSystemConfigVO> pageConfigs(AdminSystemConfigPageQueryRequest queryRequest, String token) {
        requireAdmin(token);

        AdminSystemConfigPageQueryRequest safeQuery =
                queryRequest == null ? new AdminSystemConfigPageQueryRequest() : queryRequest;
        long current = safeQuery.getCurrent() <= 0 ? 1 : safeQuery.getCurrent();
        long pageSize = safeQuery.getPageSize() <= 0 ? 10 : Math.min(safeQuery.getPageSize(), 100);

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        String keyword = normalizeNullableText(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysConfig::getConfigKey, keyword)
                    .or()
                    .like(SysConfig::getConfigName, keyword)
                    .or()
                    .like(SysConfig::getDescription, keyword));
        }

        String category = normalizeNullableText(safeQuery.getCategory());
        if (StringUtils.hasText(category)) {
            wrapper.eq(SysConfig::getCategory, category);
        }

        Integer status = safeQuery.getStatus();
        if (status != null) {
            if (!Objects.equals(status, 0) && !Objects.equals(status, 1)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误");
            }
            wrapper.eq(SysConfig::getStatus, status);
        }

        wrapper.orderByAsc(SysConfig::getCategory, SysConfig::getConfigKey, SysConfig::getId);
        IPage<SysConfig> page = sysConfigMapper.selectPage(new Page<>(current, pageSize), wrapper);

        Page<AdminSystemConfigVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollectionUtils.isEmpty(page.getRecords())) {
            result.setRecords(List.of());
            return result;
        }
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public List<String> listCategories(String token) {
        requireAdmin(token);
        List<SysConfig> configList = sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .select(SysConfig::getCategory)
                .orderByAsc(SysConfig::getCategory));
        if (CollectionUtils.isEmpty(configList)) {
            return List.of();
        }
        return configList.stream()
                .map(SysConfig::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(Long configId, AdminSystemConfigUpdateRequest request, String token) {
        LoginUserVO loginUser = requireAdmin(token);
        if (configId == null || configId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "配置ID不合法");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        SysConfig config = sysConfigMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "配置不存在");
        }

        boolean changed = false;
        if (request.getConfigValue() != null) {
            String nextValue = request.getConfigValue().trim();
            validateConfigValue(config.getValueType(), nextValue);
            config.setConfigValue(nextValue);
            changed = true;
        }
        if (request.getDescription() != null) {
            config.setDescription(normalizeNullableText(request.getDescription()));
            changed = true;
        }
        if (request.getStatus() != null) {
            Integer nextStatus = request.getStatus();
            if (!Objects.equals(nextStatus, 0) && !Objects.equals(nextStatus, 1)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误");
            }
            config.setStatus(nextStatus);
            changed = true;
        }
        if (!changed) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可更新的字段");
        }

        config.setUpdateBy(loginUser.getId());
        return sysConfigMapper.updateById(config) > 0;
    }

    private LoginUserVO requireAdmin(String token) {
        LoginUserVO loginUser = authService.getLoginUser(token);
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getRoles()) || !loginUser.getRoles().contains(ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
        }
        return loginUser;
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private void validateConfigValue(String valueType, String configValue) {
        String normalizedType = StringUtils.hasText(valueType)
                ? valueType.trim().toUpperCase(Locale.ROOT)
                : "STRING";
        String value = configValue == null ? "" : configValue.trim();
        try {
            switch (normalizedType) {
                case "INT", "INTEGER" -> Integer.parseInt(value);
                case "LONG" -> Long.parseLong(value);
                case "DOUBLE", "FLOAT", "DECIMAL" -> new BigDecimal(value);
                case "BOOL", "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(value)
                            && !"false".equalsIgnoreCase(value)
                            && !"1".equals(value)
                            && !"0".equals(value)) {
                        throw new IllegalArgumentException("布尔值仅支持 true/false/1/0");
                    }
                }
                case "JSON" -> objectMapper.readTree(value);
                default -> {
                    // STRING 等类型默认放行
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "配置值类型不匹配: " + normalizedType);
        }
    }

    private AdminSystemConfigVO toVO(SysConfig config) {
        AdminSystemConfigVO vo = new AdminSystemConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigName(config.getConfigName());
        vo.setConfigValue(config.getConfigValue());
        vo.setValueType(config.getValueType());
        vo.setCategory(config.getCategory());
        vo.setDescription(config.getDescription());
        vo.setStatus(config.getStatus());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
