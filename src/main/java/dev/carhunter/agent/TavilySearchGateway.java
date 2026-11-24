package dev.carhunter.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/** Calls the Fly.io Tavily Connector; its gateway injects the Tavily credential. */
@Component
public class TavilySearchGateway {
    private final RestClient client;
    private final String baseUrl;

    public TavilySearchGateway(RestClient.Builder builder,
            @Value("${carhunter.tavily.base-url:}") String baseUrl) {
        this.client = builder.build();
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    }

    public String search(String query, int maxResults) {
        if (baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "Tavily is not configured. Set TAVILY_BASE_URL to the Fly.io Tavily Connector gateway URL.");
        }
        Map<String, Object> body = Map.of(
                "query", query,
                "search_depth", "basic",
                "max_results", Math.max(1, Math.min(maxResults, 10)),
                "include_answer", false
        );
        try {
            String response = client.post().uri(baseUrl + "/search")
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(String.class);
            if (response == null || response.isBlank()) {
                throw new IllegalStateException("Tavily returned an empty search response.");
            }
            return response;
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            if (detail == null || detail.isBlank()) detail = "No error details returned";
            if (detail.length() > 400) detail = detail.substring(0, 400);
            throw new IllegalStateException("Tavily search failed (" + ex.getStatusCode() + "): " + detail, ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not complete Tavily search: " + ex.getMessage(), ex);
        }
    }
}
