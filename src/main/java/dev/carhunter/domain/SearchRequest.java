package dev.carhunter.domain;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** v0.2 request: US is the fixed source market; destination is selected per investigation. */
public record SearchRequest(
        @NotBlank @Pattern(regexp = "US", message = "v0.2 source market must be US") String sourceMarket,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "Use an ISO 3166-1 alpha-2 country code") String destinationCountry,
        @NotNull @DecimalMin("1000") BigDecimal budgetAmount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "Use an ISO 4217 currency code") String budgetCurrency,
        @Size(max = 80) String make,
        @Size(max = 80) String model,
        @Size(max = 80) String bodyType,
        List<@NotBlank String> fuel,
        @Size(max = 40) String transmission,
        @Min(1990) @Max(2035) Integer minYear,
        @Min(1990) @Max(2035) Integer maxYear,
        @Min(0) @Max(1_000_000) Integer maxMileageMiles,
        List<@NotBlank String> sellerTypes,
        @Size(max = 2) String usState,
        @Size(max = 12) String usZipCode,
        @Min(1) @Max(8) int candidateLimit
) {
    @AssertTrue(message = "minYear must not be greater than maxYear")
    public boolean isYearRangeValid() {
        return minYear == null || maxYear == null || minYear <= maxYear;
    }
    @AssertTrue(message = "sellerTypes may contain only dealer and private")
    public boolean isSellerTypesValid() {
        return sellerTypes == null || sellerTypes.stream()
                .allMatch(type -> "dealer".equalsIgnoreCase(type) || "private".equalsIgnoreCase(type));
    }
}
