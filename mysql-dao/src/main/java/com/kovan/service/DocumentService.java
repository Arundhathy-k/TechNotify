package com.kovan.service;

import com.kovan.entity.Document;
import com.kovan.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import static java.time.Instant.now;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void saveOrUpdateDocument(Document document) {

        Optional<Document> existingDocumentOpt = documentRepository.findByFileName(document.getFileName());

        if (existingDocumentOpt.isPresent()) {
            Document existingDocument = existingDocumentOpt.get();
            existingDocument.setUpdatedBy(document.getUpdatedBy());
            existingDocument.setUpdatedDate(document.getUpdatedDate());
            documentRepository.save(existingDocument);
        } else {
            documentRepository.save(Document.builder()
                    .id(document.getId())
                    .fileName(document.getFileName())
                    .createdBy(document.getCreatedBy())
                    .createdDate(now())
                    .updatedBy(document.getUpdatedBy())
                    .updatedDate(now())
                    .build());
        }
    }

    public List<Document> getAllDocumentsFromDb() {
       return documentRepository.findAll();
    }

    public Optional<Document> findByFileName(String fileName) {
        return documentRepository.findByFileName(fileName);
    }
    public Document findDocumentById(String id){
        return documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Data not found for id: " + id));
    }
}
