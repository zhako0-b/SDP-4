package com.catalogsync;


public interface SupplierCatalogSource {
    String supplierPrefix();

    Product fetchProduct(SupplierSku sku) throws CatalogSyncException;
}
