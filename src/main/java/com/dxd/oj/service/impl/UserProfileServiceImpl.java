package com.dxd.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.mapper.SysUserMapper;
import com.dxd.oj.mapper.SubmissionMapper;
import com.dxd.oj.model.dto.user.UserPasswordUpdateRequest;
import com.dxd.oj.model.dto.user.UserProfileUpdateRequest;
import com.dxd.oj.model.entity.Submission;
import com.dxd.oj.model.entity.SysUser;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.UserProfileVO;
import com.dxd.oj.model.vo.UserProfileStatsVO;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String STATUS_ACCEPTED = "ACCEPTED";

    private final AuthService authService;
    private final SysUserMapper sysUserMapper;
    private final SubmissionMapper submissionMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${oj.file.upload-dir:${user.home}/online-judge/uploads}")
    private String uploadDir;

    @Value("${oj.file.public-url-prefix:/uploads}")
    private String publicUrlPrefix;

    @Value("${oj.file.max-avatar-size-mb:5}")
    private int maxAvatarSizeMb;

    @Override
    public UserProfileVO getCurrentUserProfile(String token) {
        Long userId = getLoginUserId(token);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return toUserProfileVO(user);
    }

    @Override
    public UserProfileStatsVO getCurrentUserStats(String token) {
        Long userId = getLoginUserId(token);
        long submitCount = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)));
        long acceptedCount = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(Submission::getStatus, STATUS_ACCEPTED)));
        long solvedCount = querySolvedCount(userId);
        List<LocalDate> activeDays = queryActiveDays(userId);

        UserProfileStatsVO stats = new UserProfileStatsVO();
        stats.setSolvedCount(solvedCount);
        stats.setSubmitCount(submitCount);
        stats.setAcceptedCount(acceptedCount);
        stats.setAcRate(calcAcRate(submitCount, acceptedCount));
        stats.setCurrentStreakDays(calcCurrentStreak(activeDays));
        stats.setLongestStreakDays(calcLongestStreak(activeDays));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateCurrentUserProfile(String token, UserProfileUpdateRequest request) {
        Long userId = getLoginUserId(token);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String realName = trimToNull(request.getRealName());
        String studentNo = trimToNull(request.getStudentNo());
        String email = trimToNull(request.getEmail());
        String phone = trimToNull(request.getPhone());
        String avatar = trimToNull(request.getAvatar());
        Integer gender = request.getGender();

        checkUniqueField(userId, email, phone, studentNo);

        user.setRealName(realName);
        user.setStudentNo(studentNo);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAvatar(avatar);
        if (gender != null) {
            user.setGender(gender);
        }

        int rows = sysUserMapper.updateById(user);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新个人信息失败");
        }
        log.info("用户中心-更新个人信息成功：userId={}", userId);
        return toUserProfileVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO uploadAvatar(String token, MultipartFile file) {
        Long userId = getLoginUserId(token);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        validateAvatarFile(file);
        String avatarUrl = saveAvatarFile(file);
        user.setAvatar(avatarUrl);
        int rows = sysUserMapper.updateById(user);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像更新失败");
        }
        log.info("用户中心-上传头像成功：userId={}, avatar={}", userId, avatarUrl);
        return toUserProfileVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(String token, UserPasswordUpdateRequest request) {
        Long userId = getLoginUserId(token);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getOldPassword())
                || !StringUtils.hasText(request.getNewPassword())
                || !StringUtils.hasText(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }
        if (!request.getNewPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }
        if (request.getNewPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能小于6位");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        int rows = sysUserMapper.updateById(user);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改密码失败");
        }
        log.info("用户中心-修改密码成功：userId={}", userId);
        return true;
    }

    private void checkUniqueField(Long userId, String email, String phone, String studentNo) {
        if (StringUtils.hasText(email)) {
            Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, email)
                    .ne(SysUser::getId, userId));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被占用");
            }
        }

        if (StringUtils.hasText(phone)) {
            Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, phone)
                    .ne(SysUser::getId, userId));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号已被占用");
            }
        }

        if (StringUtils.hasText(studentNo)) {
            Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getStudentNo, studentNo)
                    .ne(SysUser::getId, userId));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号已被占用");
            }
        }
    }

    private Long getLoginUserId(String token) {
        LoginUserVO loginUserVO = authService.getLoginUser(token);
        return loginUserVO.getId();
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件不能为空");
        }
        long maxSize = (long) maxAvatarSizeMb * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件大小不能超过" + maxAvatarSizeMb + "MB");
        }
        String ext = getFileExtension(file.getOriginalFilename());
        if (!isAllowedImageExt(ext)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像仅支持 jpg/jpeg/png/webp/gif 格式");
        }
    }

    private String saveAvatarFile(MultipartFile file) {
        String ext = getFileExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        LocalDate now = LocalDate.now();
        Path dir = Paths.get(uploadDir, "avatar", String.valueOf(now.getYear()), String.format("%02d", now.getMonthValue()));
        try {
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path filePath = dir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String relative = "/avatar/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + fileName;
            return normalizeUrlPrefix(publicUrlPrefix) + relative;
        } catch (IOException e) {
            log.error("用户中心-头像文件保存失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }
    }

    private String normalizeUrlPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String result = prefix.trim();
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }

    private boolean isAllowedImageExt(String ext) {
        if (!StringUtils.hasText(ext)) {
            return false;
        }
        String lower = ext.toLowerCase(Locale.ROOT);
        return "jpg".equals(lower)
                || "jpeg".equals(lower)
                || "png".equals(lower)
                || "webp".equals(lower)
                || "gif".equals(lower);
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private UserProfileVO toUserProfileVO(SysUser user) {
        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setId(user.getId());
        profileVO.setUsername(user.getUsername());
        profileVO.setRealName(user.getRealName());
        profileVO.setStudentNo(user.getStudentNo());
        profileVO.setEmail(user.getEmail());
        profileVO.setPhone(user.getPhone());
        profileVO.setAvatar(user.getAvatar());
        profileVO.setGender(user.getGender());
        profileVO.setStatus(user.getStatus());
        return profileVO;
    }

    private long querySolvedCount(Long userId) {
        Long value = jdbcTemplate.queryForObject(
                "select count(distinct problem_id) from submission where user_id = ? and status = ?",
                Long.class,
                userId,
                STATUS_ACCEPTED
        );
        return safeLong(value);
    }

    private List<LocalDate> queryActiveDays(Long userId) {
        return jdbcTemplate.query(
                "select distinct date(submit_time) as submit_day from submission where user_id = ? order by submit_day asc",
                (rs, rowNum) -> {
                    Date date = rs.getDate("submit_day");
                    return date == null ? null : date.toLocalDate();
                },
                userId
        ).stream().filter(day -> day != null).toList();
    }

    private double calcAcRate(long submitCount, long acceptedCount) {
        if (submitCount <= 0) {
            return 0D;
        }
        double rate = acceptedCount * 100D / submitCount;
        return Math.round(rate * 100D) / 100D;
    }

    private int calcCurrentStreak(List<LocalDate> activeDays) {
        if (activeDays == null || activeDays.isEmpty()) {
            return 0;
        }
        Set<LocalDate> daySet = new HashSet<>(activeDays);
        LocalDate cursor = LocalDate.now();
        int streak = 0;
        while (daySet.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int calcLongestStreak(List<LocalDate> activeDays) {
        if (activeDays == null || activeDays.isEmpty()) {
            return 0;
        }
        int best = 1;
        int current = 1;
        for (int i = 1; i < activeDays.size(); i++) {
            LocalDate prev = activeDays.get(i - 1);
            LocalDate now = activeDays.get(i);
            if (now.equals(prev.plusDays(1))) {
                current++;
            } else if (now.equals(prev)) {
                continue;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
        }
        return best;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }
}
