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
        List<String> activity
) {}
