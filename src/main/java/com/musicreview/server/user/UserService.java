package com.musicreview.server.user;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.user.domain.User;
import com.musicreview.server.user.domain.UserRepository;
import com.musicreview.server.user.dto.ChangePasswordRequest;
import com.musicreview.server.user.dto.UpdateMeRequest;
import com.musicreview.server.user.dto.UserResponse;
import com.musicreview.server.user.dto.WithdrawRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
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

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        verifyPassword(user, request.currentPassword());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    /** 계정과 내가 쓴 리뷰(태그 포함)를 모두 삭제 */
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = getUser(userId);
        verifyPassword(user, request.password());
        reviewRepository.deleteAll(reviewRepository.findByUserId(userId));
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
