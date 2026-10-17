package com.catalogsync;

import java.util.Map;

public class FullCatalogSync extends CatalogSync {
    private final Map<SupplierSku, Product> catalog;

    public FullCatalogSync(SupplierCatalogSource source, Map<SupplierSku, Product> catalog) {
        super(source);
        this.catalog = java.util.Objects.requireNonNull(catalog);
    }

    @Override
    protected void afterSync(Product product) {
        catalog.put(product.sku(), product);
        System.out.println("[FULL] Stored " + product.sku() + ": " + product.name()
                + ", price=" + product.price() + ", stock=" + product.stockQuantity());
    }
}
