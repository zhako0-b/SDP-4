package com.catalogsync;

import java.util.Map;

public class LocalSupplierASource implements SupplierCatalogSource {
    private final Map<SupplierSku, Product> catalog;

    public LocalSupplierASource() {
        this(Map.of(new SupplierSku("A-1001"),
                new Product(new SupplierSku("A-1001"), "Bluetooth Mouse", 19.99, 120)));
    }

    public LocalSupplierASource(Map<SupplierSku, Product> catalog) {
        this.catalog = Map.copyOf(catalog);
        this.catalog.forEach((sku, product) -> {
            if (!sku.equals(product.sku()) || !sku.supplierPrefix().equals("A")) {
                throw new IllegalArgumentException("Supplier A catalog has an inconsistent SKU");
            }
        });
    }

    @Override public String supplierPrefix() { return "A"; }

    @Override
    public Product fetchProduct(SupplierSku sku) throws CatalogSyncException {
        Product product = catalog.get(sku);
        if (product == null) {
            throw new CatalogSyncException(CatalogSyncException.Code.NOT_FOUND,
                    "Product not found: " + sku);
        }
        return product;
    }
}
