package com.catalogsync;

import java.util.List;

public class LocalSupplierBSource implements SupplierCatalogSource {
    private final List<Product> priceList;

    public LocalSupplierBSource() {
        this(List.of(new Product(new SupplierSku("B-2002"), "Mechanical Keyboard", 79.50, 40)));
    }

    public LocalSupplierBSource(List<Product> priceList) {
        this.priceList = List.copyOf(priceList);
        if (this.priceList.stream().anyMatch(p -> !p.sku().supplierPrefix().equals("B"))
                || this.priceList.stream().map(Product::sku).distinct().count() != this.priceList.size()) {
            throw new IllegalArgumentException("Supplier B price list has inconsistent or duplicate SKUs");
        }
    }

    @Override public String supplierPrefix() { return "B"; }

    @Override
    public Product fetchProduct(SupplierSku sku) throws CatalogSyncException {
        return priceList.stream().filter(product -> product.sku().equals(sku)).findFirst()
                .orElseThrow(() -> new CatalogSyncException(CatalogSyncException.Code.NOT_FOUND,
                        "Product not found: " + sku));
    }
}
