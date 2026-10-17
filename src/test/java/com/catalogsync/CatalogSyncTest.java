package com.catalogsync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static com.catalogsync.CatalogSyncException.Code.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogSyncTest {
    private final SupplierSku sku = new SupplierSku("A-1001");
    private final Product product = new Product(sku, "Mouse", 19.99, 120);

    /** A hand-written stub and spy; no external supplier is contacted. */
    private static class StubSource implements SupplierCatalogSource {
        private final String prefix;
        Product result;
        CatalogSyncException failure;
        SupplierSku requested;
        int calls;

        StubSource(String prefix, Product result) { this.prefix = prefix; this.result = result; }
        public String supplierPrefix() { return prefix; }
        public Product fetchProduct(SupplierSku sku) throws CatalogSyncException {
            requested = sku;
            calls++;
            if (failure != null) throw failure;
            return result;
        }
    }

    @Test void fullSyncDelegatesAndStoresAllFields() throws Exception {
        StubSource source = new StubSource("A", product);
        Map<SupplierSku, Product> catalog = new HashMap<>();
        new FullCatalogSync(source, catalog).syncProduct(sku);
        assertEquals(1, source.calls);
        assertEquals(sku, source.requested);
        assertEquals(product, catalog.get(sku));
    }

    @Test void fullSyncReplacesAnExistingProduct() throws Exception {
        StubSource source = new StubSource("A", product);
        Map<SupplierSku, Product> catalog = new HashMap<>();
        FullCatalogSync sync = new FullCatalogSync(source, catalog);
        sync.syncProduct(sku);
        source.result = new Product(sku, "Updated Mouse", 21.0, 90);
        sync.syncProduct(sku);
        assertEquals(1, catalog.size());
        assertEquals(source.result, catalog.get(sku));
    }

    @Test void incrementalSyncDelegatesAndRecordsFirstPrice() throws Exception {
        StubSource source = new StubSource("A", product);
        Map<SupplierSku, Double> history = new HashMap<>();
        new IncrementalCatalogSync(source, history).syncProduct(sku);
        assertEquals(1, source.calls);
        assertEquals(sku, source.requested);
        assertEquals(19.99, history.get(sku));
    }

    @Test void incrementalSyncUpdatesChangedPrice() throws Exception {
        StubSource source = new StubSource("A", product);
        Map<SupplierSku, Double> history = new HashMap<>(Map.of(sku, 10.0));
        new IncrementalCatalogSync(source, history).syncProduct(sku);
        assertEquals(19.99, history.get(sku));
    }

    @Test void incrementalSyncDoesNotWriteAnUnchangedPrice() throws Exception {
        // An immutable map would throw if the unchanged branch attempted to write.
        StubSource source = new StubSource("A", product);
        Map<SupplierSku, Double> history = Map.of(sku, 19.99);
        new IncrementalCatalogSync(source, history).syncProduct(sku);
        assertEquals(19.99, history.get(sku));
    }

    @Test void historyIsSharedAcrossSyncObjects() throws Exception {
        StubSource source = new StubSource("A", product);
        Map<SupplierSku, Double> history = new HashMap<>();
        new IncrementalCatalogSync(source, history).syncProduct(sku);
        source.result = new Product(sku, "Mouse", 20.0, 120);
        new IncrementalCatalogSync(source, history).syncProduct(sku);
        assertEquals(20.0, history.get(sku));
    }

    @Test void bothAbstractionsPropagateFailureWithoutChangingState() {
        StubSource source = new StubSource("A", product);
        source.failure = new CatalogSyncException(SOURCE_FAILURE, "Unavailable");
        Map<SupplierSku, Product> catalog = new HashMap<>();
        Map<SupplierSku, Double> history = new HashMap<>();
        assertSame(source.failure, assertThrows(CatalogSyncException.class,
                () -> new FullCatalogSync(source, catalog).syncProduct(sku)));
        assertSame(source.failure, assertThrows(CatalogSyncException.class,
                () -> new IncrementalCatalogSync(source, history).syncProduct(sku)));
        assertTrue(catalog.isEmpty());
        assertTrue(history.isEmpty());
    }

    private LegacyEdiSupplierCSource adapter(String row) {
        return new LegacyEdiSupplierCSource(new LegacyEdiCatalogParser(Map.of("C-10234", row)));
    }

    private void assertAdapterFailure(LegacyEdiSupplierCSource adapter, CatalogSyncException.Code code) {
        CatalogSyncException failure = assertThrows(CatalogSyncException.class,
                () -> adapter.fetchProduct(new SupplierSku("C-10234")));
        assertEquals(code, failure.getCode());
        assertNull(failure.getCause(), "Legacy exception details must not leak");
        assertFalse(failure.getMessage().contains("EDI"));
    }

    @Test void adapterConvertsValidData() throws Exception {
        Product actual = adapter("C-10234;Wheel;14.75;300")
                .fetchProduct(new SupplierSku("C-10234"));
        assertEquals(new Product(new SupplierSku("C-10234"), "Wheel", 14.75, 300), actual);
    }

    @Test void adapterTranslatesMissingRow() {
        assertAdapterFailure(new LegacyEdiSupplierCSource(new LegacyEdiCatalogParser(Map.of())), NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "bad;data", "C-10234;Wheel;14.75;300;extra",
            "C-10234;Wheel;bad;300", "C-10234;Wheel;14.75;bad", "C-10234;Wheel;NaN;300",
            "C-10234;Wheel;Infinity;300", "C-10234;Wheel;-1;300", "C-10234;Wheel;14.75;-1",
            "C-10234;;14.75;300", "C-OTHER;Wheel;14.75;300", "C-10234;Wheel;14.75;999999999999"})
    void adapterRejectsMalformedOrInvalidRows(String row) {
        assertAdapterFailure(adapter(row), INVALID_DATA);
    }

    @Test void adapterTranslatesLookupFailureFromAStub() {
        LegacyEdiCatalogParser stub = new LegacyEdiCatalogParser(Map.of()) {
            @Override public String getRawLineForSku(String sku) {
                throw new IllegalStateException("secret parser detail");
            }
        };
        assertAdapterFailure(new LegacyEdiSupplierCSource(stub), SOURCE_FAILURE);
    }

    @Test void adapterTranslatesParseFailureFromAStub() {
        LegacyEdiCatalogParser stub = new LegacyEdiCatalogParser(Map.of("C-10234", "row")) {
            @Override public String[] parseLine(String row) { throw new IllegalStateException("private detail"); }
        };
        assertAdapterFailure(new LegacyEdiSupplierCSource(stub), SOURCE_FAILURE);
    }

    @Test void adapterTranslatesEmptySentinelFromAStub() {
        LegacyEdiCatalogParser stub = new LegacyEdiCatalogParser(Map.of("C-10234", "row")) {
            @Override public String[] parseLine(String row) { return new String[0]; }
        };
        assertAdapterFailure(new LegacyEdiSupplierCSource(stub), INVALID_DATA);
    }

    @Test void adapterDefensivelyRejectsNullParserOutput() {
        LegacyEdiCatalogParser stub = new LegacyEdiCatalogParser(Map.of("C-10234", "row")) {
            @Override public String[] parseLine(String row) { return null; }
        };
        assertAdapterFailure(new LegacyEdiSupplierCSource(stub), INVALID_DATA);
    }

    @Test void serviceConfigurationLoadsAndRoutesExactlyThreeSources() throws Exception {
        List<SupplierCatalogSource> providers = ServiceLoader.load(SupplierCatalogSource.class)
                .stream().map(ServiceLoader.Provider::get).toList();
        assertEquals(3, providers.size());
        SupplierSourceFactory factory = new SupplierSourceFactory(providers);
        assertInstanceOf(LocalSupplierASource.class, factory.resolveSource(new SupplierSku("A-1001")));
        assertInstanceOf(LocalSupplierBSource.class, factory.resolveSource(new SupplierSku("B-2002")));
        assertInstanceOf(LegacyEdiSupplierCSource.class, factory.resolveSource(new SupplierSku("C-10234")));
    }

    @Test void unknownSupplierUsesCommonFailureContract() {
        SupplierSourceFactory factory = new SupplierSourceFactory(List.of());
        assertEquals(UNKNOWN_SOURCE, assertThrows(CatalogSyncException.class,
                () -> factory.resolveSource(new SupplierSku("Z-1"))).getCode());
    }

    @Test void duplicatePrefixesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SupplierSourceFactory(
                List.of(new StubSource("A", product), new StubSource("A", product))));
    }

    @Test void newSourceAndNewAbstractionWorkWithoutChangingExistingClasses() throws Exception {
        SupplierSku d = new SupplierSku("D-1");
        StubSource supplierD = new StubSource("D", new Product(d, "Extra", 5, 7));
        SupplierSourceFactory factory = new SupplierSourceFactory(List.of(supplierD));
        class StockOnlySync extends CatalogSync {
            int stock;
            StockOnlySync(SupplierCatalogSource source) { super(source); }
            protected void afterSync(Product product) { stock = product.stockQuantity(); }
        }
        StockOnlySync sync = new StockOnlySync(factory.resolveSource(d));
        sync.syncProduct(d);
        assertEquals(7, sync.stock);
        assertEquals(1, supplierD.calls);
    }

    @Test void bothAbstractionsWorkWithEveryRealSource() throws Exception {
        for (SupplierCatalogSource source : List.of(new LocalSupplierASource(),
                new LocalSupplierBSource(), new LegacyEdiSupplierCSource())) {
            SupplierSku id = new SupplierSku(switch (source.supplierPrefix()) {
                case "A" -> "A-1001";
                case "B" -> "B-2002";
                default -> "C-10234";
            });
            Map<SupplierSku, Product> catalog = new HashMap<>();
            Map<SupplierSku, Double> history = new HashMap<>();
            new FullCatalogSync(source, catalog).syncProduct(id);
            new IncrementalCatalogSync(source, history).syncProduct(id);
            assertEquals(catalog.get(id).price(), history.get(id));
        }
    }

    @Test void nativeSourcesAlsoUseTheCommonMissingProductFailure() {
        for (SupplierCatalogSource source : List.of(new LocalSupplierASource(), new LocalSupplierBSource())) {
            assertEquals(NOT_FOUND, assertThrows(CatalogSyncException.class,
                    () -> source.fetchProduct(new SupplierSku(source.supplierPrefix() + "-missing"))).getCode());
        }
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {"SKU1", "A-", "a-1001", " A-1001", "A-1;bad"})
    void invalidInputIsRejectedBeforeSupplierSelection(String value) {
        assertThrows(IllegalArgumentException.class, () -> new SupplierSku(value));
    }
}
