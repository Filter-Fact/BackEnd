package com.example.f_f.user.jwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@Primary
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisTokenStore implements TokenStore {
    private final StringRedisTemplate redis;

    public RedisTokenStore(StringRedisTemplate redis) { this.redis = redis; }

    private String key(String userId, String token) { return "refresh:" + userId + ":" + token; }

    @Override public void saveRefresh(String userId, String refreshToken, long ttlMs) {
        redis.opsForValue().set(key(userId, refreshToken), "1", Duration.ofMillis(ttlMs));
    }
    @Override public boolean isRefreshValid(String userId, String refreshToken) {
        return Boolean.TRUE.equals(redis.hasKey(key(userId, refreshToken)));
    }
    @Override public void revokeRefresh(String userId, String refreshToken) {
        redis.delete(key(userId, refreshToken));
    }
    @Override
    public void revokeAll(String userId) {
        // 패턴 삭제 (주의: 대량 키에선 SCAN 사용)
        Set<String> keys = redis.keys("rt:" + userId + ":*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }
}
