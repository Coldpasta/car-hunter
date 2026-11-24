package dev.carhunter.domain;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record SearchRequest(
        @NotNull @DecimalMin("1000") BigDecimal budget,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotEmpty List<@NotBlank String> countries,
        @NotBlank String bodyType,
        @NotEmpty List<@NotBlank String> fuel,
        @NotBlank String transmission,
        @Min(1990) @Max(2035) int minYear,
        @Min(0) @Max(1_000_000) int maxMileageKm,
        @Min(1) @Max(8) int candidateLimit
) {}
