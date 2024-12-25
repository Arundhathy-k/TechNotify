package com.kovan.app.controller;

import com.kovan.app.service.S3Service;
import com.kovan.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static java.util.Objects.isNull;

@RestController
@RequestMapping("/s3")
public class S3Controller {

    private final S3Service s3Service;
    private final DocumentService service;

    public S3Controller(S3Service s3Service, DocumentService service) {
        this.s3Service = s3Service;
        this.service = service;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable("id") String id) {
        try {
            String fileName = service.findDocumentById(id).getFileName();
            byte[] data = s3Service.downloadFile(id);
            ByteArrayResource resource = new ByteArrayResource(data);
            return ResponseEntity.ok()
                    .contentLength(data.length)
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while downloading file: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (isNull(file ) || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is missing or empty");
        }
        try {
            return ResponseEntity.ok("File uploaded successfully with ID: " + s3Service.uploadFile(file));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while uploading file: " + e.getMessage());
        }
    }

    @GetMapping("/filesList")
    public ResponseEntity<?> listFiles() {
        try {
            return ResponseEntity.ok(s3Service.listFiles());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while fetching files list: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(s3Service.deleteFile(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while deleting file: " + e.getMessage());
        }
    }
}
