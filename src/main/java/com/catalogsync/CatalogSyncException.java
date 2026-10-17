package com.catalogsync;


public class CatalogSyncException extends Exception {
    public enum Code { NOT_FOUND, INVALID_DATA, SOURCE_FAILURE, UNKNOWN_SOURCE }

    private final Code code;

    public CatalogSyncException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() { return code; }
}
