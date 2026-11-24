package dev.carhunter.service;

import dev.carhunter.domain.CarCandidate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Removes duplicate listing URLs and duplicate VINs while preserving first-seen order. */
@Component
public class ListingDeduplicator {
    public List<CarCandidate> deduplicate(List<CarCandidate> candidates) {
        Map<String, CarCandidate> unique = new LinkedHashMap<>();
        for (CarCandidate candidate : candidates) {
            String key = validVin(candidate.vin())
                    ? "vin:" + candidate.vin().toUpperCase(Locale.ROOT)
                    : "url:" + canonicalUrl(candidate.sourceUrl());
            unique.putIfAbsent(key, candidate);
        }
        return List.copyOf(unique.values());
    }

    String canonicalUrl(String value) {
        if (value == null) return "";
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            return host + path;
        } catch (IllegalArgumentException ex) {
            return value.trim().toLowerCase(Locale.ROOT);
        }
    }

    private boolean validVin(String vin) {
        return vin != null && vin.toUpperCase(Locale.ROOT).matches("[A-HJ-NPR-Z0-9]{17}");
    }
}
