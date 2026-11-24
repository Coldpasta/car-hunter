package dev.carhunter.api;

import dev.carhunter.imports.demo.DemoImportEstimator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoImportEstimateController.class)
@Import(DemoImportEstimator.class)
class DemoImportEstimateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void calculatesEstimateThroughHttpAndReturnsItemizedBreakdown() throws Exception {
        mockMvc.perform(post("/api/demo/import-estimates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationCountry").value("PL"))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.status").value("ILLUSTRATIVE_ONLY"))
                .andExpect(jsonPath("$.customsValueEstimatePln").value(92800.00))
                .andExpect(jsonPath("$.exciseEstimatePln").value(2876.80))
                .andExpect(jsonPath("$.vatEstimatePln").value(22005.66))
                .andExpect(jsonPath("$.estimatedTotalPln").value(122682.46))
                .andExpect(jsonPath("$.breakdown.length()").value(10))
                .andExpect(jsonPath("$.assumptions.length()").isNotEmpty())
                .andExpect(jsonPath("$.limitations.length()").isNotEmpty());
    }

    @Test
    void rejectsMissingRequiredInput() throws Exception {
        String request = validRequest().replace("\"insuranceUsd\": 200,", "");

        mockMvc.perform(post("/api/demo/import-estimates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNegativeVehiclePrice() throws Exception {
        String request = validRequest().replace("\"vehiclePriceUsd\": 20000", "\"vehiclePriceUsd\": -1");

        mockMvc.perform(post("/api/demo/import-estimates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTaxRateAboveOneHundredPercent() throws Exception {
        String request = validRequest().replace("\"assumedVatRatePercent\": 23", "\"assumedVatRatePercent\": 101");

        mockMvc.perform(post("/api/demo/import-estimates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    private static String validRequest() {
        return """
                {
                  "vehiclePriceUsd": 20000,
                  "usInlandTransportUsd": 1000,
                  "freightUsd": 2000,
                  "insuranceUsd": 200,
                  "destinationHandlingPln": 1500,
                  "complianceAndModificationsPln": 3000,
                  "registrationPln": 500,
                  "usdToPln": 4.00,
                  "assumedDutyRatePercent": 0,
                  "assumedExciseRatePercent": 3.1,
                  "assumedVatRatePercent": 23
                }
                """;
    }
}
