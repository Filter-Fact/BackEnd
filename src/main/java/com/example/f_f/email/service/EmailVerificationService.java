package com.example.f_f.email.service;

import com.example.f_f.global.exception.CustomException;
import com.example.f_f.global.exception.RsCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final Random random = new Random();

    // TTL 정책
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(15);

    public void sendVerificationCode(String email, String purpose) {
        String code = generate6Digit();

        // Redis에 저장 (5분 유효)
        redis.opsForValue().set(codeKey(email, purpose), code, CODE_TTL);

        // 메일 전송
        sendMail(email, purpose, code);
    }

    public void verifyAndMarkAsVerified(String email, String purpose, String code) {
        String savedCode = redis.opsForValue().get(codeKey(email, purpose));

        //이메일 인증 코드 검증에 대해 에러 세분화
        if (savedCode == null) {
            throw new CustomException(RsCode.VERIFICATION_TIME_OUT);
        } else if (!savedCode.equals(code)) {
            throw new CustomException(RsCode.INVALID_VERIFICATION_CODE);
        }

        // 사용 후 즉시 코드 삭제
        redis.delete(codeKey(email, purpose));

        // 인증 완료 마크(15분 동안 유효)
        redis.opsForValue().set(verifiedKey(email, purpose), "1", VERIFIED_TTL);
    }

    public void ensureVerified(String email, String purpose) {
        String v = redis.opsForValue().get(verifiedKey(email, purpose));

        if (v == null) {
            throw new CustomException(RsCode.EMAIL_VERIFICATION_FAILED);
        }

        redis.delete(verifiedKey(email, purpose));
    }

    private void sendMail(String email, String purpose, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[FilterFacts] " + purpose + " 이메일 인증 코드");
        message.setText("안녕하세요!\n\n요청하신 인증 코드는 " + code + " 입니다.\n5분 내에 입력해주세요.");
        mailSender.send(message);
    }

    private String codeKey(String email, String purpose) {
        return "ev:code:" + purpose + ":" + email;
    }
    private String verifiedKey(String email, String purpose) {
        return "ev:verified:" + purpose + ":" + email;
    }
    private String generate6Digit() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}