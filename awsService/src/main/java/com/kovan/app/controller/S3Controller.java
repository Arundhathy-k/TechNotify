package com.kovan.app.controller;

import com.kovan.app.service.S3Service;
import com.kovan.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @GetMapping("/{bucketName}/download/{id}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable("bucketName") String bucketName,
                                                          @PathVariable("id") String id ){
        String fileName = service.findDocumentById(id).getFileName();
         byte[] data = s3Service.downloadFile(bucketName,id);
         ByteArrayResource resource = new ByteArrayResource(data);
            return ResponseEntity.ok()
                    .contentLength(data.length)
                    .header("Content-type","application/octet-stream")
                    .header("Content-disposition", "attachment; filename=\"" +fileName + "\"")
                    .body(resource);
        }

    @PostMapping("/{bucketName}/upload")
    public ResponseEntity<List<String>> uploadFile(@PathVariable("bucketName") String bucketName,@RequestParam("file") MultipartFile file) throws Exception {

        return ResponseEntity.ok(s3Service.uploadFile(bucketName, file));
    }

    @GetMapping("/{bucketName}/filesList")
    public ResponseEntity<List<String>> listFiles(@PathVariable("bucketName") String bucketName) {

        return ResponseEntity.ok(s3Service.listFiles(bucketName));
    }

    @DeleteMapping("/{bucketName}/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable("bucketName") String bucketName,@PathVariable("id") String id){
        return new ResponseEntity<>(s3Service.deleteFile(bucketName,id),HttpStatus.OK);
    }
}
