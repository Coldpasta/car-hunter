package dev.carhunter.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvestigationReport(
        UUID id,
        Instant createdAt,
        SearchRequest criteria,
        List<CarCandidate> candidates,
        String investigation,
        String skepticReview,
        String recommendation,
        List<String> activity,
        ScoutDiagnostics scoutDiagnostics
) {
    public record ScoutDiagnostics(
            int discoveredCount,
            int eligibleCount,
            List<String> searchedSources,
            List<Rejection> rejections
    ) {}

    public record Rejection(
            String sourceUrl,
            String reasonCode,
            String reason
    ) {}
}
