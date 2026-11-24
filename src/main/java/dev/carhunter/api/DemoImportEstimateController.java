package dev.carhunter.api;

import dev.carhunter.imports.demo.DemoImportEstimateRequest;
import dev.carhunter.imports.demo.DemoImportEstimateResponse;
import dev.carhunter.imports.demo.DemoImportEstimator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo/import-estimates")
public class DemoImportEstimateController {
    private final DemoImportEstimator estimator;

    public DemoImportEstimateController(DemoImportEstimator estimator) {
        this.estimator = estimator;
    }

    @PostMapping
    public DemoImportEstimateResponse estimate(@Valid @RequestBody DemoImportEstimateRequest request) {
        return estimator.estimate(request);
    }
}
