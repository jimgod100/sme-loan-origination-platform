package com.smeloan.platform.application.repository;

import com.smeloan.platform.application.entity.Document;
import com.smeloan.platform.application.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByLoanApplicationId(Long applicationId);

    List<Document> findByLoanApplicationIdAndDocumentType(Long applicationId, DocumentType documentType);
}
