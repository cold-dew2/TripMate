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

        // JWT 검사 제외
        if (uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/login")
                || uri.startsWith("/signup")) {

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