package com.mutune.backend.oauth.userinfo;

import java.util.Map;

@SuppressWarnings("unchecked")
public class NaverOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attrs;
    public NaverOAuth2UserInfo(Map<String, Object> attrs) { this.attrs = attrs; }

    private Map<String, Object> res() {
        return (Map<String, Object>) attrs.get("response");
    }

    public String getId()       { return (String) res().get("id"); }
    public String getEmail()    { return (String) res().get("email"); }
    public String getName()     { return (String) res().get("name"); }
    public String getImageUrl() { return (String) res().get("profile_image"); }
    public Map<String, Object> getAttributes() { return attrs; }
}
