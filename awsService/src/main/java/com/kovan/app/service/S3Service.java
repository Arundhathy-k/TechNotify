package com.kovan.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.kovan.entity.Document;
import com.kovan.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static java.util.Objects.requireNonNull;

@Service
@Slf4j
public class S3Service {

    private final AmazonS3 s3Client;
    private final DocumentService service;

    public S3Service(AmazonS3 s3Client, DocumentService service) {
        this.s3Client = s3Client;
        this.service = service;
    }

    public String uploadFile(String bucketName, MultipartFile file) {

        File tempFile = convertMultipartToFile(file);
        String fileTypeFolder = determineFolder(file.getOriginalFilename());
        String path = fileTypeFolder + "/" + file.getOriginalFilename();

        String uniqueId = UUID.randomUUID().toString();

        service.saveOrUpdateDocument(Document.builder()
                .id(uniqueId)
                .fileName(file.getOriginalFilename()).build());
        try {
            log.info("Uploading file {} to bucket {}, key: {}", file.getOriginalFilename(), bucketName, path);
            s3Client.putObject(bucketName, path, tempFile);
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("File upload failed");
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
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private File convertMultipartToFile(MultipartFile file) {
        File convertedFile = new File(requireNonNull(file.getOriginalFilename()));
        try(FileOutputStream fos = new FileOutputStream(convertedFile)){
            fos.write(file.getBytes());
        }catch (IOException e) {
           log.error("Error converting multipartFile to file", e);
        }
        return convertedFile;
    }

    public byte[] downloadFile(String bucketName,String id) {

        S3Object s3Object = s3Client.getObject(bucketName, service.findDocumentById(id).getFileName());
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Error downloading file with id {} from bucket {}", id, bucketName, e);
        }
        return new byte[0];
    }
    public String deleteFile(String bucketName,String id){
        String fileName = service.findDocumentById(id).getFileName();
        s3Client.deleteObject(bucketName, fileName);
        return fileName + " removed ...";
    }

    public List<String> listFiles(String bucketName) {

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
        }
        return fileNames;
    }
}
