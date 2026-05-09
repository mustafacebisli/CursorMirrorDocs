package com.example.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class InvoiceGenerator {

    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("0.20");

    private final Map<UUID, Invoice> invoicesByOrder = new HashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);
    private final TaxConfiguration taxConfiguration;
    private final AuditLogger auditLogger;

    public InvoiceGenerator(TaxConfiguration taxConfiguration, AuditLogger auditLogger) {
        this.taxConfiguration = taxConfiguration;
        this.auditLogger = auditLogger;
    }

    public Invoice generateFor(Order order) {
        Invoice existing = invoicesByOrder.get(order.getId());
        if (existing != null) {
            return existing;
        }

        BigDecimal vatRate = taxConfiguration.rateFor(order.getCustomerId())
                .orElse(DEFAULT_VAT_RATE);

        BigDecimal taxableBase = order.getSubtotal();
        BigDecimal vatAmount = taxableBase.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = order.getTotal().add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        String number = formatInvoiceNumber(LocalDate.now(), sequence.getAndIncrement());
        Invoice invoice = new Invoice(
                UUID.randomUUID(),
                number,
                order.getId(),
                order.getCustomerId(),
                taxableBase,
                vatRate,
                vatAmount,
                grandTotal,
                Instant.now()
        );
        invoicesByOrder.put(order.getId(), invoice);
        auditLogger.log("invoice.generated", number);
        return invoice;
    }

    public Invoice voidInvoice(UUID orderId, String reason) {
        Invoice existing = invoicesByOrder.get(orderId);
        if (existing == null) {
            throw new IllegalStateException("No invoice for order: " + orderId);
        }
        if (existing.isVoided()) {
            return existing;
        }
        Invoice voided = existing.markVoided(reason, Instant.now());
        invoicesByOrder.put(orderId, voided);
        auditLogger.log("invoice.voided", existing.getNumber() + ":" + reason);
        return voided;
    }

    public Invoice findByOrder(UUID orderId) {
        return invoicesByOrder.get(orderId);
    }

    private String formatInvoiceNumber(LocalDate date, long seq) {
        return String.format("INV-%d-%05d", date.getYear(), seq);
    }
}
