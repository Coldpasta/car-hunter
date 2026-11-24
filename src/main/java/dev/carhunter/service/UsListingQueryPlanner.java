package dev.carhunter.service;

import dev.carhunter.domain.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds bounded, marketplace-specific searches for US retail listings. */
@Component
public class UsListingQueryPlanner {
    private static final List<String> MARKETPLACES = List.of(
            "site:cars.com",
            "site:autotrader.com",
            "site:cargurus.com"
    );

    public List<String> plan(SearchRequest request) {
        String vehicle = join(request.make(), request.model());
        if (vehicle.isBlank()) vehicle = "used car";
        List<String> constraints = new ArrayList<>();
        constraints.add(vehicle);
        constraints.add("used for sale");
        constraints.add("clean title");
        constraints.add("United States");
        if (request.minYear() != null) constraints.add("year " + request.minYear() + " or newer");
        if (request.maxYear() != null) constraints.add("year " + request.maxYear() + " or older");
        if (request.maxMileageMiles() != null) constraints.add("under " + request.maxMileageMiles() + " miles");
        if (request.usState() != null && !request.usState().isBlank()) constraints.add(request.usState());
        if (request.usZipCode() != null && !request.usZipCode().isBlank()) constraints.add(request.usZipCode());
        if (request.fuel() != null && !request.fuel().isEmpty()) constraints.add(String.join(" ", request.fuel()));
        if (request.transmission() != null && !request.transmission().isBlank()) constraints.add(request.transmission());
        constraints.add("under $" + request.budgetAmount().toPlainString());
        return MARKETPLACES.stream()
                .map(domain -> domain + " " + String.join(" ", constraints))
                .toList();
    }

    private String join(String make, String model) {
        return ((make == null ? "" : make.trim()) + " " + (model == null ? "" : model.trim()))
                .trim().toLowerCase(Locale.ROOT);
    }
}
