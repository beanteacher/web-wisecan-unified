package com.wisecan.unified.exception;

import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.exception.AccountLockedException;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.exception.InsufficientFundsException;
import com.wisecan.unified.service.trial.TrialAbuseBlockedException;
import com.wisecan.unified.service.trial.TrialSessionExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 발송 정합성 검증 실패 — HTTP 409 Conflict.
     * errorCode 필드를 포함하여 클라이언트가 실패 원인을 식별할 수 있도록 한다.
     * SendValidationException extends IllegalStateException 이므로
     * 이 핸들러가 일반 IllegalStateException 핸들러보다 먼저 매칭된다.
     */
    @ExceptionHandler(SendValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleSendValidation(SendValidationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("[" + e.getErrorCode().name() + "] " + e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getDefaultMessage()).findFirst().orElse("입력값 오류");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(AccountLockedException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(e.getMessage()));
    }

    /** 체험 모드 어뷰징 차단 — HTTP 429 Too Many Requests (W-406) */
    @ExceptionHandler(TrialAbuseBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTrialAbuseBlocked(TrialAbuseBlockedException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(e.getMessage()));
    }

    /** 체험 세션 만료 — HTTP 401 Unauthorized (W-406) */
    @ExceptionHandler(TrialSessionExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTrialSessionExpired(TrialSessionExpiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
    }

    /** 과금/충전 비즈니스 오류 — HTTP 400 Bad Request (W-401) */
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<ApiResponse<Void>> handleBilling(BillingException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    /** 잔액 부족 전체 취소 — HTTP 402 Payment Required (W-405) */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientFunds(InsufficientFundsException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.getMessage()));
    }
}
