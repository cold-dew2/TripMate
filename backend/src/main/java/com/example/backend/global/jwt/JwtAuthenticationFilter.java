package com.example.backend.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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


        try {

            String header = request.getHeader("Authorization");


            if (header == null || !header.startsWith("Bearer ")) {

                filterChain.doFilter(request, response);
                return;
            }


            String token = header.substring(7);


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
}