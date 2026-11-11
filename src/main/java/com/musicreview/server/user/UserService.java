package com.musicreview.server.user;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.user.domain.User;
import com.musicreview.server.user.domain.UserRepository;
import com.musicreview.server.user.dto.ChangePasswordRequest;
import com.musicreview.server.user.dto.UpdateMeRequest;
import com.musicreview.server.user.dto.UpdateSettingsRequest;
import com.musicreview.server.user.dto.UserSettingsResponse;
import com.musicreview.server.user.dto.UserResponse;
import com.musicreview.server.user.dto.WithdrawRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateMeRequest request) {
        User user = getUser(userId);
        if (request.nickname() != null) {
            user.changeNickname(request.nickname().strip());
        }
        if (request.profileImageUrl() != null) {
            user.changeProfileImageUrl(request.profileImageUrl());
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(Long userId) {
        return UserSettingsResponse.from(getUser(userId));
    }

    /** 보낸 항목만 바꿈. 아무것도 안 보내면 현재 값을 그대로 돌려줌 */
    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UpdateSettingsRequest request) {
        User user = getUser(userId);
        user.changeSettings(request.themeMode(), request.textScale());
        return UserSettingsResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        verifyPassword(user, request.currentPassword());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    /** 계정을 삭제하고, 내가 쓴 데이터는 UserWithdrawnEvent를 받는 쪽(리뷰 등)이 함께 삭제 */
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = getUser(userId);
        verifyPassword(user, request.password());
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
        userRepository.delete(user);
    }

    /** 다른 서비스에서도 쓰는 사용자 조회. 탈퇴 후 남은 토큰이면 404 */
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    void verifyPassword(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }
}
