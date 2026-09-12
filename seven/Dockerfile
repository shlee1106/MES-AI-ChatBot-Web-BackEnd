# Java 25 환경 적용
FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

# 빌드된 JAR 파일을 컨테이너로 복사
COPY build/libs/*SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]