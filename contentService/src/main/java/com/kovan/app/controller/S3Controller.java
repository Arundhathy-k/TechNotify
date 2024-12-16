package com.kovan.app.controller;

import com.kovan.app.service.S3Service;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/s3")
public class S3Controller {

   private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/{bucketName}/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("bucketName") String bucketName,
                                                 @PathVariable("id") Long id ) throws IOException {

        try {
         Resource resource = s3Service.downloadFile(bucketName,id);
          String contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename()+ "\"")
                    .body(resource);
        }
        catch (IOException e) {
            System.err.println("Error downloading file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/{bucketName}/upload")
    public ResponseEntity<String> uploadFile(@PathVariable("bucketName") String bucketName,@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is missing or empty");
        }
        s3Service.uploadFile(bucketName,file);
        return ResponseEntity.ok("File uploaded successfully");
    }
}
