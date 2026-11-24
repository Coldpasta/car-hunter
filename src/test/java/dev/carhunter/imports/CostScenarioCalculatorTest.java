package dev.carhunter.imports;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CostScenarioCalculatorTest {
    private final CostScenarioCalculator calculator = new CostScenarioCalculator();

    @Test
    void aggregatesLowExpectedAndHighAmounts() {
        var result = calculator.aggregate("PLN", List.of(
                component("purchase", "PLN", "100", "110", "120"),
                component("freight", "PLN", "10", "15", "20")),
                Set.of("purchase", "freight"));

        assertTrue(result.isComplete());
        assertEquals(new BigDecimal("110"), result.total().low());
        assertEquals(new BigDecimal("125"), result.total().expected());
        assertEquals(new BigDecimal("140"), result.total().high());
    }

    @Test
    void missingRequiredComponentProducesNoTotal() {
        var result = calculator.aggregate("PLN",
                List.of(component("purchase", "PLN", "100", "100", "100")),
                Set.of("purchase", "customs-duty"));

        assertFalse(result.isComplete());
        assertNull(result.total());
        assertEquals(List.of("customs-duty"), result.missingComponents());
    }

    @Test
    void currencyMismatchProducesNoTotal() {
        var result = calculator.aggregate("PLN",
                List.of(component("purchase", "USD", "100", "100", "100")),
                Set.of("purchase"));

        assertFalse(result.isComplete());
        assertNull(result.total());
        assertTrue(result.errors().getFirst().contains("Currency mismatch"));
    }

    @Test
    void duplicateComponentCodeProducesNoTotal() {
        var result = calculator.aggregate("PLN", List.of(
                component("freight", "PLN", "1", "1", "1"),
                component("freight", "PLN", "2", "2", "2")),
                Set.of("freight"));

        assertFalse(result.isComplete());
        assertNull(result.total());
        assertTrue(result.errors().getFirst().contains("Duplicate component code"));
    }

    @Test
    void rejectsInvalidCostRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new CostRange(new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("30")));
        assertThrows(IllegalArgumentException.class,
                () -> new CostRange(new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ONE));
    }

    private CostScenarioComponent component(String code, String currency,
                                            String low, String expected, String high) {
        return new CostScenarioComponent(code, code, currency,
                new CostRange(new BigDecimal(low), new BigDecimal(expected), new BigDecimal(high)),
                true, false);
    }
}
