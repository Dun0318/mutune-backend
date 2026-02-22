package com.mutune.backend.Security.Jwt;

import com.mutune.backend.Entity.UserEntity;
import com.mutune.backend.Repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtProvider.parseClaims(token);

                Long userId = Long.valueOf(claims.getSubject());
                UserEntity user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    // SecurityContext 세팅
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // @RequestAttribute 용으로 저장
                    request.setAttribute("user", user);
                }
            } catch (Exception e) {
                // 토큰 검증 실패 시 무시 → SecurityContext에 아무것도 안 들어감
                logger.warn("JWT 검증 실패", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
