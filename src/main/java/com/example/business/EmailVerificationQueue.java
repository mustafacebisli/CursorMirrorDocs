package com.example.business;

import java.util.UUID;

public interface EmailVerificationQueue {
    void enqueueVerification(UUID customerId, String email);
    boolean consumeToken(UUID customerId, String token);
}
