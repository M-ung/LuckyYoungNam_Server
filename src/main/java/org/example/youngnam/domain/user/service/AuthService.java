package org.example.youngnam.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.youngnam.auth.jwt.JwtProvider;
import org.example.youngnam.auth.jwt.Token;
import org.example.youngnam.domain.refreshtoken.entitiy.RefreshToken;
import org.example.youngnam.domain.refreshtoken.repository.RefreshTokenRepository;
import org.example.youngnam.domain.user.dto.UserLoginRes;
import org.example.youngnam.domain.user.entity.User;
import org.example.youngnam.domain.user.repository.UserRepository;
import org.example.youngnam.external.feign.kakao.KakaoFeignProvider;
import org.example.youngnam.global.exception.exceptions.EntityNotFoundException;
import org.example.youngnam.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final KakaoFeignProvider kakaoFeignProvider;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public UserLoginRes login(final String authorizationCode) {
        final String socialId = kakaoFeignProvider.login(authorizationCode);

        //이미 유저 있을 경우
        if (isExistUser(socialId)) {
            final User foundUser = userRepository.findBySocialId(socialId).orElseThrow(
                    () -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND)
            );
            refreshTokenRepository.deleteByUserId(foundUser.getUserId());

            return issueToken(foundUser.getUserId());
        } else { // 첫 가입 유저
            final User newUser = User.create(socialId);
            userRepository.save(newUser);
            return issueToken(newUser.getUserId());
        }
    }

    private UserLoginRes issueToken(final Long userId) {
        Token issuedToken = jwtProvider.issueToken(userId);
        return UserLoginRes.of(issuedToken.accessToken(), issuedToken.refreshToken());
    }

    private boolean isExistUser(final String socialId) {
        return userRepository.existsBySocialId(socialId);
    }
}
