package com.kovan.app.service;

import com.kovan.entity.Document;
import com.kovan.exception.FileException;
import com.kovan.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static io.micrometer.common.util.StringUtils.isBlank;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;

@Service
@Slf4j
public class S3Service {

    @Value("${bucketName}")
    private String bucketName;

    private final S3Client s3Client;
    private final DocumentService service;

    public S3Service(S3Client s3Client, DocumentService service) {
        this.s3Client = s3Client;
        this.service = service;
    }

    public String uploadFile(MultipartFile file) {

        String fileTypeFolder = determineFolder(file.getOriginalFilename());
        String path = fileTypeFolder + "/" + file.getOriginalFilename();

        String uniqueId = randomUUID().toString();

        service.saveDocument(Document.builder()
                .id(uniqueId)
                .fileName(path).build());
        try {
            log.info("Uploading file {} to bucket {}, key: {}", file.getOriginalFilename(), bucketName, path);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build();
            s3Client.putObject(putObjectRequest,RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new FileException("File upload failed", e);
        }
        return uniqueId;
    }

    private String determineFolder(String fileName) {

        String fileExtension = getFileExtension(fileName);
        return switch (fileExtension.toLowerCase()) {
            case "pdf" -> "pdfs";
            case "doc", "docx" -> "docs";
            case "jpg", "jpeg", "png" -> "images";
            case "txt" -> "texts";
            default -> "others";
        };
    }

    private String getFileExtension(String fileName) {
        if (isBlank(fileName) || !fileName.contains(".")) {
            throw new FileException("Invalid file name: " + fileName);
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    public byte[] downloadFile(String id) {

        String fileName = service.findDocumentById(id).getFileName();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        try (ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(getObjectRequest);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            log.info("File with id {} downloaded successfully from bucket {}", id, bucketName);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Error downloading file with id {} from bucket {}", id, bucketName, e);
            throw new FileException("File download failed", e);
        }
    }

    public String deleteFile(String id) {
        String fileName = service.findDocumentById(id).getFileName();

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        try {
            s3Client.deleteObject(deleteObjectRequest);
            service.deleteFile(id);

            log.info("File {} deleted successfully from bucket {}", fileName, bucketName);
            return fileName + " removed ...";
        } catch (Exception e) {
            log.error("Error deleting file {} from bucket {}", fileName, bucketName, e);
            throw new FileException("File deletion failed", e);
        }
    }

    public List<String> listFiles() {
        List<String> fileNames = new ArrayList<>();
        try {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

            while (true) {
                for (S3Object s3Object : listObjectsV2Response.contents()) {
                    fileNames.add(s3Object.key());
                }

                if (TRUE.equals(listObjectsV2Response.isTruncated())) {
                    listObjectsV2Request = listObjectsV2Request.toBuilder()
                            .continuationToken(listObjectsV2Response.nextContinuationToken())
                            .build();
                    listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
                } else {
                    break;
                }
            }

            log.info("Listed {} files in bucket {}", fileNames.size(), bucketName);
        } catch (Exception e) {
            log.error("Error listing files in bucket {}", bucketName, e);
            throw new FileException("Failed to list files in bucket: " + bucketName, e);
        }

        return fileNames;
    }
}
