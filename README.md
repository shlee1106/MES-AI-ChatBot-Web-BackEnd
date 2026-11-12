# MES AI ChatBot Web - Backend

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.2-6DB33F?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![OpenSearch](https://img.shields.io/badge/OpenSearch-255AAC?style=flat-square&logo=opensearch&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)

사내 제조·업무 문서(PDF, DOCX, PPTX, XLSX, CSV)를 업로드하여 문맥 단위로 분할·벡터화하고, RAG(Retrieval-Augmented Generation) 파이프라인을 통해 사내 문서 기반
질의응답을 제공하는 백엔드 서버입니다.

---

## 주요 기능

### 1. 다중 포맷 문서 파싱 및 의미 기반 청킹 (Semantic Chunking)

- **포맷별 전용 파서**: `Apache PDFBox`(PDF), `Apache POI`(DOCX, PPTX, XLSX), `OpenCSV`(CSV)를 이용해 문서 구조에 맞게 텍스트와 표 데이터를 추출합니다.
- **비정형 문서 무결성 검증**: LLM을 통해 문맥 단위로 분할한 뒤, `ChunkingValidator`가 원본 문자열과 대조하여 요약·누락 등 원문 변형이 감지되면 즉시 글자 수 기반 분할로 전환
  (Fallback)합니다.
- **정형 데이터(Excel/CSV) 행 단위 매핑**: 수치 변형을 방지하기 위해 LLM이 텍스트를 다시 생성하지 않고 연관된 줄 번호(Row Index) 범위만 반환하도록 하여 원본 데이터를 보존합니다.

### 2. Spring AI 기반 RAG 질의응답 및 세션 관리

- **벡터 유사도 검색**: `Ollama` 임베딩 모델을 통해 문서를 벡터화하여 `VectorStore`에 저장하고, 질문 시 유사도가 높은 문서 청크를 조합해 답변을 생성합니다.
- **채팅 세션 및 히스토리**: 사용자(회사·사번) 단위로 대화방을 생성·조회·삭제하고 대화 내역을 관리합니다.

### 3. JWT 인증 및 계층형 권한 제어 (Spring Security)

- **Stateless 인증 필터 체인**: `JWTFilter`, `LoginFilter`, `CustomLogoutFilter`를 통해 세션 없이 JWT 기반으로 인증을 처리하며, 비밀번호는 `Argon2`
  알고리즘으로 암호화합니다.
- **계층형 권한(`RoleHierarchy`)**: `ROLE_MASTER > ROLE_MANAGER > ROLE_USER` 권한 체계를 적용해 사원 계정 생성 및 관리자 기능을 통제합니다.
- **Redis TTL 토큰 관리**: Refresh Token과 비밀번호 재설정용 일회용 인증 토큰(유효시간 5분)을 Redis에 저장하여 만료 주기를 관리하고, SMTP를 통해 비밀번호 재설정 메일을 비동기로
  발송합니다.

### 4. 다중 저장소 일괄 삭제

- 본인이 업로드한 파일 삭제 요청 시 로컬 디스크 원본 파일, `MySQL` 메타데이터, `VectorStore` 임베딩 데이터를 함께 제거하여 삭제된 문서가 재검색되지 않도록 처리합니다.

---

## 기술 스택

| 분류                     | 기술                                                                     |
|:-------------------------|:-------------------------------------------------------------------------|
| **Language & Framework** | Java 25, Spring Boot 4.1.2, Spring Security, Spring Data JPA, Validation |
| **AI & RAG**             | Spring AI 2.0.2, OpenAI API (Chat), Ollama (Embedding)                   |
| **Document Parsing**     | Apache PDFBox 3.0.8, Apache POI 6.0.0, OpenCSV 5.12.0                    |
| **Database**             | MySQL (RDB), Redis (Token Store), OpenSearch (VectorStore)               |
| **Infra & Docs**         | Docker, Docker Compose, Springdoc OpenAPI (Swagger UI), Gradle           |

---

## 패키지 구조

```text
src/main/java/com/mes/mesaichatbot
├── config          # SecurityConfig 등 애플리케이션 설정
├── controller      # 파일, 회원, 인증(JWT), 이메일 API 컨트롤러
├── domain          # JPA/Redis 엔티티 및 도메인별(user, jwt, email) 서비스·DTO
├── dto             # 파일 입출력 및 로그인 공통 DTO
├── filter          # JWT 검증, 로그인, 로그아웃 커스텀 필터
├── handler         # 로그인 성공 핸들러 및 전역 예외 처리(GlobalExceptionHandler)
├── llm             # RAG 질의응답 컨트롤러, 청킹 서비스, 무결성 검증기(Validator)
├── parser          # PDF, DOCX, PPTX, XLSX, CSV 문서 파서
├── repository      # JPA 및 Redis 리포지토리
├── service         # 파일 업로드·삭제 및 문서 파싱/임베딩 연동 서비스
└── util            # JWT 발급 및 파싱 유틸리티
```

---

## 주요 API 명세

| 분류         |  Method  | Endpoint                                      | 설명                                        |      권한      |
|:-------------|:--------:|:----------------------------------------------|:--------------------------------------------|:--------------:|
| **Auth**     |  `POST`  | `/api/v1/login`                               | 사원 로그인 (Access/Refresh 발급)           |   PermitAll    |
| **Auth**     |  `POST`  | `/api/v1/logout`                              | 로그아웃 및 Refresh Token 만료 처리         |   PermitAll    |
| **Auth**     |  `POST`  | `/api/v1/jwt/refresh`                         | Refresh Token 쿠키 기반 Access Token 재발급 |   PermitAll    |
| **Password** |  `POST`  | `/api/v1/password/request`                    | 비밀번호 재설정 이메일 발송 요청            |   PermitAll    |
| **Password** |  `GET`   | `/api/v1/password/token/{token}`              | 비밀번호 재설정 토큰 유효성 검증            |   PermitAll    |
| **Password** |  `POST`  | `/api/v1/password/update`                     | 새 비밀번호 변경 처리                       |   PermitAll    |
| **User**     |  `POST`  | `/api/v1/admin/users`                         | 신규 사원 계정 생성                         | `MANAGER` 이상 |
| **User**     |  `GET`   | `/api/v1/users/me`                            | 내 정보 조회                                | Authenticated  |
| **User**     |  `GET`   | `/api/v1/users/`                              | 전체 사용자 목록 조회                       | Authenticated  |
| **User**     | `PATCH`  | `/api/v1/users/{target}`                      | 사용자 권한(Role) 수정                      | `MANAGER` 이상 |
| **User**     | `DELETE` | `/api/v1/users/{target}`                      | 사용자 계정 삭제                            | `MANAGER` 이상 |
| **File**     |  `GET`   | `/api/v1/files`                               | 업로드된 사내 문서 목록 조회                | Authenticated  |
| **File**     |  `POST`  | `/api/v1/files/upload`                        | 문서 업로드, 파싱·청킹 및 벡터 인덱싱       | Authenticated  |
| **File**     | `DELETE` | `/api/v1/files/delete/{fileId}`               | 문서 원본, 메타데이터, 임베딩 일괄 삭제     |   본인 파일    |
| **Chat**     |  `POST`  | `/api/v1/chat/sessionrooms`                   | 신규 채팅 세션 생성                         | Authenticated  |
| **Chat**     |  `GET`   | `/api/v1/chat/sessions`                       | 내 채팅 세션 목록 조회                      | Authenticated  |
| **Chat**     |  `GET`   | `/api/v1/chat/sessions/{sessionId}/histories` | 특정 세션의 대화 이력 조회                  |   본인 세션    |
| **Chat**     |  `POST`  | `/api/v1/chat/sessions/{sessionId}/ask`       | 사내 문서 기반 RAG 질문 및 답변 생성        |   본인 세션    |
| **Chat**     | `DELETE` | `/api/v1/chat/sessions/{sessionId}`           | 채팅 세션 및 대화 내역 삭제                 |   본인 세션    |

---

## 시작하기 (Getting Started)

### 1. 사전 요구 사항

- **JDK 25**
- **Docker & Docker Compose**
- **MySQL** 및 **Ollama** (로컬 또는 원격 서버)

### 2. 환경 변수 설정

루트 경로의 `.env.example` 파일을 복사하여 `.env` 파일을 생성하고 환경에 맞게 값을 입력합니다.

```bash
cp .env.example .env
```

*(아래 설정은 예시입니다)*

```properties
# 임베딩 모델 설정 (Ollama)
EMBEDDING_MODEL=embeddinggemma-2:latest
OLLAMA_HOST=localhost
# LLM 모델 설정 (OpenAI 및 호환 API 사용 가능)
LLM_API_KEY=sk-example-key
LLM_API_URL=https://api.openai.com/v1
LLM_MODEL=gpt-6-luna
# Amazon OpenSearch Serverless 설정 (벡터 저장소)
# 컬렉션 엔드포인트에서 https:// 를 뺀 호스트 이름
OPENSEARCH_HOST=example.aoss.region.on.aws
AWS_ACCESS_KEY_ID=example-key
AWS_SECRET_ACCESS_KEY=example-secret
# MySQL 설정
MYSQL_HOST=localhost
MYSQL_DB=example_db
MYSQL_USER=example
MYSQL_PASSWORD=password
# Redis 설정
REDIS_HOST=localhost
# SMTP 설정
#SMTP_HOST=
SMTP_USERNAME=test
SMTP_PASSWORD=test
# JWT 서명 설정
JWT_SECRET=test
# 프론트엔드 호스트 이름 설정
HOSTNAME=http://localhost:3000
```

### 3. 인프라 컨테이너 실행 (Redis)

```bash
docker compose up -d
```

### 4. 빌드 및 실행

```bash
./gradlew clean build
./gradlew bootRun
```

### 5. API 테스트 및 문서 확인

- **Swagger UI**: 서버 실행 후 `http://localhost:8080/swagger-ui/index.html` 접속
- **HTTP Client**: 프로젝트 내 `/http` 디렉토리의 `.http` 요청 파일 및 `/samples` 디렉토리의 샘플 문서를 사용해 각 엔드포인트를 바로 테스트할 수 있습니다.
