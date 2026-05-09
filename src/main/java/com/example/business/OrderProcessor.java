package com.example.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderProcessor {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("19.90");

    private final CustomerService customerService;
    private final InventoryManager inventoryManager;
    private final InvoiceGenerator invoiceGenerator;
    private final AuditLogger auditLogger;

    public OrderProcessor(CustomerService customerService,
                          InventoryManager inventoryManager,
                          InvoiceGenerator invoiceGenerator,
                          AuditLogger auditLogger) {
        this.customerService = customerService;
        this.inventoryManager = inventoryManager;
        this.invoiceGenerator = invoiceGenerator;
        this.auditLogger = auditLogger;
    }

    public Order placeOrder(UUID customerId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one line");
        }

        customerService.findById(customerId)
                .orElseThrow(() -> new IllegalStateException("Unknown customer: " + customerId));

        for (OrderLine line : lines) {
            inventoryManager.reserve(line.getSku(), line.getQuantity());
        }

        BigDecimal subtotal = lines.stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : SHIPPING_FEE;

        BigDecimal total = subtotal.add(shipping).setScale(2, RoundingMode.HALF_UP);

        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, customerId, lines, subtotal, shipping, total,
                OrderStatus.CONFIRMED, Instant.now());

        invoiceGenerator.generateFor(order);
        auditLogger.log("order.placed", orderId.toString());
        return order;
    }

    public Order cancel(Order order, String reason) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel a shipped order");
        }
        for (OrderLine line : order.getLines()) {
            inventoryManager.release(line.getSku(), line.getQuantity());
        }
        Order cancelled = order.withStatus(OrderStatus.CANCELLED);
        auditLogger.log("order.cancelled", order.getId() + ":" + reason);
        return cancelled;
    }

    public Order markShipped(Order order, String trackingCode) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped");
        }
        Order shipped = order.withStatus(OrderStatus.SHIPPED).withTrackingCode(trackingCode);
        auditLogger.log("order.shipped", order.getId() + ":" + trackingCode);
        return shipped;
    }
}
