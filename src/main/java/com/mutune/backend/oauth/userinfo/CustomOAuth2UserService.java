package com.mutune.backend.oauth;

import com.mutune.backend.Entity.AuthProvider;
import com.mutune.backend.service.UserService;
import com.mutune.backend.oauth.userinfo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User o = super.loadUser(req);
        String registrationId = req.getClientRegistration().getRegistrationId(); // google/kakao/naver
        Map<String, Object> attributes = o.getAttributes();

        OAuth2UserInfo info = switch (registrationId) {
            case "google"     -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"      -> new KakaoOAuth2UserInfo(attributes);
            case "naver"      -> new NaverOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };

        // DB 반영: 존재하면 반환, 없으면 생성
        var provider = AuthProvider.valueOf(registrationId.toUpperCase());
        userService.registerOrGet(
                provider,
                info.getId(),
                info.getEmail(),
                info.getName(),
                info.getImageUrl()
        );

        // 애플리케이션에서 사용할 표준 사용자 (권한: ROLE_USER)
        String nameAttrKey = req.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                nameAttrKey
        );
    }
}
