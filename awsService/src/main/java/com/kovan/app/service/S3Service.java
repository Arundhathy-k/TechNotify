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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.micrometer.common.util.StringUtils.isBlank;
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
    public String createBucket(String bucketName) {
        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.createBucket(createBucketRequest);
            log.info("Bucket {} created successfully", bucketName);
            return "Bucket created successfully: " + bucketName;
        } catch (S3Exception e) {
            log.error("Error creating bucket {}: {}", bucketName, e.getMessage());
            return "Error creating bucket: " + e.getMessage();
        }
    }

    public String deleteBucket(String bucketName) {
        try {
            deleteAllObjects(bucketName);
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.deleteBucket(deleteBucketRequest);

            log.info("Bucket {} deleted successfully", bucketName);
            return "Bucket deleted successfully: " + bucketName;
        } catch (S3Exception e) {
            log.error("Error deleting bucket {}: {}", bucketName, e.getMessage());
            return "Error deleting bucket: " + e.getMessage();
        }
    }
    private void deleteAllObjects(String bucketName) {
    ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .build();
    List<CompletableFuture<Void>> objectDeletionFutures = s3Client.listObjectsV2Paginator(listObjectsV2Request).stream()
            .flatMap(response -> response.contents().stream())
            .map(s3Object -> CompletableFuture.runAsync(() -> {
                try {
                    DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Object.key())
                            .build();
                    s3Client.deleteObject(deleteObjectRequest);
                    log.info("Deleted object: {}", s3Object.key());
                } catch (S3Exception e) {
                    log.error("Error deleting object {}: {}", s3Object.key(), e.getMessage());
                }
            }))
            .toList();
    ListObjectVersionsRequest listObjectVersionsRequest = ListObjectVersionsRequest.builder()
            .bucket(bucketName)
            .build();

    List<CompletableFuture<Void>> versionDeletionFutures = s3Client.listObjectVersionsPaginator(listObjectVersionsRequest).stream()
            .flatMap(response -> response.versions().stream())
            .map(version -> CompletableFuture.runAsync(() -> {
                try {
                    DeleteObjectRequest deleteVersionRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(version.key())
                            .versionId(version.versionId())
                            .build();
                    s3Client.deleteObject(deleteVersionRequest);
                    log.info("Deleted version: {} for object: {}", version.versionId(), version.key());
                } catch (S3Exception e) {
                    log.error("Error deleting version {} for object {}: {}", version.versionId(), version.key(), e.getMessage());
                }
            }))
            .toList();
    CompletableFuture.allOf(
            CompletableFuture.allOf(objectDeletionFutures.toArray(new CompletableFuture[0])),
            CompletableFuture.allOf(versionDeletionFutures.toArray(new CompletableFuture[0]))
            ).join();
    log.info("All objects and versions deleted from bucket: {}", bucketName);
}

    public String renameBucket(String oldBucketName, String newBucketName) {
        try {
            createBucket(newBucketName);
            log.info("Created new bucket: {}", newBucketName);

            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(oldBucketName)
                    .build();
            List<CompletableFuture<Void>> futures = s3Client.listObjectsV2Paginator(listObjectsV2Request).stream()
                    .flatMap(response -> response.contents().stream())
                    .map(object -> CompletableFuture.runAsync(() -> {
                        try {
                            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                                    .sourceBucket(oldBucketName)
                                    .sourceKey(object.key())
                                    .destinationBucket(newBucketName)
                                    .destinationKey(object.key())
                                    .build();
                            s3Client.copyObject(copyObjectRequest);
                            log.info("Copied object {} from {} to {}", object.key(), oldBucketName, newBucketName);
                        } catch (S3Exception e) {
                            log.error("Error copying object {} from {} to {}: {}", object.key(), oldBucketName, newBucketName, e.getMessage());
                            throw new FileException("Failed to copy object: " + object.key(), e);
                        }
                    })).toList();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            deleteBucket(oldBucketName);
            log.info("Deleted old bucket: {}", oldBucketName);

            return "Bucket renamed successfully from " + oldBucketName + " to " + newBucketName;
        } catch (S3Exception e) {
            log.error("Error renaming bucket from {} to {}: {}", oldBucketName, newBucketName, e.getMessage());
            return "Error renaming bucket: " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error renaming bucket: {}", e.getMessage());
            return "Unexpected error renaming bucket: " + e.getMessage();
        }
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
        if (isBlank(id)) {
            throw new IllegalArgumentException("File ID cannot be null or empty.");
        }
        String fileName;
        try {
            fileName = service.findDocumentById(id).getFileName();
            if (isBlank(fileName)) {
                throw new FileException("File name not found for the given ID: " + id);
            }
        } catch (Exception e) {
            log.error("Error retrieving file information for ID {}", id, e);
            throw new FileException("Failed to retrieve file details for deletion.", e);
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        try {
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File {} deleted successfully from bucket {}", fileName, bucketName);
            service.deleteFile(id);
            log.info("File record with ID {} deleted successfully from database", id);
            return fileName + " removed ...";
        } catch (Exception e) {
            log.error("Error deleting file {} from bucket {}", fileName, bucketName, e);
            throw new FileException("File deletion failed for " + fileName, e);
        }
    }

    public List<String> listFiles() {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            log.debug("S3 Response: {}", response);

            List<String> fileNames = response.contents().stream()
                    .map(S3Object::key)
                    .toList();

            log.info("Listed {} files in bucket {}", fileNames.size(), bucketName);
            return fileNames;
        } catch (Exception e) {
            log.error("Error listing files in bucket {}", bucketName, e);
            throw new FileException("Failed to list files in bucket: " + bucketName, e);
        }
    }

}
