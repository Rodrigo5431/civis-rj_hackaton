package com.civisanalytics.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DoctavianService {

	@Value("${doctavian.api.key}")
	private String apiKey;

	@Value("${doctavian.api.url}")
	private String apiUrl;

	@Value("${doctavian.template.urn}")
	private String templateUrn;

	@Value("${doctavian.api.token}")
	private String apiToken;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

public String gerarTermoOficial(String auditId, String idObra, String aiVerdict, String empresaContratada) {
        
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.set("x-api-key", apiKey.trim());
        String cleanToken = apiToken.startsWith("Bearer ") ? apiToken : "Bearer " + apiToken;
        authHeaders.set("Authorization", cleanToken.trim());

        Map<String, Object> dataValues = new HashMap<>();
        dataValues.put("id_obra", idObra);
        dataValues.put("audit_id", auditId);
        dataValues.put("empresa_contratada", empresaContratada);
        dataValues.put("parecer_tecnico", aiVerdict);
        dataValues.put("data_emissao", java.time.LocalDate.now().toString());

        String dataUrn = fazerUploadDeDados(dataValues, authHeaders);
        System.out.println("DEBUG DOCTAVIAN -> Upload de dados OK. URN recebido: " + dataUrn);

        String generateUrl = apiUrl + "/v1/documents/document/generate";

        HttpHeaders generateHeaders = new HttpHeaders();
        generateHeaders.putAll(authHeaders);
        generateHeaders.setContentType(MediaType.APPLICATION_JSON);
        generateHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> externalContext = new HashMap<>();
        externalContext.put("id", "audit-" + auditId);

        Map<String, Object> templateConfig = new HashMap<>();
        templateConfig.put("name", "template.docx"); 
        templateConfig.put("urn", templateUrn);
        templateConfig.put("fileFormat", "docx");
        templateConfig.put("loadMethod", "Storage");
        templateConfig.put("options", new HashMap<>());

        Map<String, Object> dataConfig = new HashMap<>();
        dataConfig.put("loadMethod", "Storage");
        dataConfig.put("urn", dataUrn);

        Map<String, Object> documentConfig = new HashMap<>();
        documentConfig.put("name", "Termo-Notificacao-Obra-" + idObra);
        documentConfig.put("fileFormat", "pdf");
        documentConfig.put("deliveryMethod", "Storage");
        documentConfig.put("path", "root");
        documentConfig.put("locale", "pt-BR");
        documentConfig.put("timezone", "America/Sao_Paulo");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("externalContext", externalContext);
        requestBody.put("template", templateConfig);
        requestBody.put("data", dataConfig);
        requestBody.put("document", documentConfig);

        try {
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
            System.out.println("\n--- DEBUG DOCTAVIAN [PAYLOAD DE GERAÇÃO] ---");
            System.out.println(jsonPayload);
            System.out.println("----------------------------------------------\n");
        } catch (Exception e) {
            System.out.println("Não foi possível logar o payload: " + e.getMessage());
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, generateHeaders);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    generateUrl, 
                    HttpMethod.POST, 
                    requestEntity, 
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object documentUrlObj = body.get("document_url");
                return documentUrlObj != null ? documentUrlObj.toString() : "https://demo.portal.doctavian.com";
            } else {
                throw new RuntimeException("Erro na resposta de geração. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar o documento final: " + e.getMessage());
        }
    }

	private String fazerUploadDeDados(Map<String, Object> dataValues, HttpHeaders authHeaders) {
		String uploadUrl = apiUrl + "/v1/documents/data/upload";

		try {
			String jsonString = objectMapper.writeValueAsString(dataValues);

			org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(
					jsonString.getBytes()) {
				@Override
				public String getFilename() {
					return "civis-data.json";
				}
			};

			HttpHeaders uploadHeaders = new HttpHeaders();
			uploadHeaders.putAll(authHeaders);
			uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

			org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
			body.add("file", resource);

			HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body,
					uploadHeaders);

			ResponseEntity<Map> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, Map.class);

			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Map<String, Object> responseBody = response.getBody();

				try {
					Map<String, Object> resultObj = (Map<String, Object>) responseBody.get("result");
					Map<String, Object> dataObj = (Map<String, Object>) resultObj.get("data");
					java.util.List<Map<String, Object>> filesList = (java.util.List<Map<String, Object>>) dataObj
							.get("files");

					if (filesList != null && !filesList.isEmpty()) {
						return filesList.get(0).get("id").toString();
					}
				} catch (Exception parseEx) {
					throw new RuntimeException(
							"O formato da resposta mudou. Não foi possível extrair o ID: " + responseBody);
				}

				throw new RuntimeException("Nenhum ID encontrado na resposta de upload.");
			} else {
				throw new RuntimeException("Status de erro no upload de dados: " + response.getStatusCode());
			}
		} catch (Exception e) {
			throw new RuntimeException("Falha ao fazer upload do JSON de dados: " + e.getMessage());
		}
	}
}