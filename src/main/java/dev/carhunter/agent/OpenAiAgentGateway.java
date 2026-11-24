package dev.carhunter.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class OpenAiAgentGateway implements AgentGateway {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String model;
    private final int maxOutputTokens;

    public OpenAiAgentGateway(RestClient.Builder builder, ObjectMapper mapper,
            @Value("${carhunter.openai.base-url}") String baseUrl,
            @Value("${carhunter.openai.api-key:}") String apiKey,
            @Value("${carhunter.openai.model}") String model,
            @Value("${carhunter.openai.max-output-tokens:2500}") int maxOutputTokens) {
        RestClient.Builder configured = builder.baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) configured.defaultHeader("Authorization", "Bearer " + apiKey);
        this.client = configured.build();
        this.mapper = mapper;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
    }

    @Override
    public String run(String role, String instructions, String context) {
        String input = "ROLE: " + role + "\n\nINSTRUCTIONS:\n" + instructions
                + "\n\nINVESTIGATION CONTEXT:\n" + context;
        // Web retrieval is performed by TavilySearchGateway; the Fly.io OpenAI Connector
        // does not support forwarding the hosted web_search tool.
        Map<String, Object> body = Map.of("model", model, "input", input,
                "max_output_tokens", maxOutputTokens, "store", false);
        try {
            String response = client.post().uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(String.class);
            return extractText(response);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("OpenAI request failed (" + ex.getStatusCode()
                    + "): " + safeMessage(ex.getResponseBodyAsString()), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not complete " + role + " agent request: " + ex.getMessage(), ex);
        }
    }

    private String extractText(String response) throws Exception {
        JsonNode root = mapper.readTree(response);
        JsonNode direct = root.path("output_text");
        if (direct.isTextual() && !direct.asText().isBlank()) return direct.asText();
        StringBuilder text = new StringBuilder();
        for (JsonNode item : root.path("output")) {
            for (JsonNode content : item.path("content")) {
                if (content.path("type").asText().equals("output_text")) {
                    if (!text.isEmpty()) text.append("\n");
                    text.append(content.path("text").asText());
                }
            }
        }
        if (text.isEmpty()) throw new IllegalStateException("OpenAI returned no text output.");
        return text.toString();
    }

    private String safeMessage(String body) {
        if (body == null || body.isBlank()) return "No error details returned";
        return body.length() > 500 ? body.substring(0, 500) : body;
    }
}
