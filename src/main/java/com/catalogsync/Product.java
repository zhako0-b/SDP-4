package com.catalogsync;

import java.util.Objects;

public record Product(SupplierSku sku, String name, double price, int stockQuantity) {
    public Product {
        Objects.requireNonNull(sku, "sku");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (!Double.isFinite(price) || price < 0 || stockQuantity < 0) {
            throw new IllegalArgumentException("Price and stock must be valid non-negative values");
        }
    }
}
