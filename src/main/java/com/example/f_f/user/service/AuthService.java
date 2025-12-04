package com.example.f_f.user.service;

import com.example.f_f.email.service.EmailVerificationService;
import com.example.f_f.global.exception.CustomException;
import com.example.f_f.global.exception.RsCode;
import com.example.f_f.user.dto.*;
import com.example.f_f.user.entity.Purpose;
import com.example.f_f.user.entity.User;
import com.example.f_f.user.jwt.JwtService;
import com.example.f_f.user.jwt.TokenStore;
import com.example.f_f.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final TokenStore store;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public String register(RegisterRequest req) {

        if (userRepository.existsByUserId(req.userId())) {
            throw new CustomException(RsCode.DUPLICATE_USER_ID);
        }

        emailVerificationService.ensureVerified(req.email(), String.valueOf(Purpose.SIGN_UP));

        User u = new User();
        u.setUserId(req.userId());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setEmail(req.email());
        userRepository.save(u);
        return u.getUserId();
    }

    public TokenResponse login(LoginRequest req) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.userId(), req.password())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();

        String access = jwt.generateAccessToken(user);
        String refresh = jwt.generateRefreshToken(user);

        long expMs = jwt.getExpiration(access).getTime() - System.currentTimeMillis();
        long refreshTtl = jwt.getExpiration(refresh).getTime() - System.currentTimeMillis();
        store.saveRefresh(user.getUsername(), refresh, refreshTtl);

        return new TokenResponse(access, expMs, refresh);
    }

    public TokenResponse refresh(RefreshRequest req) {
        String refresh = req.refreshToken();
        if (!jwt.isTokenValid(refresh) || !jwt.isRefresh(refresh)) {
            throw new CustomException(RsCode.INVALID_REFRESH_TOKEN);
        }

        String userId = jwt.getUsername(refresh);
        if (!store.isRefreshValid(userId, refresh)) {
            throw new CustomException(RsCode.INVALID_REFRESH_TOKEN);
        }

        store.revokeRefresh(userId, refresh);

        UserDetails stub = org.springframework.security.core.userdetails.User
                .withUsername(userId).password("N/A").build();

        String newAccess = jwt.generateAccessToken(stub);
        String newRefresh = jwt.generateRefreshToken(stub);

        long expMs = jwt.getExpiration(newAccess).getTime() - System.currentTimeMillis();
        long refreshTtl = jwt.getExpiration(newRefresh).getTime() - System.currentTimeMillis();
        store.saveRefresh(userId, newRefresh, refreshTtl);

        return new TokenResponse(newAccess, expMs, newRefresh);
    }

    public void logout(LogoutRequest req) {
        String refresh = req.refreshToken();
        if (!jwt.isTokenValid(refresh) || !jwt.isRefresh(refresh)) {
            throw new CustomException(RsCode.INVALID_REFRESH_TOKEN);
        }
        String username = jwt.getUsername(refresh);
        store.revokeRefresh(username, refresh);
    }

}
