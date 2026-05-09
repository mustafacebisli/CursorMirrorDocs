package com.example.business;

import java.time.Instant;
import java.util.UUID;

public final class Customer {
    private final UUID id;
    private final String fullName;
    private final String email;
    private final Instant createdAt;
    private final Instant emailVerifiedAt;
    private final Instant deletedAt;

    public Customer(UUID id, String fullName, String email, Instant createdAt) {
        this(id, fullName, email, createdAt, null, null);
    }

    private Customer(UUID id, String fullName, String email, Instant createdAt,
                     Instant emailVerifiedAt, Instant deletedAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = createdAt;
        this.emailVerifiedAt = emailVerifiedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public Customer withFullName(String v) {
        return new Customer(id, v, email, createdAt, emailVerifiedAt, deletedAt);
    }
    public Customer withEmail(String v) {
        return new Customer(id, fullName, v, createdAt, emailVerifiedAt, deletedAt);
    }
    public Customer withEmailVerifiedAt(Instant v) {
        return new Customer(id, fullName, email, createdAt, v, deletedAt);
    }
    public Customer withDeletedAt(Instant v) {
        return new Customer(id, fullName, email, createdAt, emailVerifiedAt, v);
    }
}
