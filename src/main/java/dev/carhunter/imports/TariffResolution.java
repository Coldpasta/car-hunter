package dev.carhunter.imports;

import java.util.Optional;

public record TariffResolution(TariffResolutionStatus status, Optional<TariffMeasureRecord> measure) {
    public TariffResolution {
        if (status == null || measure == null) {
            throw new IllegalArgumentException("Tariff resolution status and measure are required");
        }
        if ((status == TariffResolutionStatus.FOUND) != measure.isPresent()) {
            throw new IllegalArgumentException("Only FOUND resolutions may contain a tariff measure");
        }
    }

    public static TariffResolution found(TariffMeasureRecord measure) {
        return new TariffResolution(TariffResolutionStatus.FOUND, Optional.of(measure));
    }

    public static TariffResolution incomplete(TariffResolutionStatus status) {
        if (status == TariffResolutionStatus.FOUND) {
            throw new IllegalArgumentException("FOUND requires a tariff measure");
        }
        return new TariffResolution(status, Optional.empty());
    }
}
