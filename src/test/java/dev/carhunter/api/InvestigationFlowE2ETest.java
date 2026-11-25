package dev.carhunter.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.carhunter.service.ListingPageVerifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvestigationFlowE2ETest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> TAVILY_REQUESTS = new CopyOnWriteArrayList<>();
    private static final List<String> OPENAI_ROLES = new CopyOnWriteArrayList<>();
    private static final List<String> OPENAI_PATHS = new CopyOnWriteArrayList<>();
    private static final List<String> OPENAI_AUTH_HEADERS = new CopyOnWriteArrayList<>();

    private static final HttpServer TAVILY_SERVER = startServer("/tavily/search",
            InvestigationFlowE2ETest::handleTavily);
    private static final HttpServer OPENAI_SERVER = startServer("/v1/responses",
            InvestigationFlowE2ETest::handleOpenAi);

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private ListingPageVerifier pageVerifier;

    @DynamicPropertySource
    static void connectorProperties(DynamicPropertyRegistry registry) {
        registry.add("carhunter.tavily.base-url",
                () -> "http://127.0.0.1:" + TAVILY_SERVER.getAddress().getPort() + "/tavily");
        registry.add("carhunter.openai.base-url",
                () -> "http://127.0.0.1:" + OPENAI_SERVER.getAddress().getPort());
        registry.add("carhunter.openai.api-key", () -> "integration-test-key");
        registry.add("carhunter.openai.model", () -> "integration-test-model");
    }

    @AfterAll
    static void stopStubServers() {
        TAVILY_SERVER.stop(0);
        OPENAI_SERVER.stop(0);
    }

    @Test
    void postsInvestigationAndRunsTheFullAgentFlowThroughBothHttpGateways() {
        when(pageVerifier.verify(anyString(), anyString())).thenReturn(
                new ListingPageVerifier.Verification(true, "test fixture: title quote found", Instant.now()));

        Map<String, Object> request = Map.ofEntries(
                Map.entry("sourceMarket", "US"),
                Map.entry("destinationCountry", "PL"),
                Map.entry("budgetAmount", 25000),
                Map.entry("budgetCurrency", "USD"),
                Map.entry("make", "Toyota"),
                Map.entry("model", "RAV4"),
                Map.entry("bodyType", "SUV"),
                Map.entry("fuel", List.of("hybrid")),
                Map.entry("transmission", "automatic"),
                Map.entry("minYear", 2021),
                Map.entry("maxYear", 2025),
                Map.entry("maxMileageMiles", 90000),
                Map.entry("sellerTypes", List.of("dealer")),
                Map.entry("usState", "CA"),
                Map.entry("candidateLimit", 5)
        );

        var response = rest.postForEntity("/api/investigations", request, String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Toyota RAV4"));
        assertTrue(response.getBody().contains("Inspect the vehicle before purchase"));

        assertEquals(4, TAVILY_REQUESTS.size(),
                "Expected three marketplace discovery calls and one candidate research call");
        assertEquals(List.of("SCOUT", "INVESTIGATOR", "SKEPTIC", "DECISION"), OPENAI_ROLES);
        assertTrue(OPENAI_PATHS.stream().allMatch("/v1/responses"::equals));
        assertTrue(OPENAI_AUTH_HEADERS.stream().allMatch("Bearer integration-test-key"::equals));
        assertTrue(TAVILY_REQUESTS.stream().allMatch(body -> body.contains("\"query\"")));
    }

    private static HttpServer startServer(String path, com.sun.net.httpserver.HttpHandler handler) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(path, handler);
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void handleTavily(HttpExchange exchange) throws IOException {
        try (exchange) {
            TAVILY_REQUESTS.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"results":[
                      {"url":"https://www.cars.com/vehicledetail/123",
                       "title":"2022 Toyota RAV4 Hybrid for sale",
                       "content":"Dealer listing. Clean title. 2022 Toyota RAV4 Hybrid, 50000 miles, $24000."}
                    ]}
                    """);
        }
    }

    private static void handleOpenAi(HttpExchange exchange) throws IOException {
        try (exchange) {
            OPENAI_PATHS.add(exchange.getRequestURI().getPath());
            OPENAI_AUTH_HEADERS.add(exchange.getRequestHeaders().getFirst("Authorization"));
            JsonNode request = MAPPER.readTree(exchange.getRequestBody());
            String input = request.path("input").asText();
            String role = input.lines().filter(line -> line.startsWith("ROLE: "))
                    .map(line -> line.substring("ROLE: ".length())).findFirst().orElse("UNKNOWN");
            OPENAI_ROLES.add(role);

            String output = switch (role) {
                case "SCOUT" -> scoutOutput();
                case "INVESTIGATOR" -> "Model-level reliability research. Vehicle-specific title history remains unverified.";
                case "SKEPTIC" -> "SUFFICIENT\\nThe listing quote is not independent title-history verification.";
                case "DECISION" -> "Ranked shortlist: Toyota RAV4. Inspect the vehicle before purchase.";
                default -> throw new IllegalStateException("Unexpected agent role: " + role);
            };
            sendJson(exchange, 200, MAPPER.writeValueAsString(Map.of("output_text", output)));
        } catch (Exception ex) {
            sendJson(exchange, 500, MAPPER.writeValueAsString(Map.of("error", ex.getMessage())));
        }
    }

    private static String scoutOutput() {
        return """
                {"candidates":[{
                  "id":"car-1",
                  "makeModel":"Toyota RAV4",
                  "title":"2022 Toyota RAV4 Hybrid",
                  "sourceUrl":"https://www.cars.com/vehicledetail/123",
                  "country":"US",
                  "state":"CA",
                  "price":24000,
                  "currency":"USD",
                  "year":2022,
                  "mileageMiles":50000,
                  "fuel":"hybrid",
                  "transmission":"automatic",
                  "sellerClaims":"Dealer listing",
                  "titleStatus":"CLEAN",
                  "titleEvidenceQuote":"Clean title",
                  "titleEvidenceUrl":"https://www.cars.com/vehicledetail/123",
                  "sellerType":"dealer",
                  "vin":"1HGCM82633A004352"
                }]}
                """;
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
