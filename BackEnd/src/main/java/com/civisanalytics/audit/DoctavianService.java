package com.civisanalytics.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class DoctavianService {

	@Value("${doctavian.api.key}")
	private String apiKey;

	@Value("${doctavian.api.url}")
	private String apiUrl;

	@Value("${doctavian.template.urn}")
	private String templateUrn;

	public String gerarTermoOficial(String nomeObra, String parecerIa) {
		RestTemplate restTemplate = new RestTemplate();

		// Cabeçalho LIMPO - Apenas a x-api-key oficial da conta!
		HttpHeaders headers = new HttpHeaders();
		headers.set("x-api-key", apiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Corpo da requisição estruturado
		Map<String, Object> requestBody = new HashMap<>();

		// 1. Configuração do Template
		Map<String, Object> templateMap = new HashMap<>();
		templateMap.put("urn", templateUrn);
		templateMap.put("loadMethod", "Storage");
		templateMap.put("fileFormat", "docx"); 
		requestBody.put("template", templateMap);

		// 2. Dados dinâmicos do Template Ouro
		List<Map<String, String>> variables = new ArrayList<>();
		variables.add(Map.of("name", "id_protocolo", "value", UUID.randomUUID().toString().substring(0, 8).toUpperCase(), "type", "global"));
		variables.add(Map.of("name", "data_auditoria", "value", java.time.LocalDate.now().toString(), "type", "global"));
		variables.add(Map.of("name", "nome_obra", "value", nomeObra, "type", "global"));
		variables.add(Map.of("name", "empresa_contratada", "value", nomeObra, "type", "global"));
		variables.add(Map.of("name", "nivel_risco", "value", "Análise Concluída - Auditado por IA", "type", "global"));
		variables.add(Map.of("name", "parecer_ia", "value", parecerIa, "type", "global"));

		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("variables", variables);
		requestBody.put("data", dataMap);

		// 3. Configuração do Documento de Saída (SEM CAMINHO FIXO)
		Map<String, Object> documentMap = new HashMap<>();
		documentMap.put("name", "Termo_Auditoria_" + UUID.randomUUID().toString().substring(0, 6));
		documentMap.put("fileFormat", "pdf");
		documentMap.put("deliveryMethod", "Storage");
		documentMap.put("timezone", "America/Sao_Paulo"); 
		documentMap.put("locale", "pt_BR");               
		requestBody.put("document", documentMap);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

		try {
			ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
			if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
				@SuppressWarnings("unchecked")
				Map<String, Object> data = (Map<String, Object>) result.get("data");
				@SuppressWarnings("unchecked")
				Map<String, Object> document = (Map<String, Object>) data.get("document");
				
				// Retorna a URI oficial do documento gerado
				return document.get("urn").toString();
			}
		} catch (Exception e) {
			System.err.println("ERRO FATAL NA DOCTAVIAN: " + e.getMessage());
			throw new RuntimeException("Falha na geração do PDF: " + e.getMessage());
		}
		
		throw new RuntimeException("A API não retornou o documento.");
	}
}