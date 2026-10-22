package com.kw.erp.exception;

/**
 * Ozon Seller API 调用异常
 */
public class OzonApiException extends RuntimeException {

    private final int status;

    public OzonApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public OzonApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = -1;
    }

    public int getStatus() {
        return status;
    }
}
