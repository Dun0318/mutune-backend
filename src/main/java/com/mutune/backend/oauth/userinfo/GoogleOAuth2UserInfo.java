package com.mutune.backend.oauth.userinfo;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attrs;
    public GoogleOAuth2UserInfo(Map<String, Object> attrs) { this.attrs = attrs; }

    public String getId()       { return (String) attrs.get("sub"); }
    public String getEmail()    { return (String) attrs.get("email"); }
    public String getName()     { return (String) attrs.get("name"); }
    public String getImageUrl() { return (String) attrs.get("picture"); }
    public Map<String, Object> getAttributes() { return attrs; }
}
