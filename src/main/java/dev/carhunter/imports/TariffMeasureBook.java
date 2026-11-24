package dev.carhunter.imports;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Exact-match tariff resolver. It never falls back to a parent CN code or a stale measure. */
public class TariffMeasureBook {

    public TariffResolution resolve(
            List<TariffMeasureRecord> records,
            VehicleClassificationAssessment classification,
            OriginAssessment origin,
            LocalDate importDate) {
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(importDate, "importDate");

        if (classification == null || !classification.isConfirmed()) {
            return TariffResolution.incomplete(classification != null
                    && classification.status() == VehicleClassificationStatus.OUT_OF_SCOPE
                    ? TariffResolutionStatus.CLASSIFICATION_OUT_OF_SCOPE
                    : TariffResolutionStatus.CLASSIFICATION_UNRESOLVED);
        }
        if (origin == null || origin.status() == OriginAssessmentStatus.UNVERIFIED) {
            return TariffResolution.incomplete(TariffResolutionStatus.ORIGIN_UNVERIFIED);
        }
        if (origin.status() == OriginAssessmentStatus.CONFLICTING) {
            return TariffResolution.incomplete(TariffResolutionStatus.ORIGIN_CONFLICTING);
        }

        List<TariffMeasureRecord> matches = records.stream()
                .filter(record -> record.classificationCode().equals(classification.classificationCode()))
                .filter(record -> record.originCountryCode().equals(origin.countryCode()))
                .filter(record -> record.isEffectiveOn(importDate))
                .toList();

        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple tariff measures apply for "
                    + classification.classificationCode() + " / " + origin.countryCode()
                    + " on " + importDate);
        }
        if (matches.isEmpty()) {
            return TariffResolution.incomplete(TariffResolutionStatus.NO_APPLICABLE_MEASURE);
        }

        TariffMeasureRecord measure = matches.getFirst();
        if (measure.requiresUsDirectTransportEvidence()
                && !origin.hasRequiredUsTransportEvidence()) {
            return TariffResolution.incomplete(TariffResolutionStatus.DIRECT_TRANSPORT_UNPROVEN);
        }
        return TariffResolution.found(measure);
    }
}
