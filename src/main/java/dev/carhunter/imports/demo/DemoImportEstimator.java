package dev.carhunter.imports.demo;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DemoImportEstimator {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public DemoImportEstimateResponse estimate(DemoImportEstimateRequest input) {
        BigDecimal fx = input.usdToPln();
        BigDecimal vehicle = pln(input.vehiclePriceUsd().multiply(fx));
        BigDecimal inland = pln(input.usInlandTransportUsd().multiply(fx));
        BigDecimal freight = pln(input.freightUsd().multiply(fx));
        BigDecimal insurance = pln(input.insuranceUsd().multiply(fx));

        // Deliberately simplified demo bases. This is not a statutory customs valuation.
        BigDecimal customsValue = vehicle.add(inland).add(freight).add(insurance);
        BigDecimal duty = percent(customsValue, input.assumedDutyRatePercent());
        BigDecimal exciseBase = customsValue.add(duty);
        BigDecimal excise = percent(exciseBase, input.assumedExciseRatePercent());
        BigDecimal vatBase = exciseBase.add(excise);
        BigDecimal vat = percent(vatBase, input.assumedVatRatePercent());

        BigDecimal handling = pln(input.destinationHandlingPln());
        BigDecimal compliance = pln(input.complianceAndModificationsPln());
        BigDecimal registration = pln(input.registrationPln());
        BigDecimal total = customsValue.add(duty).add(excise).add(vat)
                .add(handling).add(compliance).add(registration);

        List<DemoImportEstimateResponse.CostLine> lines = List.of(
                line("VEHICLE", "Vehicle purchase", vehicle, "Purchase price × USD/PLN"),
                line("US_INLAND", "US inland transport", inland, "US inland transport × USD/PLN"),
                line("FREIGHT", "International freight", freight, "Freight × USD/PLN"),
                line("INSURANCE", "Cargo insurance", insurance, "Insurance × USD/PLN"),
                line("DUTY", "Customs duty (assumption)", duty,
                        customsValue + " PLN × " + input.assumedDutyRatePercent() + "%"),
                line("EXCISE", "Excise (assumption)", excise,
                        exciseBase + " PLN × " + input.assumedExciseRatePercent() + "%"),
                line("IMPORT_VAT", "Import VAT (assumption)", vat,
                        vatBase + " PLN × " + input.assumedVatRatePercent() + "%"),
                line("DESTINATION_HANDLING", "Destination handling", handling, "User-provided amount"),
                line("COMPLIANCE", "Compliance and modifications", compliance, "User-provided amount"),
                line("REGISTRATION", "Inspection and registration", registration, "User-provided amount")
        );

        return new DemoImportEstimateResponse(
                "PL", "PLN", "ILLUSTRATIVE_ONLY", fx, pln(customsValue), duty,
                pln(exciseBase), excise, pln(vatBase), vat, pln(total), lines,
                List.of(
                        "Source market is assumed to be the United States and destination is Poland.",
                        "All USD inputs are converted using the single user-provided USD/PLN rate.",
                        "Duty, excise and VAT percentages are user-entered scenario assumptions, not live legal rates.",
                        "Simplified demo customs value = vehicle price + US inland transport + freight + insurance.",
                        "Simplified excise base = demo customs value + demo duty; simplified VAT base = demo customs value + duty + excise.",
                        "Destination handling, compliance and registration are added to the buyer total but excluded from the demo tax bases."
                ),
                List.of(
                        "Not a customs quotation, tax advice or production landed-cost calculation.",
                        "No live TARIC/ISZTAR4 lookup, origin verification, CN/TARIC classification or customs valuation assessment is performed.",
                        "No separate customs, excise and VAT exchange rates or statutory rounding/date rules are applied.",
                        "Actual tax bases may include additional eligible charges; double counting and exclusions must be reviewed for the real transaction.",
                        "Shipping, port, broker, compliance and registration costs must be independently verified."
                )
        );
    }

    private static BigDecimal percent(BigDecimal base, BigDecimal rate) {
        return pln(base.multiply(rate).divide(HUNDRED, 8, RoundingMode.HALF_UP));
    }

    private static BigDecimal pln(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static DemoImportEstimateResponse.CostLine line(
            String code, String label, BigDecimal amount, String calculation) {
        return new DemoImportEstimateResponse.CostLine(code, label, pln(amount), calculation);
    }
}
