package com.civisanalytics.audit;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.civisanalytics.audit.dto.AuditUploadResponse;

@RestController
@RequestMapping("/api/audits")
public class AuditController {

	private final AuditService auditService;
	private final SerpApiService serpApiService;
	private final DoctavianService doctavianService;
	private final NameComService nameComService;

	public AuditController(AuditService auditService, SerpApiService serpApiService, DoctavianService doctavianService,
			NameComService nameComService) {
		this.auditService = auditService;
		this.serpApiService = serpApiService;
		this.doctavianService = doctavianService;
		this.nameComService = nameComService;
	}

	@PostMapping(value = "/upload", consumes = "multipart/form-data", produces = "application/json")
	public ResponseEntity<?> upload(@RequestParam("id_obra") String idObra, @RequestParam("file") MultipartFile file)
			throws IOException {
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body("{\"error\": \"O arquivo está vazio.\"}");
		}

		try (java.io.InputStream is = file.getInputStream()) {
			byte[] header = new byte[5];
			is.read(header);
			String headerString = new String(header);

			if (!headerString.equals("%PDF-")) {
				return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
						"{\"error\": \"Falha de Segurança: O arquivo enviado não é um PDF válido e foi bloqueado.\"}");
			}
		}

		try (PDDocument document = PDDocument.load(file.getInputStream())) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(1);
			stripper.setEndPage(2);
			String text = stripper.getText(document).toLowerCase();

			int score = 0;
			if (text.contains("contratante"))
				score++;
			if (text.contains("contratada"))
				score++;
			if (text.contains("cláusula"))
				score++;
			if (text.contains("licitação"))
				score++;
			if (text.contains("termo de referência"))
				score++;
			if (text.contains("diário oficial"))
				score++;
			if (text.contains("cnpj"))
				score++;

			if (score < 2) {
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
						"{\"error\": \"Conteúdo Inválido: O documento enviado não possui a estrutura jurídica de um contrato ou edital. Por favor, envie o documento correto da obra.\"}");
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"error\": \"Não foi possível processar o texto do PDF para validação.\"}");
		}

		try {
			AuditUploadResponse response = auditService.uploadAndProcess(idObra, file);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
					"{\"error\": \"Obra não encontrada! O ID informado não existe na base de dados oficial. Verifique o número e tente novamente.\"}");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"error\": \"Erro interno ao processar a auditoria: " + e.getMessage() + "\"}");
		}
	}

	@GetMapping("/obra/{idObra}")
	public ResponseEntity<List<ContractAudit>> listByObra(@PathVariable String idObra) {
		return ResponseEntity.ok(auditService.listByObra(idObra));
	}

	@PatchMapping("/{id}/approve")
	public ResponseEntity<ContractAudit> approve(@PathVariable UUID id) {
		return ResponseEntity.ok(auditService.approve(id));
	}

	@GetMapping("/diligence")
	public ResponseEntity<String> runDueDiligence(@RequestParam("companyName") String companyName) {
		String result = serpApiService.searchCompanyReputation(companyName);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/{id}/generate-official-document")
	public ResponseEntity<?> generateOfficialDocument(@PathVariable UUID id, @RequestBody Map<String, String> payload) {

		String aiVerdict = payload.get("aiVerdict");
		String idObra = payload.getOrDefault("idObra", "OBRA-N/A");

		ContractAudit audit = auditService.findById(id);
		String empresaReal = "Empresa Contratada Não Identificada";

		if (audit != null && audit.getNomeResponsavel() != null && !audit.getNomeResponsavel().isBlank()) {
			empresaReal = audit.getNomeResponsavel();
		}

		try {
			String documentUrl = doctavianService.gerarTermoOficial(id.toString(), idObra, aiVerdict, empresaReal);
			return ResponseEntity.ok(Map.of("document_url", documentUrl));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Erro ao gerar documento oficial via Doctavian: " + e.getMessage()));
		}
	}

	@GetMapping("/transparency/domain-search")
	public ResponseEntity<?> searchDomainForTransparency(@RequestParam("cityName") String cityName) {
		try {
			List<Map<String, Object>> availableDomains = nameComService.searchDomains(cityName);
			return ResponseEntity.ok(Map.of("results", availableDomains));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Erro ao buscar domínio na Name.com: " + e.getMessage()));
		}
	}

	@PostMapping("/transparency/domain-register")
	public ResponseEntity<?> registerTransparencyDomain(@RequestBody Map<String, String> payload) {
		try {
			String domainName = payload.get("domainName");
			Map<String, Object> result = nameComService.registerDomain(domainName);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Erro ao registrar domínio: " + e.getMessage()));
		}
	}

	@PostMapping("/copilot/chat")
	public ResponseEntity<?> chatWithCopilot(@RequestBody Map<String, Object> payload) {
		try {
			List<Map<String, String>> messages = (List<Map<String, String>>) payload.get("messages");

			RestTemplate restTemplate = new RestTemplate();
			String url = "https://openrouter.ai/api/v1/chat/completions";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + System.getenv("OPENROUTER_API_KEY"));
			headers.set("HTTP-Referer", "https://civis-analytics.netlify.app");
			headers.set("X-Title", "Civis RJ - Centro de Comando Preditivo");

			Map<String, Object> body = new HashMap<>();
			body.put("model", "meta-llama/llama-3.1-8b-instruct");
			body.put("messages", messages);

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
			ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

			Map<String, Object> responseBody = response.getBody();
			if (responseBody != null && responseBody.containsKey("choices")) {
				List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
				if (!choices.isEmpty()) {
					Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
					String content = (String) messageObj.get("content");

					Map<String, String> result = new HashMap<>();
					result.put("reply", content);
					return ResponseEntity.ok(result);
				}
			}

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Resposta inválida do OpenRouter"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
		}
	}
}