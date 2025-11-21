FROM gradle:8.5-jdk21-alpine AS builder

# 빌드용 디렉토리 설정
WORKDIR /build

# 의존성 캐시 최적화: build.gradle 먼저 복사
COPY build.gradle settings.gradle /build/

# 의존성 사전 다운로드 (캐시 활용 목적)
RUN gradle dependencies --no-daemon > /dev/null 2>&1 || true

# 소스 전체 복사
COPY . /build

# 실제 빌드 실행 (테스트 제외)
RUN gradle clean build -x test --no-daemon --parallel


FROM eclipse-temurin:21-jre

# 실행용 디렉토리
WORKDIR /app

# Builder 단계에서 빌드한 JAR 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# Spring Boot 기본 포트
EXPOSE 8080

# root 대신 non-root 사용자로 실행(보안)
USER 1001

ENTRYPOINT ["java", "-jar", "app.jar"]
