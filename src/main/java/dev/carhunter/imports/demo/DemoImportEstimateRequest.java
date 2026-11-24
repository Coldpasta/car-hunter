package dev.carhunter.imports.demo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Inputs for the hackathon-only illustrative estimator.
 * All monetary inputs except the FX rate are in USD; output is in PLN.
 */
public record DemoImportEstimateRequest(
        @NotNull @DecimalMin(value = "1.00") BigDecimal vehiclePriceUsd,
        @NotNull @DecimalMin("0.00") BigDecimal usInlandTransportUsd,
        @NotNull @DecimalMin("0.00") BigDecimal freightUsd,
        @NotNull @DecimalMin("0.00") BigDecimal insuranceUsd,
        @NotNull @DecimalMin("0.00") BigDecimal destinationHandlingPln,
        @NotNull @DecimalMin("0.00") BigDecimal complianceAndModificationsPln,
        @NotNull @DecimalMin("0.00") BigDecimal registrationPln,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal usdToPln,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal assumedDutyRatePercent,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal assumedExciseRatePercent,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal assumedVatRatePercent
) {}
