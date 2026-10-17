package com.catalogsync;

import java.util.Map;
import java.util.Objects;

public class LegacyEdiSupplierCSource implements SupplierCatalogSource {
    private final LegacyEdiCatalogParser legacyParser;

    public LegacyEdiSupplierCSource() {
        this(new LegacyEdiCatalogParser(Map.of(
                "C-10234", "C-10234;Industrial Caster Wheel;14.75;300")));
    }

    public LegacyEdiSupplierCSource(LegacyEdiCatalogParser legacyParser) {
        this.legacyParser = Objects.requireNonNull(legacyParser);
    }

    @Override public String supplierPrefix() { return "C"; }

    @Override
    public Product fetchProduct(SupplierSku sku) throws CatalogSyncException {
        Objects.requireNonNull(sku, "sku");
        final String[] fields;
        try {
            String row = legacyParser.getRawLineForSku(sku.value());
            if (row == null) {
                throw new CatalogSyncException(CatalogSyncException.Code.NOT_FOUND,
                        "Product not found: " + sku);
            }
            fields = legacyParser.parseLine(row);
        } catch (RuntimeException failure) {
            // Do not expose the wrapped class, its message, or its exception cause.
            throw new CatalogSyncException(CatalogSyncException.Code.SOURCE_FAILURE,
                    "Supplier could not read product: " + sku);
        }
        if (fields == null || fields.length != 4) {
            throw invalidData(sku);
        }
        try {
            SupplierSku parsedSku = new SupplierSku(fields[0]);
            if (!parsedSku.equals(sku)) {
                throw invalidData(sku);
            }
            return new Product(parsedSku, fields[1],
                    Double.parseDouble(fields[2]), Integer.parseInt(fields[3]));
        } catch (RuntimeException invalidFields) {
            throw invalidData(sku);
        }
    }

    private CatalogSyncException invalidData(SupplierSku sku) {
        return new CatalogSyncException(CatalogSyncException.Code.INVALID_DATA,
                "Supplier returned invalid product data: " + sku);
    }
}
