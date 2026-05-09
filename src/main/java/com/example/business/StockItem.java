package com.example.business;

import java.time.Instant;

public final class StockItem {
    private final String sku;
    private final int onHand;
    private final int reserved;
    private final Instant updatedAt;

    public StockItem(String sku, int onHand, int reserved, Instant updatedAt) {
        this.sku = sku;
        this.onHand = onHand;
        this.reserved = reserved;
        this.updatedAt = updatedAt;
    }

    public String getSku() { return sku; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
    public Instant getUpdatedAt() { return updatedAt; }

    public StockItem withOnHand(int v) {
        return new StockItem(sku, v, reserved, Instant.now());
    }
    public StockItem withReserved(int v) {
        return new StockItem(sku, onHand, v, Instant.now());
    }
}
