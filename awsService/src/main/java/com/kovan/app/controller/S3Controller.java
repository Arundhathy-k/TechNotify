package com.kovan.app.controller;

import com.kovan.app.service.S3Service;
import com.kovan.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
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
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable("id") String id ){
        String filePath = service.findDocumentById(id).getFileName();
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        byte[] data = s3Service.downloadFile(id);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentLength(data.length)
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
        }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file){
        if (isNull(file) || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is missing or empty");
        }
        String uniqueId = s3Service.uploadFile(file);
        return ResponseEntity.ok("File uploaded successfully with ID: " + uniqueId);
    }

    @GetMapping("/filesList")
    public ResponseEntity<List<String>> listFiles() {
        List<String> files = s3Service.listFiles();
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable("id") String id){
        return ResponseEntity.ok(s3Service.deleteFile(id));
    }

    @PostMapping("/createBucket/{bucketName}")
    public ResponseEntity<String> createBucket(@PathVariable String bucketName) {
        String response = s3Service.createBucket(bucketName);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deleteBucket/{bucketName}")
    public ResponseEntity<String> deleteBucket(@PathVariable String bucketName) {
        String response = s3Service.deleteBucket(bucketName);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/renameBucket/{oldBucketName}/{newBucketName}")
    public ResponseEntity<String> renameBucket(@PathVariable String oldBucketName,@PathVariable String newBucketName) {
        String response = s3Service.renameBucket(oldBucketName, newBucketName);
        return ResponseEntity.ok(response);
    }
}
