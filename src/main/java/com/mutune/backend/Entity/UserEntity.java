package com.mutune.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_provider_pid", columnList = "provider, provider_id", unique = true)

        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                       // PK

    @Column(nullable = false, length = 50)
    private String username;               // 닉네임

    @Column(nullable = false, length = 100)
    private String email;                  // 이메일 ( 소셜에서 제공 시 저장 없으면 null )

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;         // GOOGLE / NAVER / KAKAO

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;             // 소셜 고유 ID ( 소셜에서 제공하는 고유 ID )

    @Column(name = "profile_image", length = 255)
    private String profileImage;           // 프로필 이미지 URL (null 허용)

    @Builder.Default
    @Column(nullable = false)
    private boolean signupComplete = false; // 프로필 등록 등 초기 세팅 완료 여부

    @Builder.Default
    private boolean freeUsed = false; // 회원가입 1회 무료 사용 여부

    @Builder.Default
    @Column(nullable = false)
    private boolean termsAgreed = false; // 약관 동의 여부

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";  // 사용자 전환 : ROLE_USER, ROLE_ADMIN
    // Spring Security 인증에 사용 (Jwt)
    // 새로운 유저 가입 시 ROLE_USER 로 들어감
    // ADMIN으로는 관리자 기능을 부여

    @Column(length = 512)
    private String refreshToken;
    // 리플레시 토큰 저장용

    @Column(length = 255)
    private String bio;   // 한 줄 소개 (null 허용)

    @Column(length = 255)
    private String website; // 개인 웹사이트, SNS 링크 (null 허용)
}
