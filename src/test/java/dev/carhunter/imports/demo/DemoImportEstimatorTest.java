package dev.carhunter.imports.demo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DemoImportEstimatorTest {
    private final DemoImportEstimator estimator = new DemoImportEstimator();

    @Test
    void calculatesTransparentIllustrativeBreakdownDeterministically() {
        var request = new DemoImportEstimateRequest(
                bd("20000"), bd("1000"), bd("2000"), bd("200"),
                bd("1500"), bd("3000"), bd("500"), bd("4.00"),
                bd("0"), bd("3.1"), bd("23"));

        var result = estimator.estimate(request);

        assertThat(result.status()).isEqualTo("ILLUSTRATIVE_ONLY");
        assertThat(result.currency()).isEqualTo("PLN");
        assertThat(result.customsValueEstimatePln()).isEqualByComparingTo("92800.00");
        assertThat(result.dutyEstimatePln()).isEqualByComparingTo("0.00");
        assertThat(result.exciseEstimatePln()).isEqualByComparingTo("2876.80");
        assertThat(result.vatEstimatePln()).isEqualByComparingTo("22005.66");
        assertThat(result.estimatedTotalPln()).isEqualByComparingTo("122682.46");
        assertThat(result.breakdown()).hasSize(10);
        assertThat(result.limitations()).anyMatch(value -> value.contains("TARIC/ISZTAR4"));
    }

    @Test
    void usesExplicitDutyAssumptionAndAddsNonTaxLocalCostsToTotal() {
        var request = new DemoImportEstimateRequest(
                bd("10000"), bd("0"), bd("0"), bd("0"),
                bd("1000"), bd("2000"), bd("300"), bd("4.00"),
                bd("5"), bd("3.1"), bd("23"));

        var result = estimator.estimate(request);

        assertThat(result.dutyEstimatePln()).isEqualByComparingTo("2000.00");
        assertThat(result.estimatedTotalPln()).isEqualByComparingTo("56561.46");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
