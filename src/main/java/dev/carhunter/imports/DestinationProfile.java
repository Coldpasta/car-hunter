package dev.carhunter.imports;
import dev.carhunter.domain.ImportCostEstimate;
import dev.carhunter.domain.VehicleListing;
import java.util.Set;
/** Versioned country-specific import rules are isolated behind this contract. */
public interface DestinationProfile {
 String countryCode();
 String rulesetVersion();
 Set<String> supportedComponents();
 ImportCostEstimate estimate(VehicleListing vehicle, ImportInputs inputs);
}
