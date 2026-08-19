package com.civisanalytics.audit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.civisanalytics.audit.dto.AuditUploadResponse;

@Service
public class AuditService {

	private final ContractAuditRepository repository;
	private final NutrientDwsService dwsService;
	private final String uploadDir;
	private final JdbcTemplate jdbcTemplate;

	public AuditService(ContractAuditRepository repository, NutrientDwsService dwsService,
			@Value("${app.upload-dir}") String uploadDir, JdbcTemplate jdbcTemplate) {
		this.repository = repository;
		this.dwsService = dwsService;
		this.uploadDir = uploadDir;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public AuditUploadResponse uploadAndProcess(String idObra, MultipartFile file) throws IOException {
		String storedPath = storeFile(file);

		ContractAudit audit = new ContractAudit();
		audit.setIdObra(idObra);
		audit.setFileName(file.getOriginalFilename());
		audit.setFilePath(storedPath);
		audit.setFileSizeBytes(file.getSize());
		audit.setStatus(AuditStatus.PENDING_EXTRACTION);

		String nomeEmpresa = "Empresa Contratada Não Identificada";
		try {
			String sql = "SELECT nome_responsavel FROM obras WHERE id_obra = ?";
			List<String> resultados = jdbcTemplate.queryForList(sql, String.class, idObra);

			if (!resultados.isEmpty() && resultados.get(0) != null) {
				nomeEmpresa = resultados.get(0);
			} else {
				try {
					String sqlAlt = "SELECT nome_responsavel FROM obras WHERE id = ?";
					List<String> resultadosAlt = jdbcTemplate.queryForList(sqlAlt, String.class, Long.parseLong(idObra));
					
					if (!resultadosAlt.isEmpty() && resultadosAlt.get(0) != null) {
						nomeEmpresa = resultadosAlt.get(0);
					}
				} catch (Exception ex2) {
					System.out.println("⚠️ [Upload] Falha na busca por id numérico: " + ex2.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("⚠️ [Upload] Erro na consulta SQL da tabela 'obras': " + e.getMessage());
		}

		audit.setNomeResponsavel(nomeEmpresa);
		repository.save(audit);

		NutrientDwsService.DwsExtractionResult result = dwsService.extractData(storedPath);

		audit.setDwsDocumentId(result.documentId());
		audit.setDwsViewerUrl(result.viewerUrl());
		audit.setExtractedData(result.extractedDataJson());
		audit.setStatus(AuditStatus.EXTRACTED);
		repository.save(audit);

		return new AuditUploadResponse(audit.getId(), audit.getIdObra(), audit.getStatus(), audit.getFileName(),
				audit.getDwsDocumentId(), audit.getDwsViewerUrl(), audit.getUploadedAt());
	}

	public List<ContractAudit> listByObra(String idObra) {
		return repository.findByIdObra(idObra);
	}

	public ContractAudit findById(UUID id) {
		return repository.findById(id).orElse(null);
	}

	public ContractAudit approve(UUID id) {
		ContractAudit audit = repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Auditoria não encontrada: " + id));
		audit.setStatus(AuditStatus.APPROVED);
		return repository.save(audit);
	}

	private String storeFile(MultipartFile file) throws IOException {
		Path dir = Paths.get(uploadDir);
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
		}
		String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
		Path destination = dir.resolve(fileName);
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
		}
		return destination.toString();
	}
}