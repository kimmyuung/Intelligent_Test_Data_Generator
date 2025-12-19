# Java 서비스용 멀티스테이지 빌드
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 모듈 소스 복사
COPY itdg-common itdg-common
COPY itdg-analyzer itdg-analyzer

# 빌드
RUN chmod +x gradlew && \
    ./gradlew :itdg-analyzer:bootJar --no-daemon -x test

# 실행 이미지
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Git 설치 (저장소 분석용)
RUN apk add --no-cache git

# 보안: 비루트 사용자 생성
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 임시 디렉토리 생성
RUN mkdir -p /app/temp_repo /app/external_repos && \
    chown -R appuser:appgroup /app

# JAR 복사
COPY --from=builder /app/itdg-analyzer/build/libs/*.jar app.jar

USER appuser

# 환경 변수
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8081/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
