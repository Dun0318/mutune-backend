package com.mutune.backend.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        //  JWT 발급 로직 넣기 (User 정보로 JWT 생성)

        // JWT를 프론트엔드로 전달 (쿠키/쿼리스트링/헤더 중 선택)
        // 예시: 쿼리 파라미터로 전달
        String token = "sample-jwt-token"; // 실제 JWT 발급 로직으로 대체
        response.sendRedirect("http://localhost:5173/oauth2/callback?token=" + token);
    }
}
