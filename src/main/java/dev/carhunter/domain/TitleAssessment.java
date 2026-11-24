package dev.carhunter.domain;
import java.util.List;
public record TitleAssessment(TitleStatus status, String reportedTitle, String explanation, List<EvidenceReference> evidence) {
    public TitleAssessment { evidence = evidence == null ? List.of() : List.copyOf(evidence); }
    public boolean eligibleForCleanTitleShortlist() { return status == TitleStatus.CLEAN_EVIDENCED; }
}
