package com.catalogsync;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class Client {
    public static void main(String[] args) {
        if (args.length < 2 || !(args[0].equals("full") || args[0].equals("incremental"))) {
            System.err.println("Usage: java -jar target/catalog-sync.jar <full|incremental> <SKU> [SKU...]");
            System.exit(2);
        }
        SupplierSourceFactory factory = new SupplierSourceFactory(
                ServiceLoader.load(SupplierCatalogSource.class));
        Map<SupplierSku, Product> catalog = new HashMap<>();
        Map<SupplierSku, Double> priceHistory = new HashMap<>();
        boolean failed = false;
        for (int i = 1; i < args.length; i++) {
            try {
                SupplierSku sku = new SupplierSku(args[i]);
                SupplierCatalogSource source = factory.resolveSource(sku);
                CatalogSync sync = args[0].equals("full")
                        ? new FullCatalogSync(source, catalog)
                        : new IncrementalCatalogSync(source, priceHistory);
                sync.syncProduct(sku);
            } catch (IllegalArgumentException invalidInput) {
                System.err.println("Invalid input: " + invalidInput.getMessage());
                failed = true;
            } catch (CatalogSyncException failure) {
                System.err.println(failure.getCode() + ": " + failure.getMessage());
                failed = true;
            }
        }
        if (failed) System.exit(1);
    }
}

// add a new ref abstraction .show it working with every existing implementation, adapted one included,unchanged.