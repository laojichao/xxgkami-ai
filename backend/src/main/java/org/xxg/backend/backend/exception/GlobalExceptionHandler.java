package org.xxg.backend.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.xxg.backend.backend.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一捕获和处理各类异常，将异常转换为标准的ApiResponse格式返回给前端，
 * 避免将内部堆栈信息暴露给客户端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理自定义业务异常
     * 返回业务异常中指定的HTTP状态码和错误信息
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 处理认证失败异常（用户名或密码错误）
     * 返回401未授权状态码
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("用户名或密码错误"));
    }

    /**
     * 处理权限不足异常
     * 返回403禁止访问状态码
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("权限不足"));
    }

    /**
     * 处理参数校验异常（@Valid注解触发）
     * 遍历所有字段错误，返回字段名与错误信息的映射
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "参数验证失败", errors));
    }

    /**
     * 处理请求方法不支持异常（如用 POST 访问 GET 接口）
     * 返回 405 状态码
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("不支持的请求方法: " + e.getMethod()));
    }

    /**
     * 处理媒体类型不支持异常（如 Content-Type 错误）
     * 返回 415 状态码
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("不支持的媒体类型"));
    }

    /**
     * 处理数据完整性约束违反异常（如唯一键冲突、外键约束等）
     * 返回 409 冲突状态码
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        // 仅记录异常类型，不记录完整消息（可能包含 SQL 语句和表结构）
        log.warn("Data integrity violation: {}", e.getClass().getSimpleName());
        return ResponseEntity.status(409).body(ApiResponse.error("数据冲突，请检查输入后重试"));
    }

    /**
     * 处理请求体 JSON 解析异常（如格式错误、类型不匹配）
     * 返回 400 状态码
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Request body parse error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("请求参数格式错误"));
    }

    /**
     * 处理缺少请求参数异常
     * 返回 400 状态码
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.debug("Missing parameter: {}", e.getParameterName());
        return ResponseEntity.badRequest().body(ApiResponse.error("缺少必要参数"));
    }

    /**
     * 处理参数类型不匹配异常（如将字符串传给 Integer 参数）
     * 返回 400 状态码
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.debug("Type mismatch for parameter: {}", e.getName());
        return ResponseEntity.badRequest().body(ApiResponse.error("参数类型错误"));
    }

    /**
     * 兜底异常处理器，捕获所有未处理的异常
     * 记录错误日志，返回500通用错误信息，避免泄露内部细节
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("服务器内部错误"));
    }
}
