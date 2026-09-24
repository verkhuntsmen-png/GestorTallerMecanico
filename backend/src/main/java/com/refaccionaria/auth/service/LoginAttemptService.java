package com.refaccionaria.auth.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(normalize(key));
        return attempt != null && attempt.lockedUntil != null && attempt.lockedUntil.isAfter(Instant.now());
    }

    public void loginSucceeded(String key) {
        attempts.remove(normalize(key));
    }

    public void loginFailed(String key) {
        attempts.compute(normalize(key), (ignored, current) -> {
            Attempt attempt = current == null ? new Attempt() : current;
            attempt.count++;
            if (attempt.count >= MAX_ATTEMPTS) {
                attempt.lockedUntil = Instant.now().plusSeconds(LOCK_MINUTES * 60);
            }
            return attempt;
        });
    }

    private String normalize(String key) {
        return key == null ? "unknown" : key.trim().toLowerCase();
    }

    private static class Attempt {
        int count;
        Instant lockedUntil;
    }
}
