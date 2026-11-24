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
    public InvestigationReport(UUID id, Instant createdAt, SearchRequest criteria,
                               List<CarCandidate> candidates, String investigation,
                               String skepticReview, String recommendation, List<String> activity) {
        this(id, createdAt, criteria, candidates, investigation, skepticReview, recommendation,
                activity, new ScoutDiagnostics(candidates == null ? 0 : candidates.size(),
                        candidates == null ? 0 : candidates.size(), List.of(), List.of()));
    }

    public record ScoutDiagnostics(
            int discoveredCount,
            int eligibleCount,
            List<String> searchedSources,
            List<Rejection> rejections
    ) {
        public ScoutDiagnostics {
            searchedSources = searchedSources == null ? List.of() : List.copyOf(searchedSources);
            rejections = rejections == null ? List.of() : List.copyOf(rejections);
        }
    }

    public record Rejection(String sourceUrl, String reasonCode, String reason) {}
}
