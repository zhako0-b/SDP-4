package com.catalogsync;

import java.util.HashMap;
import java.util.Map;

public class SupplierSourceFactory {
    private final Map<String, SupplierCatalogSource> sources;

    public SupplierSourceFactory(Iterable<SupplierCatalogSource> providers) {
        Map<String, SupplierCatalogSource> registered = new HashMap<>();
        for (SupplierCatalogSource source : providers) {
            String prefix = source.supplierPrefix();
            if (prefix == null || !prefix.matches("[A-Z][A-Z0-9]*")) {
                throw new IllegalArgumentException("Invalid supplier prefix: " + prefix);
            }
            if (registered.putIfAbsent(prefix, source) != null) {
                throw new IllegalArgumentException("Duplicate supplier prefix: " + prefix);
            }
        }
        sources = Map.copyOf(registered);
    }

    public SupplierCatalogSource resolveSource(SupplierSku sku) throws CatalogSyncException {
        SupplierCatalogSource source = sources.get(sku.supplierPrefix());
        if (source == null) {
            throw new CatalogSyncException(CatalogSyncException.Code.UNKNOWN_SOURCE,
                    "No source registered for supplier: " + sku.supplierPrefix());
        }
        return source;
    }
}
