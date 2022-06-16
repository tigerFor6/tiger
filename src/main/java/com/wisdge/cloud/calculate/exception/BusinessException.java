package com.wisdge.cloud.calculate.exception;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {

    private int code;

    private String message;

    public BusinessException(int code) {
        this.code = code;
    }

    public BusinessException(String message) {
        this.message = message;
    }

    public BusinessException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
