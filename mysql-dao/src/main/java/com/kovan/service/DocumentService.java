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

    public String saveDocument(Document document) {

        documentRepository.save(Document.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .createdBy(document.getCreatedBy())
                .createdDate(now().toString())
                .updatedBy(document.getUpdatedBy())
                .updatedDate(now().toString())
                .build());
        return document.getId();
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
    public void deleteFile(String id){
        documentRepository.deleteById(id);
    }
}
