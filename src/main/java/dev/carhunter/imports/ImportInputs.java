package dev.carhunter.imports;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ImportInputs(String purchaseCurrency, BigDecimal purchasePrice,
 BigDecimal usInlandTransport, BigDecimal exportAndHandling, BigDecimal freight, BigDecimal insurance,
 BigDecimal destinationHandling, BigDecimal complianceAndModifications, BigDecimal localRegistration,
 BigDecimal fxRateToCalculationCurrency, String calculationCurrency, LocalDate fxRateDate) {}
