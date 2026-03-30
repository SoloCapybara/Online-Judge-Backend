package com.dxd.oj.service;

import com.dxd.oj.model.dto.user.UserProfileUpdateRequest;
import com.dxd.oj.model.dto.user.UserPasswordUpdateRequest;
import com.dxd.oj.model.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileVO getCurrentUserProfile(String token);

    UserProfileVO updateCurrentUserProfile(String token, UserProfileUpdateRequest request);

    UserProfileVO uploadAvatar(String token, MultipartFile file);

    boolean updatePassword(String token, UserPasswordUpdateRequest request);
}
