package com.frauddetection.payment.dto;

import com.frauddetection.common.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CreateTransactionRequest(
        @NotNull(message = "accountId is required")
        UUID accountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "currency is required")
        Currency currency,

        @NotBlank(message = "country is required")
        @Pattern(regexp = "^[A-Z]{2}$", message = "country must be an ISO 3166-1 alpha-2 code, for example US")
        String country,

        @NotBlank(message = "merchantCategory is required")
        String merchantCategory
) {
}
