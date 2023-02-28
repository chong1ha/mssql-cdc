package com.example.mssqlcdc.config;

/**
 * @author gunha
 * @version 0.1
 * @since 2023-02-28 오후 5:24
 */
public enum CDCOperationType {
    DELETE(1),
    INSERT(2),
    UPDATE_OLD(3),
    UPDATE_NEW(4);

    private final int code;

    CDCOperationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
