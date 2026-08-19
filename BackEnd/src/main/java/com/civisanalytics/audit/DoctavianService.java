package com.civisanalytics.audit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DoctavianService {

	@Value("${doctavian.api.key}")
	private String apiKey;

	@Value("${doctavian.api.url}")
	private String apiUrl;

	@Value("${doctavian.template.urn}")
	private String templateUrn;

	public String gerarTermoOficialComEmpresa(String nomeObra, String empresaContratada, String parecerIa) {
		String empresaReal = (empresaContratada != null && !empresaContratada.isBlank())
				? empresaContratada
				: (nomeObra != null ? nomeObra : "Empresa Contratada Padrão");

		return gerarTermoOficialGeral(
			nomeObra != null ? nomeObra : "Obra Pública Municipal", 
			empresaReal, 
			parecerIa
		);
	}

	public String gerarTermoOficial(String nomeObra, String parecerIa) {
		String obraReal = nomeObra != null ? nomeObra : "Obra Pública Municipal";
		String empresaReal = obraReal;

		if (obraReal.contains("-")) {
			String[] partes = obraReal.split("-", 2);
			obraReal = partes[0].trim();
			empresaReal = partes[1].trim();
		}

		return gerarTermoOficialGeral(obraReal, empresaReal, parecerIa);
	}

	private String gerarTermoOficialGeral(String nomeObra, String empresaContratada, String parecerIa) {
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("x-api-key", apiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> requestBody = new HashMap<>();

		Map<String, Object> templateMap = new HashMap<>();
		templateMap.put("urn", templateUrn);
		templateMap.put("loadMethod", "Storage");
		templateMap.put("fileFormat", "docx");
		requestBody.put("template", templateMap);

		String protocolo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		String dataAtual = java.time.LocalDate.now().toString();

		boolean isAprovado = parecerIa.toLowerCase().contains("liberar") || 
		                     parecerIa.toLowerCase().contains("aprovado") || 
		                     parecerIa.toLowerCase().contains("sem incidentes") ||
		                     parecerIa.toLowerCase().contains("autorizo");

		String statusRisco = isAprovado ? "Baixo Risco - Aprovado para Liberação" : "Alto Risco - Retido para Diligência";

		List<Map<String, String>> variables = new ArrayList<>();
		variables.add(Map.of("name", "id_protocolo", "value", protocolo, "type", "global"));
		variables.add(Map.of("name", "data_auditoria", "value", dataAtual, "type", "global"));
		variables.add(Map.of("name", "nome_obra", "value", nomeObra, "type", "global"));
		variables.add(Map.of("name", "empresa_contratada", "value", empresaContratada, "type", "global")); // Dinâmico!
		variables.add(Map.of("name", "nivel_risco", "value", statusRisco, "type", "global"));
		variables.add(Map.of("name", "parecer_ia", "value", parecerIa, "type", "global"));

		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("variables", variables);
		requestBody.put("data", dataMap);

		Map<String, Object> documentMap = new HashMap<>();
		documentMap.put("name", "Termo_Auditoria_" + protocolo);
		documentMap.put("fileFormat", "pdf");
		documentMap.put("deliveryMethod", "Storage");
		documentMap.put("timezone", "America/Sao_Paulo");
		documentMap.put("locale", "pt_BR");
		requestBody.put("document", documentMap);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

		try {
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				apiUrl, 
				HttpMethod.POST, 
				entity, 
				new ParameterizedTypeReference<Map<String, Object>>() {}
			);

			if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
				Map<String, Object> body = response.getBody();
				
				@SuppressWarnings("unchecked")
				Map<String, Object> result = (Map<String, Object>) body.get("result");
				
				if (result != null) {
					@SuppressWarnings("unchecked")
					Map<String, Object> data = (Map<String, Object>) result.get("data");
					
					if (data != null) {
						@SuppressWarnings("unchecked")
						Map<String, Object> document = (Map<String, Object>) data.get("document");
						
						if (document != null && document.containsKey("urn")) {
							return document.get("urn").toString();
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("⚠️ [Doctavian API] Falha externa. Acionando motor inteligente Apache POI...");
		}

		try {
			File templateFile = new File("template_base.docx");
			XWPFDocument doc;

			if (templateFile.exists()) {
				doc = new XWPFDocument(new FileInputStream(templateFile));
			} else {
				doc = new XWPFDocument();
				XWPFParagraph p = doc.createParagraph();
				XWPFRun r = p.createRun();
				r.setText("TERMO OFICIAL DE DILIGÊNCIA - PROTOCOLO: " + protocolo);
			}

			for (XWPFParagraph p : doc.getParagraphs()) {
				substituirTextoParagrafo(p, protocolo, dataAtual, nomeObra, empresaContratada, statusRisco, parecerIa, isAprovado);
			}

			for (XWPFTable table : doc.getTables()) {
				for (XWPFTableRow row : table.getRows()) {
					for (XWPFTableCell cell : row.getTableCells()) {
						for (XWPFParagraph p : cell.getParagraphs()) {
							substituirTextoParagrafo(p, protocolo, dataAtual, nomeObra, empresaContratada, statusRisco, parecerIa, isAprovado);
						}
					}
				}
			}

			File outDir = new File("public");
			if (!outDir.exists()) outDir.mkdir();

			File outFile = new File("public/termo_oficial_civis.docx");
			FileOutputStream fos = new FileOutputStream(outFile);
			doc.write(fos);
			doc.close();
			fos.close();

			return "http://localhost:8080/files/termo_oficial_civis.docx";

		} catch (Exception ex) {
			System.err.println("Erro crítico no gerador local: " + ex.getMessage());
			return "http://localhost:8080/files/termo_oficial_civis.docx";
		}
	}

	private void substituirTextoParagrafo(XWPFParagraph p, String protocolo, String data, String obra, String contratada, String risco, String parecer, boolean aprovado) {
		String fullText = p.getText();
		if (fullText == null || fullText.isEmpty()) {
			return;
		}

		boolean modified = false;

		if (fullText.contains("{{")) {
			fullText = fullText.replace("{{id_protocolo}}", protocolo)
					   .replace("{{data_auditoria}}", data)
					   .replace("{{nome_obra}}", obra)
					   .replace("{{empresa_contratada}}", contratada)
					   .replace("{{nivel_risco}}", risco)
					   .replace("{{parecer_ia}}", parecer);
			modified = true;
		}

		if ((fullText.contains("APROVADO") && aprovado) || (fullText.contains("RETIDO") && !aprovado)) {
			if (fullText.contains("[ ]") || fullText.contains("☐")) {
				fullText = fullText.replace("[ ]", "[X]").replace("☐", "[X]");
				modified = true;
			}
		}

		if (modified) {
			int count = p.getRuns().size();
			for (int i = count - 1; i >= 0; i--) {
				p.removeRun(i);
			}
			XWPFRun newRun = p.createRun();
			newRun.setText(fullText);
		}
	}
}