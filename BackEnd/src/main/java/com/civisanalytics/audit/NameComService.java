package com.civisanalytics.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NameComService {

    @Value("${namecom.username}")
    private String username;

    @Value("${namecom.token}")
    private String token;

    @Value("${namecom.api-url:https://api.dev.name.com/core/v1}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
            
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getBasicAuthHeader() {
        String auth = username + ":" + token;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    public List<Map<String, Object>> searchDomains(String cityName) {
        String formattedCity = cityName.toLowerCase().replaceAll("[^a-z0-9]", "-");
        
        List<String> domainsToCheck = List.of(
            "transparencia-" + formattedCity + ".com",
            "transparencia-" + formattedCity + ".org",
            "transparencia-" + formattedCity + ".net"
        );

        try {
            String baseUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
            URI uri = new URI(baseUrl + "/domains:checkAvailability");

            Map<String, Object> bodyMap = Map.of("domainNames", domainsToCheck);
            String jsonBody = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", getBasicAuthHeader())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                
                if (responseMap.containsKey("results")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
                    
                    return results.stream()
                        .map(r -> Map.<String, Object>of(
                            "domainName", r.get("domainName"),
                            "purchasePrice", r.getOrDefault("purchasePrice", 12.99)
                        ))
                        .collect(Collectors.toList());
                }
            } else {
                System.err.println("Name.com API Error (Search): " + response.statusCode() + " - " + response.body());
                throw new RuntimeException("Falha na API da Name.com. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Erro de comunicação real com Name.com: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Collections.emptyList();
    }

    public Map<String, Object> registerDomain(String domainName) {
        try {
            String baseUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
            URI uri = new URI(baseUrl + "/domains");

            Map<String, Object> bodyMap = Map.of(
                "domain", Map.of("domainName", domainName),
                "purchasePrice", 12.99
            );
            String jsonBody = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", getBasicAuthHeader())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            } else {
                System.err.println("Erro Name.com (Register): " + response.statusCode() + " - " + response.body());
                throw new RuntimeException("Erro da API: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar o domínio na Name.com: " + e.getMessage());
        }
    }
}