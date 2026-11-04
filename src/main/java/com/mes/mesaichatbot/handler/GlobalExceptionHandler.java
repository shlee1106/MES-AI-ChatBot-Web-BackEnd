package com.mes.mesaichatbot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        // 프론트엔드가 파싱하기 쉽도록 JSON 형태의 에러메세지 반환.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
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
}
