package com.kovan.app.controller;

import com.kovan.app.service.S3Service;
import com.kovan.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

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
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable("id") String id) {
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
    public ResponseEntity<List<String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(s3Service.uploadFile(file));
    }

    @GetMapping("/filesList")
    public ResponseEntity<List<String>> listFiles() {

        return ResponseEntity.ok(s3Service.listFiles());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable("id") String id){
        return new ResponseEntity<>(s3Service.deleteFile(id),HttpStatus.OK);
    }
}
