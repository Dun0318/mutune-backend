package com.mutune.backend.DTO;

import lombok.*;

/**
 * 클라이언트로 내려줄 사용자 정보 DTO
 * Entity 전체를 노출하지 않고 필요한 필드만 노출
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;            // 사용자 ID
    private String username;    // 닉네임
    private String email;       // 이메일
    private String profileImage; // 프로필 이미지
    private boolean freeUsed;   // 1회 무료 사용 여부
    private String role;        // 권한 (ROLE_USER, ROLE_ADMIN 등)

    private boolean signupComplete; //프로필 완료 여부 추후에 사용 예정 ex) 프로필을 완료해야 게시물을 올릴 수 있게 ( 나중에 SNS OR 커뮤니티 에서 사용)
    private String provider;         // GOOGLE / NAVER / KAKAO

    // === 추후 프로필/커뮤니티 기능에서 사용 예정 ===
    private String bio;     // 자기소개 (현재는 null)
    private String website; // 개인 웹사이트, SNS 링크 (현재는 null)

    private boolean termsAgreed;  // 약관 동의
}
