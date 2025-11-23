FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /build

# 1. 의존성 캐시 활용
COPY build.gradle settings.gradle/build/

RUN gradle dependencies --no-daemon || true

# 2. 전체 소스 복사
COPY . /build

# 3. 빌드 캐시 무효화
RUN rm -rf /build/build/

# 4. 빌드 실행
RUN gradle clean build -x test --no-daemon --parallel

# ------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080
USER 1001

ENTRYPOINT ["java", "-jar", "app.jar"]

