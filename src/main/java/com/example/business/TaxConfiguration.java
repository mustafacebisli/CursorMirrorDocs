package com.example.business;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TaxConfiguration {
    Optional<BigDecimal> rateFor(UUID customerId);
}
