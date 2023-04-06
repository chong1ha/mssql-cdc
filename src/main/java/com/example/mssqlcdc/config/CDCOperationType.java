package com.example.mssqlcdc.config;

/**
 * 변경 데이터의 동작 종류 Enum, 데이터 파싱 시 사용
 *
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

    /**
     * 생성자
     *
     * @param code Operation Code(동작 종류)
     */
    CDCOperationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
