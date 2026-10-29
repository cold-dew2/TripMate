package com.example.backend.trma.controller;

import com.example.backend.trma.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// E2E 테스트가 만든 계정(e2e+...@tripmate.test)을 확인하는 읽기 전용 API. 서버 로컬에서만.
// 정리 자체는 각 계정의 회원 탈퇴(/login/withdraw)로 한다(e2e/README.md 참고).
@RestController
@RequestMapping("/e2e")
@RequiredArgsConstructor
public class E2eAccountController {

    private final UserMapper userMapper;

    @GetMapping("/accounts")
    public ResponseEntity<?> accounts(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        boolean local = "127.0.0.1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote);
        if (!local) return ResponseEntity.status(403).body(Map.of("message", "로컬에서만 실행할 수 있습니다."));
        return ResponseEntity.ok(userMapper.e2eTestAccounts());
    }
}
