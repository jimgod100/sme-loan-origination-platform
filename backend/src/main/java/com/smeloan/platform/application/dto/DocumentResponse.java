package com.smeloan.platform.application.dto;

import java.time.OffsetDateTime;

/**
 * Response record for an uploaded document.
 *
 * @param id           document database identifier
 * @param documentType type of document (FINANCIAL_STATEMENT, ID_CARD, etc.)
 * @param fileName     original file name
 * @param fileSize     file size in bytes
 * @param mimeType     MIME content type
 * @param uploadedBy   username who uploaded the document
 * @param uploadedAt   upload timestamp
 */
public record DocumentResponse(
    Long id,
    String documentType,
    String fileName,
    Long fileSize,
    String mimeType,
    String uploadedBy,
    OffsetDateTime uploadedAt
) {}
