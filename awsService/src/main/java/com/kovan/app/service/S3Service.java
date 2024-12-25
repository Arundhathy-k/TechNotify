package com.kovan.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.kovan.entity.Document;
import com.kovan.exception.FileException;
import com.kovan.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static com.amazonaws.util.IOUtils.toByteArray;
import static io.micrometer.common.util.StringUtils.isBlank;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

@Service
@Slf4j
public class S3Service {

    @Value("${bucketName}")
    private String bucketName;

    private final AmazonS3 s3Client;
    private final DocumentService service;

    public S3Service(AmazonS3 s3Client, DocumentService service) {
        this.s3Client = s3Client;
        this.service = service;
    }

    public String uploadFile(MultipartFile file) {

        File tempFile = convertMultipartToFile(file);
        String fileTypeFolder = determineFolder(file.getOriginalFilename());
        String path = fileTypeFolder + "/" + file.getOriginalFilename();

        String uniqueId = randomUUID().toString();

        service.saveDocument(Document.builder()
                .id(uniqueId)
                .fileName(file.getOriginalFilename()).build());
        try {
            log.info("Uploading file {} to bucket {}, key: {}", file.getOriginalFilename(), bucketName, path);
            s3Client.putObject(bucketName, path, tempFile);
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new FileException("File upload failed", e);
        } finally {
            if (!tempFile.delete()) {
                log.warn("Temporary file {} could not be deleted", tempFile.getName());
            }
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

    private File convertMultipartToFile(MultipartFile file) {
        String fileName = requireNonNull(file.getOriginalFilename(), "File name cannot be null");
        File convertedFile = new File(fileName);

        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            try {
                fos.write(file.getBytes());
            } catch (IOException writeException) {
                log.error("Error writing file bytes to FileOutputStream", writeException);
                throw new FileException("Error writing to file", writeException);
            }
        } catch (IOException e) {
            log.error("Error converting MultipartFile to File", e);
            if (convertedFile.exists() && !convertedFile.delete()) {
                log.warn("Failed to delete partially created file: {}", convertedFile.getAbsolutePath());
            }
            throw new FileException("Error converting file", e);
        }

        return convertedFile;
    }

    public byte[] downloadFile(String id) {
        String fileName = service.findDocumentById(id).getFileName();

        try (S3Object s3Object = s3Client.getObject(bucketName, fileName);
             S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
            return toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Error downloading file with id {} from bucket {}", id, bucketName, e);
            throw new FileException("File download failed", e);
        }
    }

    public String deleteFile(String id){

        String fileName = service.findDocumentById(id).getFileName();
        s3Client.deleteObject(bucketName, fileName);
        service.deleteFile(id);
        return fileName + " removed ...";
    }

    public List<String> listFiles() {
        List<String> fileNames = new ArrayList<>();
        try {
            ObjectListing objectListing = s3Client.listObjects(bucketName);

            while (true) {
                for (S3ObjectSummary os : objectListing.getObjectSummaries()) {
                    fileNames.add(os.getKey());
                }
                if (objectListing.isTruncated()) {
                    objectListing = s3Client.listNextBatchOfObjects(objectListing);
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
