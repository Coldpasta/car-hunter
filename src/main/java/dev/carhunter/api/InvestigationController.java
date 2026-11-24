package dev.carhunter.api;

import dev.carhunter.domain.InvestigationReport;
import dev.carhunter.domain.SearchRequest;
import dev.carhunter.service.CarHunterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class InvestigationController {
    private final CarHunterService service;
    public InvestigationController(CarHunterService service) { this.service = service; }

    @PostMapping("/investigations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvestigationReport investigate(@Valid @RequestBody SearchRequest request) {
        return service.investigate(request);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP", "application", "CAR HUNTER"); }
}
