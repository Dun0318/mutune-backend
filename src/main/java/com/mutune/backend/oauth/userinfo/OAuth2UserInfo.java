package com.mutune.backend.oauth.userinfo;

import java.util.Map;

/** 모든 소셜에서 공통으로 쓰는 표준 인터페이스 */
public interface OAuth2UserInfo {
    String getId();
    String getEmail();     // 없으면 null
    String getName();      // 닉네임/이름
    String getImageUrl();  // 프로필 이미지 URL(없으면 null)
    Map<String, Object> getAttributes();
}
