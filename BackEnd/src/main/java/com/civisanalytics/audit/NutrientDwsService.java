package com.civisanalytics.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

@Service
public class NutrientDwsService {

    @Value("${nutrient.api-key}")
    private String apiKey;

    @Value("${nutrient.api-url:https://api.nutrient.io}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public DwsExtractionResult extractData(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("Arquivo não encontrado no servidor para envio à Nutrient.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "Bearer " + apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        
        String instructionsJson = "{\"schema\":{\"type\":\"object\",\"properties\":{\"numero_contrato\":{\"type\":\"string\",\"description\":\"Número do identificador do contrato\"},\"valor_total\":{\"type\":\"number\",\"description\":\"Valor total da obra ou serviço\"}},\"required\":[\"numero_contrato\"]}}";
        body.add("instructions", instructionsJson);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/extraction/extract", 
                    requestEntity, 
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Falha na Extração Nutrient. Status: " + response.getStatusCode());
            }

            Map<String, Object> responseBody = response.getBody();
            
            String requestId = responseBody.get("requestId") != null ? responseBody.get("requestId").toString() : "REQ-" + System.currentTimeMillis();

            String viewerUrl = "EMBEDDED_WEB_SDK";

            Object outputObj = responseBody.get("output");
            String extractedJson = outputObj != null ? outputObj.toString() : "{\"status\": \"Extraído com sucesso\"}";

            return new DwsExtractionResult(requestId, viewerUrl, extractedJson);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com o Extract endpoint da Nutrient DWS: " + e.getMessage());
        }
    }

    public record DwsExtractionResult(String documentId, String viewerUrl, String extractedDataJson) {}
}