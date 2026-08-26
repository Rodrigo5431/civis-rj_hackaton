package com.civisanalytics.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NameComService {

    @Value("${namecom.username:USUARIO_TESTE}")
    private String username;

    @Value("${namecom.token:TOKEN_TESTE}")
    private String token;

    @Value("${namecom.api-url:https://api.dev.name.com/v4}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> registerDomain(String domainName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> domainObj = new HashMap<>();
        domainObj.put("domainName", domainName);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("domain", domainObj);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.name.com/v4/domains",
                    requestEntity,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar o domínio na Name.com: " + e.getMessage());
        }
    }
}