package com.catalogsync;

import java.util.Map;

/** Legacy parser with an incompatible API. Adapted by LegacyEdiSupplierCSource. */
public class LegacyEdiCatalogParser {

    private final Map<String, String> rawCsvLinesBySku;

    public LegacyEdiCatalogParser(Map<String, String> rawCsvLinesBySku) {
        this.rawCsvLinesBySku = rawCsvLinesBySku;
    }

    public String[] parseLine(String rawCsvRow) {
        if (rawCsvRow == null || rawCsvRow.isBlank()) {
            return new String[0];
        }
        String[] fields = rawCsvRow.split(";");
        if (fields.length != 4) {
            return new String[0];
        }
        return fields;
    }

    public String getRawLineForSku(String sku) {
        return rawCsvLinesBySku.get(sku);
    }
}
