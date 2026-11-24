package dev.carhunter.imports.demo;

import java.math.BigDecimal;
import java.util.List;

public record DemoImportEstimateResponse(
        String destinationCountry,
        String currency,
        String status,
        BigDecimal usdToPln,
        BigDecimal customsValueEstimatePln,
        BigDecimal dutyEstimatePln,
        BigDecimal exciseBaseEstimatePln,
        BigDecimal exciseEstimatePln,
        BigDecimal vatBaseEstimatePln,
        BigDecimal vatEstimatePln,
        BigDecimal estimatedTotalPln,
        List<CostLine> breakdown,
        List<String> assumptions,
        List<String> limitations
) {
    public DemoImportEstimateResponse {
        breakdown = List.copyOf(breakdown);
        assumptions = List.copyOf(assumptions);
        limitations = List.copyOf(limitations);
    }

    public record CostLine(String code, String label, BigDecimal amountPln, String calculation) {}
}
