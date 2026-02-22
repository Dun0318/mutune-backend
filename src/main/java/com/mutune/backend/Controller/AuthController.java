package com.mutune.backend.Controller;

import com.mutune.backend.DTO.UserResponseDTO;
import com.mutune.backend.Entity.UserEntity;
import com.mutune.backend.Repository.UserRepository;
import com.mutune.backend.Security.Jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /** 프론트에서 accessToken을 보내면 현재 사용자 정보 반환 */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(@RequestAttribute("user") UserEntity user) {
        UserResponseDTO dto = UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .freeUsed(user.isFreeUsed())
                .role(user.getRole())
                .signupComplete(user.isSignupComplete())
                .provider(user.getProvider().name())
                .bio(user.getBio())
                .website(user.getWebsite())
                .termsAgreed(user.isTermsAgreed())
                .build();

        return ResponseEntity.ok(dto);
    }

    /** RefreshToken을 이용해 AccessToken 재발급 */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body("refreshToken is required");
        }

        try {
            // 1) RefreshToken 파싱
            Claims claims = jwtProvider.parseClaims(refreshToken);
            Long userId = Long.valueOf(claims.getSubject());

            // 2) DB에서 사용자 조회 + RefreshToken 일치 확인
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (!refreshToken.equals(user.getRefreshToken())) {
                return ResponseEntity.status(401).body("Invalid refresh token");
            }

            // 3) 새 AccessToken + RefreshToken 생성
            String newAccessToken = jwtProvider.createAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    user.getUsername(),
                    user.getProfileImage(),
                    user.getProvider().name(),
                    user.getBio(),
                    user.getWebsite()
            );
            String newRefreshToken = jwtProvider.createRefreshToken(user.getId());

            // 4) DB에 새 RefreshToken 저장
            user.setRefreshToken(newRefreshToken);
            userRepository.save(user);

            // 5) 반환
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);

            return ResponseEntity.ok(tokens);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("refresh token invalid or expired");
        }
    }
}
