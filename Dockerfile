FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /build

# 1. 의존성 캐시 위해 gradle 파일만 먼저 복사
COPY build.gradle settings.gradle /build/

# 2. 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 3. 소스 전체 복사
COPY . /build

# ⭐ 4. 빌드 캐시 문제 방지를 위한 강제 옵션
# Gradle 캐시 무효화 + 빌드 캐시 삭제
RUN rm -rf /build/build/

# 5. 실제 빌드 실행
RUN gradle clean build -x test --no-daemon --parallel


# --------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080
USER 1001
ENTRYPOINT ["java", "-jar", "app.jar"]
