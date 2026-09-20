package com.example.backend.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtUtil jwtUtil;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // JWT 검사 제외(토큰이 없어도 되는, 로그인 전 단계에서만 쓰는 API).
        // UserController가 통째로 "/login"에 매핑돼 있다 보니 예전엔 uri.startsWith("/login")로
        // 걸러서, 로그인 여부를 실제로 가려야 하는 /login/reviewList 같은 API까지 JWT 파싱 자체가
        // 건너뛰어져 있었다(그 결과 Authentication이 항상 비어서 로그인해도 항상 NEED_LOGIN으로
        // 응답함). 진짜로 토큰 없이 접근해야 하는 경로만 정확히 지정한다.
        if (uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.equals("/login/login")
                || uri.equals("/login/logout")
                || uri.equals("/login/signup")
                || uri.equals("/login/existsUserId")
                || uri.equals("/login/findId")
                || uri.equals("/login/verifyReset")
                || uri.equals("/login/resetPassword")) {

            filterChain.doFilter(request, response);
            return;
        }


        try {

            String token = resolveToken(request);

            if (token == null) {

                filterChain.doFilter(request, response);
                return;
            }


            if (!jwtUtil.validateToken(token)) {

                throw new BadCredentialsException(
                        "Invalid JWT Token"
                );

            }


            String userId = jwtUtil.getUserId(token);


            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_USER")
                            )
                    );


            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);


            filterChain.doFilter(request, response);


        } catch (BadCredentialsException e) {

            jwtAuthenticationEntryPoint.commence(
                    request,
                    response,
                    e
            );
        }
    }

    // 쿠키(accessToken)를 우선 사용하고, 없으면 Authorization 헤더를 사용한다.
    private String resolveToken(HttpServletRequest request) {

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }
}