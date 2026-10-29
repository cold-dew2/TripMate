package com.example.backend.global.jwt;

import com.example.backend.trma.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 탈퇴한 회원 아이디 목록. 탈퇴 전에 발급된 토큰(다른 기기 등)이 만료 전까지 계속 쓰이지 않도록
// JWT 필터가 요청마다 확인한다. 매 요청 DB 조회를 피하려고 메모리에 두며(인스턴스 하나짜리 배포),
// 처음 쓸 때 DB에서 한 번 읽고 이후 탈퇴는 add로 바로 반영한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawnUserRegistry {

    private final UserMapper userMapper;
    private final Set<String> withdrawn = ConcurrentHashMap.newKeySet();
    private volatile boolean loaded = false;

    public boolean isWithdrawn(String userId) {
        ensureLoaded();
        return userId != null && withdrawn.contains(userId);
    }

    public void add(String userId) {
        withdrawn.add(userId);
    }

    private void ensureLoaded() {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            try {
                withdrawn.addAll(userMapper.withdrawnUserIds());
                loaded = true;
            } catch (Exception e) {
                // DB를 못 읽어도 요청 자체는 막지 않는다(다음 요청에서 다시 시도). 로그인은 DB에서 따로 막힌다.
                log.warn("탈퇴 회원 목록을 불러오지 못했습니다.", e);
            }
        }
    }
}
