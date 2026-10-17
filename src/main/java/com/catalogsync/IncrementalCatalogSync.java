package com.catalogsync;

import java.util.Map;

public class IncrementalCatalogSync extends CatalogSync {
    private final Map<SupplierSku, Double> lastKnownPrices;

    public IncrementalCatalogSync(SupplierCatalogSource source, Map<SupplierSku, Double> lastKnownPrices) {
        super(source);
        this.lastKnownPrices = java.util.Objects.requireNonNull(lastKnownPrices);
    }

    @Override
    protected void afterSync(Product product) {
        Double oldPrice = lastKnownPrices.get(product.sku());
        if (oldPrice == null || Double.compare(oldPrice, product.price()) != 0) {
            lastKnownPrices.put(product.sku(), product.price());
            System.out.println("[INCREMENTAL] " + product.sku() + ": " + oldPrice + " -> " + product.price());
        } else {
            System.out.println("[INCREMENTAL] " + product.sku() + ": unchanged");
        }
    }
}
