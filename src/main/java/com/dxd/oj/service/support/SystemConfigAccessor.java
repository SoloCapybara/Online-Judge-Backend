package com.dxd.oj.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.mapper.SysConfigMapper;
import com.dxd.oj.model.entity.SysConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SystemConfigAccessor {

    private static final int STATUS_ENABLED = 1;

    private final SysConfigMapper sysConfigMapper;

    public int getInt(String configKey, int fallback, int min, int max) {
        String value = getEnabledConfigValue(configKey);
        if (!StringUtils.hasText(value)) {
            return clamp(fallback, min, max);
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return clamp(parsed, min, max);
        } catch (NumberFormatException e) {
            return clamp(fallback, min, max);
        }
    }

    private String getEnabledConfigValue(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return null;
        }
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey.trim())
                .eq(SysConfig::getStatus, STATUS_ENABLED)
                .last("limit 1"));
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return null;
        }
        return config.getConfigValue().trim();
    }

    private int clamp(int value, int min, int max) {
        int safeMin = Math.min(min, max);
        int safeMax = Math.max(min, max);
        if (value < safeMin) {
            return safeMin;
        }
        if (value > safeMax) {
            return safeMax;
        }
        return value;
    }
}
