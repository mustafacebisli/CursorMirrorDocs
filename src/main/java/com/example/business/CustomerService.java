package com.example.business;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CustomerService {

    private final Map<UUID, Customer> store = new HashMap<>();
    private final EmailVerificationQueue emailQueue;
    private final AuditLogger auditLogger;

    public CustomerService(EmailVerificationQueue emailQueue, AuditLogger auditLogger) {
        this.emailQueue = emailQueue;
        this.auditLogger = auditLogger;
    }

    public Customer create(String fullName, String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        boolean duplicate = store.values().stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(email) && c.getDeletedAt() == null);
        if (duplicate) {
            throw new DuplicateEmailException(email);
        }

        UUID id = UUID.randomUUID();
        Customer customer = new Customer(id, fullName, email, Instant.now());
        store.put(id, customer);

        emailQueue.enqueueVerification(id, email);
        auditLogger.log("customer.created", id.toString());
        return customer;
    }

    public Customer update(UUID id, String fullName, String email) {
        Customer existing = requireActive(id);
        boolean emailChanged = !existing.getEmail().equalsIgnoreCase(email);

        Customer updated = existing
                .withFullName(fullName)
                .withEmail(email)
                .withEmailVerifiedAt(emailChanged ? null : existing.getEmailVerifiedAt());

        store.put(id, updated);
        if (emailChanged) {
            emailQueue.enqueueVerification(id, email);
        }
        auditLogger.log("customer.updated", id.toString());
        return updated;
    }

    public void softDelete(UUID id) {
        Customer existing = requireActive(id);
        store.put(id, existing.withDeletedAt(Instant.now()));
        auditLogger.log("customer.softDeleted", id.toString());
    }

    public void deactivate(UUID id) {
        Customer existing = requireActive(id);
        Customer deactivated = existing
                .withEmailVerifiedAt(null)
                .withDeletedAt(Instant.now());
        store.put(id, deactivated);
        auditLogger.log("customer.deactivated", id.toString());
    }

    public Optional<Customer> findById(UUID id) {
        Customer c = store.get(id);
        if (c == null || c.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(c);
    }

    public Customer verifyEmail(UUID id, String token) {
        Customer existing = requireActive(id);
        if (existing.getEmailVerifiedAt() != null) {
            return existing;
        }
        if (!emailQueue.consumeToken(id, token)) {
            throw new IllegalStateException("Invalid or expired verification token");
        }
        Customer verified = existing.withEmailVerifiedAt(Instant.now());
        store.put(id, verified);
        auditLogger.log("customer.emailVerified", id.toString());
        return verified;
    }

    private Customer requireActive(UUID id) {
        Customer c = store.get(id);
        if (c == null || c.getDeletedAt() != null) {
            throw new CustomerNotFoundException(id);
        }
        return c;
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String email) {
            super("Email already in use: " + email);
        }
    }

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(UUID id) {
            super("Customer not found: " + id);
        }
    }
}
