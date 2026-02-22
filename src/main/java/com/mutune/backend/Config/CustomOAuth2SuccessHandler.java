package com.mutune.backend.Config;

import com.mutune.backend.Entity.AuthProvider;
import com.mutune.backend.Entity.UserEntity;
import com.mutune.backend.Repository.UserRepository;
import com.mutune.backend.Security.Jwt.JwtProvider;
import com.mutune.backend.service.UserService;
import com.mutune.backend.oauth.userinfo.GoogleOAuth2UserInfo;
import com.mutune.backend.oauth.userinfo.KakaoOAuth2UserInfo;
import com.mutune.backend.oauth.userinfo.NaverOAuth2UserInfo;
import com.mutune.backend.oauth.userinfo.OAuth2UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final UserService userService; // ⬅ 추가 주입 (Service 계층 고정)

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 1) principal은 기본적으로 DefaultOAuth2User일 수 있음 → 캐스팅 금지
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            redirectError(response, "unsupported_principal");
            return;
        }
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 2) provider 식별 (google/kakao/naver)
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            redirectError(response, "missing_oauth2_token");
            return;
        }
        String registrationId = token.getAuthorizedClientRegistrationId(); // google/kakao/naver

        // 3) provider별 사용자 정보 파싱
        OAuth2UserInfo info = switch (registrationId) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            case "naver"  -> new NaverOAuth2UserInfo(attributes);
            default -> {
                redirectError(response, "unsupported_provider");
                yield null;
            }
        };
        if (info == null) return;

        // 4) enum 매핑을 안전하게
        AuthProvider provider = toProvider(registrationId);

        // 5) DB 업서트 (네 기존 구조 유지)
        userService.registerOrGet(
                provider,
                info.getId(),
                info.getEmail(),
                info.getName(),
                info.getImageUrl()
        );

        // 6) 최종적으로 Entity 재조회해서 JWT 생성 (DTO/엔티티 반환 타입 혼선 방지)
        UserEntity user = userRepository.findByProviderAndProviderId(provider, info.getId())
                .orElseThrow(() -> new UsernameNotFoundException("user not found after registerOrGet"));

        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getUsername(),
                user.getProfileImage(),
                user.getProvider().name(),
                user.getBio(),
                user.getWebsite()
        );
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        user.setRefreshToken(refreshToken); // 리프레시 토큰 DB에 저장
        userRepository.save(user); // update 반영

        // 7) 안전한 URL 빌드(자동 인코딩)
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendBaseUrl + "/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build(true) // encoding
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private AuthProvider toProvider(String id) {
        return switch (id.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            case "kakao"  -> AuthProvider.KAKAO;
            case "naver"  -> AuthProvider.NAVER;
            default -> throw new IllegalArgumentException("Unsupported provider: " + id);
        };
    }

    private void redirectError(HttpServletResponse response, String code) throws IOException {
        String url = UriComponentsBuilder
                .fromUriString(frontendBaseUrl + "/Login")
                .queryParam("error", code)
                .build(true).toUriString();
        response.sendRedirect(url);
    }
}
