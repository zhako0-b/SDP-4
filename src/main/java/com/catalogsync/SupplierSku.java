package com.catalogsync;

public record SupplierSku(String value) {
    public SupplierSku {
        if (value == null || !value.matches("[A-Z][A-Z0-9]*-[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("SKU must have a supplier prefix and an identifier, e.g. A-1001");
        }
    }

    public String supplierPrefix() {
        return value.substring(0, value.indexOf('-'));
    }

    @Override
    public String toString() { return value; }
}
