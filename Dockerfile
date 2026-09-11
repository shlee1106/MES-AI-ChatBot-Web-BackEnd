# 프로젝트 소스 코드 컴파일 단계
FROM eclipse-temurin:25-alpine AS builder
# 컨테이너임을 명시 (외부 의존성 테스트 건너뛰기)
ENV CI=true
# 작업 경로를 /builder로 설정 (cd /builder)
WORKDIR /builder

# 프로젝트를 컨테이너로 복사
COPY . .

# 빌드 실행
RUN ./gradlew build --no-daemon
# jar 파일 추출
RUN java -Djarmode=tools -jar build/libs/*SNAPSHOT.jar extract --layers --destination extracted

# 실행 단계
FROM eclipse-temurin:25-alpine
# 작업 경로를 /application으로 설정 (cd /application)
WORKDIR /application

# Copy the extracted jar contents from the builder container into the working directory in the runtime container
# Every copy step creates a new docker layer
# This allows docker to only pull the changes it really needs
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# Start the application jar - this is not the uber jar used by the builder
# This jar only contains application code and references to the extracted jar files
# This layout is efficient to start up and AOT cache (and CDS) friendly
ENTRYPOINT ["java", "-jar", "application.jar"]