package com.example.business;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Order {
    private final UUID id;
    private final UUID customerId;
    private final List<OrderLine> lines;
    private final BigDecimal subtotal;
    private final BigDecimal shipping;
    private final BigDecimal total;
    private final OrderStatus status;
    private final Instant placedAt;
    private final String trackingCode;

    public Order(UUID id, UUID customerId, List<OrderLine> lines,
                 BigDecimal subtotal, BigDecimal shipping, BigDecimal total,
                 OrderStatus status, Instant placedAt) {
        this(id, customerId, lines, subtotal, shipping, total, status, placedAt, null);
    }

    private Order(UUID id, UUID customerId, List<OrderLine> lines,
                  BigDecimal subtotal, BigDecimal shipping, BigDecimal total,
                  OrderStatus status, Instant placedAt, String trackingCode) {
        this.id = id;
        this.customerId = customerId;
        this.lines = List.copyOf(lines);
        this.subtotal = subtotal;
        this.shipping = shipping;
        this.total = total;
        this.status = status;
        this.placedAt = placedAt;
        this.trackingCode = trackingCode;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public List<OrderLine> getLines() { return lines; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShipping() { return shipping; }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public Instant getPlacedAt() { return placedAt; }
    public String getTrackingCode() { return trackingCode; }

    public Order withStatus(OrderStatus s) {
        return new Order(id, customerId, lines, subtotal, shipping, total, s, placedAt, trackingCode);
    }
    public Order withTrackingCode(String code) {
        return new Order(id, customerId, lines, subtotal, shipping, total, status, placedAt, code);
    }
}
