package com.mutune.backend.DTO;

import com.mutune.backend.Entity.AuthProvider;
import lombok.*;

/**
 역할: API 입출력 전용 데이터 모델 (Entity 노출 방지)
 컨트롤러 <-> 서비스 간 전송에 사용
 필요 필드만 노출/수정 가능하도록 제한
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserDTO {
    private Long id; //아이디
    private String username; // 닉네임
    private String email; // 이메일
    private AuthProvider provider;   // GOOGLE / NAVER / KAKAO /INSTAGRAM
    private String providerId;       // 소셜 고유 ID
    private String profileImage;    // 프로필
    private boolean signupComplete;  // 프로필/초기세팅 완료 여부
    private boolean freeUsed;        // (옵션) 가입 1회 무료 사용 여부
    private boolean termsAgreed; // 약관 동의 여부 (개인 정보)
}
