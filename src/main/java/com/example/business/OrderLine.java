package com.example.business;

import java.math.BigDecimal;

public final class OrderLine {
    private final String sku;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderLine(String sku, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
