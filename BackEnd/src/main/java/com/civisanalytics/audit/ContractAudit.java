package com.civisanalytics.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contract_audits")
@Getter
@Setter
@NoArgsConstructor
public class ContractAudit {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "id_obra", nullable = false)
	private String idObra;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "file_path", nullable = false)
	private String filePath;

	@Column(name = "file_size_bytes")
	private Long fileSizeBytes;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private AuditStatus status = AuditStatus.PENDING_EXTRACTION;

	@Column(name = "dws_document_id")
	private String dwsDocumentId;

	@Column(name = "dws_viewer_url")
	private String dwsViewerUrl;

	@Column(name = "extracted_data")
	private String extractedData;

	@Column(name = "auditor_notes")
	private String auditorNotes;

	@Column(name = "nome_responsavel")
	private String nomeResponsavel;

	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private OffsetDateTime uploadedAt = OffsetDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getIdObra() {
		return idObra;
	}

	public void setIdObra(String idObra) {
		this.idObra = idObra;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Long getFileSizeBytes() {
		return fileSizeBytes;
	}

	public void setFileSizeBytes(Long fileSizeBytes) {
		this.fileSizeBytes = fileSizeBytes;
	}

	public AuditStatus getStatus() {
		return status;
	}

	public void setStatus(AuditStatus status) {
		this.status = status;
	}

	public String getDwsDocumentId() {
		return dwsDocumentId;
	}

	public void setDwsDocumentId(String dwsDocumentId) {
		this.dwsDocumentId = dwsDocumentId;
	}

	public String getDwsViewerUrl() {
		return dwsViewerUrl;
	}

	public void setDwsViewerUrl(String dwsViewerUrl) {
		this.dwsViewerUrl = dwsViewerUrl;
	}

	public String getExtractedData() {
		return extractedData;
	}

	public void setExtractedData(String extractedData) {
		this.extractedData = extractedData;
	}

	public String getAuditorNotes() {
		return auditorNotes;
	}

	public void setAuditorNotes(String auditorNotes) {
		this.auditorNotes = auditorNotes;
	}

	public String getNomeResponsavel() {
		return nomeResponsavel;
	}

	public void setNomeResponsavel(String nomeResponsavel) {
		this.nomeResponsavel = nomeResponsavel;
	}

	public OffsetDateTime getUploadedAt() {
		return uploadedAt;
	}

	public void setUploadedAt(OffsetDateTime uploadedAt) {
		this.uploadedAt = uploadedAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}