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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.civisanalytics.audit.dto.AuditUploadResponse;

@Service
public class AuditService {

	private final ContractAuditRepository repository;
	private final NutrientDwsService dwsService;
	private final String uploadDir;

	public AuditService(ContractAuditRepository repository, NutrientDwsService dwsService,
			@Value("${app.upload-dir}") String uploadDir) {
		this.repository = repository;
		this.dwsService = dwsService;
		this.uploadDir = uploadDir;
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