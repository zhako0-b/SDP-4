package com.catalogsync;

import java.util.Objects;

public abstract class CatalogSync {
    protected final SupplierCatalogSource source;

    protected CatalogSync(SupplierCatalogSource source) {
        this.source = Objects.requireNonNull(source);
    }

    public final void syncProduct(SupplierSku sku) throws CatalogSyncException {
        Objects.requireNonNull(sku, "sku");
        Product product = source.fetchProduct(sku);
        afterSync(product);
    }

    protected abstract void afterSync(Product product);
}
