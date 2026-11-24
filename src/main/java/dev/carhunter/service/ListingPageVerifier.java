package dev.carhunter.service;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fetches the public listing HTML without browser automation or access-control bypass.
 * A page that blocks automated access is treated as unverified, never as clean.
 */
@Component
public class ListingPageVerifier {
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "cars.com", "www.cars.com", "autotrader.com", "www.autotrader.com",
            "cargurus.com", "www.cargurus.com");
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final Pattern NON_VISIBLE = Pattern.compile(
            "(?is)<(script|style|noscript|svg)[^>]*>.*?</\\1>|<!--.*?-->");
    private static final Pattern TAGS = Pattern.compile("(?s)<[^>]+>");
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public Verification verify(String listingUrl, String expectedQuote) {
        try {
            URI uri = URI.create(listingUrl);
            if (!isApprovedUri(uri)) {
                return Verification.failed("Listing URL is outside the approved HTTPS marketplace hosts.");
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "CarHunter/0.2 listing evidence verifier")
                    .header("Accept", "text/html")
                    .GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return Verification.failed("Listing page could not be fetched (HTTP " + response.statusCode() + ").");
                }
                String contentType = response.headers().firstValue("content-type").orElse("");
                if (!contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
                    return Verification.failed("Marketplace response was not an HTML listing page.");
                }
                byte[] bytes = readBounded(body, MAX_RESPONSE_BYTES);
                if (bytes == null) {
                    return Verification.failed("Marketplace HTML exceeded the 2 MiB verification limit.");
                }
                String html = new String(bytes, StandardCharsets.UTF_8);
                String visibleText = NON_VISIBLE.matcher(html).replaceAll(" ");
                visibleText = TAGS.matcher(visibleText).replaceAll(" ");
                visibleText = decodeBasicEntities(visibleText);
                String normalizedPage = normalize(visibleText);
                String normalizedQuote = normalize(expectedQuote);
                if (normalizedQuote.isBlank() || !normalizedPage.contains(normalizedQuote)) {
                    return Verification.failed("The seller's Clean/Clear Title quote was not found in the fetched listing page.");
                }
                return new Verification(true, "Exact title-evidence quote found in fetched listing HTML.", Instant.now());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Verification.failed("Listing verification was interrupted.");
        } catch (Exception ex) {
            return Verification.failed("Listing page could not be independently verified: " + ex.getClass().getSimpleName() + ".");
        }
    }

    private boolean isApprovedUri(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null
                && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && ALLOWED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    private byte[] readBounded(InputStream input, int maxBytes) throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) return null;
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }

    private String decodeBasicEntities(String value) {
        return value.replace("&nbsp;", " ").replace("&#160;", " ")
                .replace("&amp;", "&").replace("&quot;", "\\"")
                .replace("&#39;", "'").replace("&apos;", "'");
    }

    public record Verification(boolean verified, String reason, Instant retrievedAt) {
        static Verification failed(String reason) {
            return new Verification(false, reason, Instant.now());
        }
    }
}
