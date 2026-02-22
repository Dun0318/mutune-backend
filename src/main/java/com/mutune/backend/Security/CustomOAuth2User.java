package com.mutune.backend.Security;

import com.mutune.backend.Entity.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
// 토큰 발급 ( 보안 ) Security 패키지 = 보안
@Getter
public class CustomOAuth2User implements OAuth2User {

    private final UserEntity user;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(UserEntity user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // 필요하다면 ROLE 반환
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

    public UserEntity getUserEntity() {
        return user;
    }
}
