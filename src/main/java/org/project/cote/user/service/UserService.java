package org.project.cote.user.service;

import lombok.RequiredArgsConstructor;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.user.domain.User;
import org.project.cote.user.dto.UserResponse;
import org.project.cote.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeNickname(Long userId, String newNickname) {
        User user = findUser(userId);
        if (user.getNickname().equals(newNickname)) {
            return UserResponse.from(user);
        }
        if (userRepository.existsByNickname(newNickname)) {
            throw new ApiException(ErrorCode.NICKNAME_ALREADY_TAKEN);
        }
        user.changeNickname(newNickname);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 사전 existsByNickname 통과 후 다른 트랜잭션이 같은 닉네임을 점유한 경우
            throw new ApiException(ErrorCode.NICKNAME_ALREADY_TAKEN);
        }
        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
