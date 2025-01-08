package com.kovan.app.controller;

import com.kovan.app.service.S3Service;
import com.kovan.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
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
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable("id") String id ) throws NoResourceFoundException {

        String filePath = service.findDocumentById(id).getFileName();
        if (isNull(filePath)) {
            throw new NoResourceFoundException(HttpMethod.GET,"Resource not found for this id.");
        }
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

        return ResponseEntity.ok()
                .contentLength(s3Service.downloadFile(id).length)
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body( new ByteArrayResource(s3Service.downloadFile(id)));
        }

    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(s3Service.uploadFile(file));
    }

    @PostMapping("/updateFile/{fileId}")
    public ResponseEntity<String> updateFile(@PathVariable String fileId, @RequestParam("file") MultipartFile newFile) {
        return ResponseEntity.ok("File updated successfully with ID: " + s3Service.updateFile(fileId, newFile));
    }

    @GetMapping("/filesList")
    public ResponseEntity<List<String>> listFiles() {
        return ResponseEntity.ok(s3Service.listFiles());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable("id") String id) {
        return ResponseEntity.ok(s3Service.deleteFile(id));
    }

    @PostMapping("/createBucket/{bucketName}")
    public ResponseEntity<String> createBucket(@PathVariable String bucketName) {
        return ResponseEntity.ok(s3Service.createBucket(bucketName));
    }

    @DeleteMapping("/deleteBucket/{bucketName}")
    public ResponseEntity<String> deleteBucket(@PathVariable String bucketName) {
        return ResponseEntity.ok(s3Service.deleteBucket(bucketName));
    }

    @PostMapping("/renameBucket/{oldBucketName}/{newBucketName}")
    public ResponseEntity<String> renameBucket(@PathVariable String oldBucketName,@PathVariable String newBucketName) {
        return ResponseEntity.ok(s3Service.renameBucket(oldBucketName, newBucketName));
    }
}
