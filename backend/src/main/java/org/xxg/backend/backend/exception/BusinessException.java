package org.xxg.backend.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 自定义业务异常类
 * 用于表示业务逻辑中的异常情况（如参数错误、资源不存在等），
 * 继承RuntimeException，由全局异常处理器统一捕获并返回标准错误响应。
 */
public class BusinessException extends RuntimeException {

    /** HTTP状态码，默认400 BAD_REQUEST */
    private final HttpStatus httpStatus;

    /**
     * 构造业务异常（默认返回400状态码）
     * @param message 错误描述信息
     */
    public BusinessException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    /**
     * 构造业务异常（指定HTTP状态码）
     * @param message 错误描述信息
     * @param httpStatus 自定义HTTP状态码
     */
    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * 获取异常对应的HTTP状态码
     * @return HTTP状态码
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
