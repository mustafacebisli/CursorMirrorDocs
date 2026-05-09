package com.example.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Invoice {
    private final UUID id;
    private final String number;
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal taxableBase;
    private final BigDecimal vatRate;
    private final BigDecimal vatAmount;
    private final BigDecimal grandTotal;
    private final Instant issuedAt;
    private final boolean voided;
    private final String voidReason;
    private final Instant voidedAt;

    public Invoice(UUID id, String number, UUID orderId, UUID customerId,
                   BigDecimal taxableBase, BigDecimal vatRate, BigDecimal vatAmount,
                   BigDecimal grandTotal, Instant issuedAt) {
        this(id, number, orderId, customerId, taxableBase, vatRate, vatAmount,
                grandTotal, issuedAt, false, null, null);
    }

    private Invoice(UUID id, String number, UUID orderId, UUID customerId,
                    BigDecimal taxableBase, BigDecimal vatRate, BigDecimal vatAmount,
                    BigDecimal grandTotal, Instant issuedAt,
                    boolean voided, String voidReason, Instant voidedAt) {
        this.id = id;
        this.number = number;
        this.orderId = orderId;
        this.customerId = customerId;
        this.taxableBase = taxableBase;
        this.vatRate = vatRate;
        this.vatAmount = vatAmount;
        this.grandTotal = grandTotal;
        this.issuedAt = issuedAt;
        this.voided = voided;
        this.voidReason = voidReason;
        this.voidedAt = voidedAt;
    }

    public UUID getId() { return id; }
    public String getNumber() { return number; }
    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getTaxableBase() { return taxableBase; }
    public BigDecimal getVatRate() { return vatRate; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public Instant getIssuedAt() { return issuedAt; }
    public boolean isVoided() { return voided; }
    public String getVoidReason() { return voidReason; }
    public Instant getVoidedAt() { return voidedAt; }

    public Invoice markVoided(String reason, Instant at) {
        return new Invoice(id, number, orderId, customerId, taxableBase, vatRate,
                vatAmount, grandTotal, issuedAt, true, reason, at);
    }
}
