---
name: spring-rules
description: Spring Boot 코드 작성 규칙. Spring Boot 코드를 작성할 때 읽는다
---

# Spring Boot 작업 규정

- 모든 Controller 핸들러 메서드는 ResponseEntity<T>를 반환한다
    - 적절한 HTTP 상태 코드와 응답 본문을 명시한다
    - 기존 공통 응답 DTO와 오류 응답 형식을 따른다
- 예외 처리 시 구체적인 예외 타입을 우선 사용한다
    - 일반적인 비즈니스 로직에서 Exception 또는 RuntimeException을
      무분별하게 catch하지 않는다
    - 예외를 무시하거나 성공 결과로 변환하지 않는다
    - 전역 예외 처리기에서 예상하지 못한 오류를 처리해야 하는 경우에는
      기존 오류 처리 정책을 따르고 민감한 내부 정보를 노출하지 않는다