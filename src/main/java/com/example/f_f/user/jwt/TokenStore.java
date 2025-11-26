package com.example.f_f.user.jwt;

public interface TokenStore {
    void saveRefresh(String username, String refreshToken, long ttlMs);

    boolean isRefreshValid(String username, String refreshToken);

    void revokeRefresh(String username, String refreshToken);

//    void revokeAll(String username);
}