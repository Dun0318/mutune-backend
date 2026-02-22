package com.mutune.backend.oauth.userinfo;

import java.util.Map;

@SuppressWarnings("unchecked")
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attrs;
    public KakaoOAuth2UserInfo(Map<String, Object> attrs) { this.attrs = attrs; }

    public String getId() { return String.valueOf(attrs.get("id")); }

    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
        return account == null ? null : (String) account.get("email");
    }

    public String getName() {
        Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
        if (account == null) return null;
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        return profile == null ? null : (String) profile.get("nickname");
    }

    public String getImageUrl() {
        Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
        if (account == null) return null;
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        return profile == null ? null : (String) profile.get("profile_image_url");
    }

    public Map<String, Object> getAttributes() { return attrs; }
}
