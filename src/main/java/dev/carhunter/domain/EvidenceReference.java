package dev.carhunter.domain;
import java.time.Instant;
public record EvidenceReference(String claim, String sourceUrl, String sourceType, Instant retrievedAt, String assessment) {}
