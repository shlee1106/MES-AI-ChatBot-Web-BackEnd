package com.mes.mesaichatbot.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 애플리케이션 전역에서 발생하는 예외를 가로채어 공통 규격의 HTTP 응답으로 변환하는 핸들러.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 잘못된 인자값으로 인해 발생하는 비즈니스 예외를 처리(400 Bad Request)
     *
     * @param e 발생한 {@link IllegalArgumentException}
     * @return 에러 메시지가 담긴 JSON 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        // 프론트엔드가 파싱하기 쉽도록 JSON 형태의 에러메세지 반환 (메시지가 null일 경우 기본 문구 사용)
        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage() : "잘못된 요청입니다.";
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    /**
     * {@code @Valid} 유효성 검증 실패 시 발생하는 예외를 처리(400 Bad Request)
     *
     * @param e 발생한 {@link MethodArgumentNotValidException}
     * @return 필드별 검증 오류 메시지를 줄바꿈으로 합친 텍스트 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        // 모든 필드의 검증 실패 메시지를 추출하여 줄바꿈(\n)으로 연결
        String errorMessage = e.getBindingResult().getFieldErrors().parallelStream().map(
                DefaultMessageSourceResolvable::getDefaultMessage
        ).collect(Collectors.joining("\n"));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorMessage);
    }

    /**
     * LLM 서비스를 이용할 수 없을 때 발생하는 예외를 처리(503 Service Unavailable)
     *
     * @param e {@link UnsupportedOperationException}
     * @return LLM 서비스 이용 불가 안내 메시지
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedOperationException(UnsupportedOperationException e) {
        log.error("LLM 서비스를 이용할 수 없습니다: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                Map.of("error", "LLM 서비스를 이용할 수 없습니다.")
        );
    }

    /**
     * 존재하지 않는 API 경로로 요청했을 때 404 Not Found를 반환하는 예외 처리 메서드
     *
     * @return 존재하지 않는 API 안내 메시지
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, String>> handleNoHandlerFoundException() {
        // 매핑되지 않은 잘못된 URL 호출 시 404 상태 코드와 안내 문구 반환
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "존재하지 않는 API입니다."));
    }

    /**
     * API 주소는 존재하지만 지원하지 않는 HTTP 메서드(예: POST 전용에 GET 요청)로 호출했을 때 처리(405 Method Not Allowed)
     *
     * @return 지원하지 않는 메서드 안내 메시지
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupportedException(
    ) {
        // 허용되지 않은 HTTP 메서드로 요청 시 405 상태 코드와 안내 문구 반환
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "지원하지 않는 HTTP 메서드입니다."));
    }

    /**
     * 요청 JSON 본문 형식이 깨졌거나 Enum(예: UserRole)에 없는 잘못된 값을 보냈을 때 처리(400 Bad Request)
     *
     * @return 요청 데이터 형식 오류 안내 메시지
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadableException(
    ) {
        // JSON 역직렬화(파싱) 실패 시 400 상태 코드와 안내 문구 반환
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "요청 데이터 형식이 올바르지 않습니다."));
    }

    /**
     * 업로드한 파일 크기가 설정된 제한 용량(10MB)을 초과했을 때 처리(413 Content Too Large)
     *
     * @return 파일 크기 초과 안내 메시지
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(
    ) {
        // 허용된 최대 파일 업로드 용량(10MB) 초과 시 413 상태 코드와 안내 문구 반환
        return ResponseEntity
                .status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("error", "파일 크기는 10MB를 초과할 수 없습니다."));
    }

    /**
     * 사용자가 어떤 서비스에 접근하거나 기능을 요청하였을 때 권한이 없을 경우 처리(403 Forbidden)
     *
     * @param e 발생한 {@link AccessDeniedException}
     * @return 접근 권한 없음 안내 메시지
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException e) {
        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage() : "사용자 권한이 없습니다.";
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", message));
    }

    /**
     * 중복 계정 생성 및 중복키 존재 시 예외처리 (409 Conflict)
     * 서비스 계층 상황에 따라 구체적인 에러 사유를 담아 예외 던지기 위해 e.getMessage() 사용
     *
     * @param e 발생한 {@link DuplicateKeyException}
     * @return 중복 데이터 안내 메시지
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateKeyException(DuplicateKeyException e) {
        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage() : "이미 생성된 계정이 있습니다.";
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", message));
    }

    /**
     * 사용자를 찾을 수 없는 경우 예외처리 (404 Not Found)
     *
     * @param e 발생한 {@link UsernameNotFoundException}
     * @return 사용자 미존재 안내 메시지
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUsernameNotFoundException(UsernameNotFoundException e) {
        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage() : "올바르지 않은 요청 방식입니다.";
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", message));
    }

    /**
     * 유효한 인증 세션 정보가 없을 때 발생하는 예외 처리 (401 Unauthorized)
     *
     * @return 인증 실패 안내 메시지
     */
    @ExceptionHandler({AuthenticationCredentialsNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, String>> handleAuthenticationException() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "세션이 유효하지 않습니다."));
    }

    /**
     * 손상되었거나 올바르지 않은 형식의 파일 업로드 시 발생하는 예외 처리 (400 Bad Request)
     *
     * @return 파일 형식 오류 안내 메시지
     */
    @ExceptionHandler({IOException.class, NotOfficeXmlFileException.class})
    public ResponseEntity<Map<String, String>> handleInvalidFileException() {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "파일이 올바르지 않습니다."));
    }

    /**
     * 상태 코드가 명시된 비즈니스 예외 처리 (예: 404 Not Found, 400 Bad Request 등)
     *
     * @param e 발생한 {@link ResponseStatusException}
     * @return 예외에 지정된 HTTP 상태 코드와 안내 메시지
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException e) {
        String message = (e.getReason() != null && !e.getReason().isBlank())
                ? e.getReason() : "요청을 처리할 수 없습니다.";
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", message));
    }

    /**
     * 외부 LLM 응답이 비어 있거나 호출에 실패했을 때 발생하는 예외 처리 (502 Bad Gateway)
     *
     * @param e 발생한 {@link IllegalStateException}
     * @return 외부 서비스 응답 실패 안내 메시지
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException e) {
        log.error("LLM 응답이 비어 있거나 실패했습니다: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "LLM 응답이 비어 있거나 실패했습니다."));
    }

    /**
     * 위에서 개별적으로 처리되지 않은 예기치 못한 서버 내부 오류 처리 (500 Internal Server Error)
     *
     * @param e 발생한 {@link Exception}
     * @return 서버 내부 오류 안내 메시지
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("예기치 못한 서버 오류가 발생했습니다: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "예기치 못한 오류가 발생했습니다."));
    }
}
