package com.example.business;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InventoryManager {

    private final Map<String, StockItem> stockBySku = new HashMap<>();
    private final AuditLogger auditLogger;
    private final int lowStockThreshold;

    public InventoryManager(AuditLogger auditLogger, int lowStockThreshold) {
        this.auditLogger = auditLogger;
        this.lowStockThreshold = lowStockThreshold;
    }

    public synchronized void registerSku(String sku, int initialQuantity) {
        if (stockBySku.containsKey(sku)) {
            throw new IllegalStateException("SKU already registered: " + sku);
        }
        stockBySku.put(sku, new StockItem(sku, initialQuantity, 0, Instant.now()));
        auditLogger.log("inventory.skuRegistered", sku + ":" + initialQuantity);
    }

    public synchronized void reserve(String sku, int quantity) {
        StockItem item = require(sku);
        int available = item.getOnHand() - item.getReserved();
        if (available < quantity) {
            throw new OutOfStockException(sku, quantity, available);
        }
        stockBySku.put(sku, item.withReserved(item.getReserved() + quantity));
        auditLogger.log("inventory.reserved", sku + ":" + quantity);
        warnIfLow(sku);
    }

    public synchronized void release(String sku, int quantity) {
        StockItem item = require(sku);
        int newReserved = Math.max(0, item.getReserved() - quantity);
        stockBySku.put(sku, item.withReserved(newReserved));
        auditLogger.log("inventory.released", sku + ":" + quantity);
    }

    public synchronized void commit(String sku, int quantity) {
        StockItem item = require(sku);
        if (item.getReserved() < quantity) {
            throw new IllegalStateException(
                    "Cannot commit more than reserved for SKU " + sku);
        }
        StockItem next = item
                .withOnHand(item.getOnHand() - quantity)
                .withReserved(item.getReserved() - quantity);
        stockBySku.put(sku, next);
        auditLogger.log("inventory.committed", sku + ":" + quantity);
        warnIfLow(sku);
    }

    public synchronized void restock(String sku, int quantity) {
        StockItem item = require(sku);
        stockBySku.put(sku, item.withOnHand(item.getOnHand() + quantity));
        auditLogger.log("inventory.restocked", sku + ":" + quantity);
    }

    public Optional<StockItem> get(String sku) {
        return Optional.ofNullable(stockBySku.get(sku));
    }

    private StockItem require(String sku) {
        StockItem item = stockBySku.get(sku);
        if (item == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        return item;
    }

    private void warnIfLow(String sku) {
        StockItem item = stockBySku.get(sku);
        int available = item.getOnHand() - item.getReserved();
        if (available <= lowStockThreshold) {
            auditLogger.log("inventory.lowStock", sku + ":" + available);
        }
    }

    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException(String sku, int requested, int available) {
            super("Out of stock for " + sku + " (requested=" + requested
                    + ", available=" + available + ")");
        }
    }
}
