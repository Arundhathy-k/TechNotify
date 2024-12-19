package com.kovan.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.kovan.app.util.PdfConverter;
import com.kovan.entity.Document;
import com.kovan.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import static com.amazonaws.util.IOUtils.toByteArray;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.iterate;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Slf4j
public class S3Service {

    private final AmazonS3 s3Client;
    private final DocumentService documentService;
    private final HtmlGeneratorService htmlGeneratorService;
    private final PdfConverter pdfConverter;

    public S3Service(AmazonS3 s3Client, DocumentService documentService, HtmlGeneratorService htmlGeneratorService, PdfConverter pdfConverter) {
        this.s3Client = s3Client;
        this.documentService = documentService;
        this.htmlGeneratorService = htmlGeneratorService;
        this.pdfConverter = pdfConverter;
    }
    public List<String> uploadFile(String bucketName, MultipartFile file) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        return "xlsx".equalsIgnoreCase(fileExtension)
                ? processExcelFile(bucketName, file)
                : List.of(processUpload(bucketName, convertMultipartToFile(file)));
    }

    private List<String> processExcelFile(String bucketName, MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            return stream(sheet.spliterator(), false)
                    .skip(1) // Skip header row
                    .map(row -> {
                        try {
                            return processRow(bucketName, row);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();
        }
    }

    private String processRow(String bucketName, Row row) throws IOException {
        String userId = getCellValue(row, 0);
        String name = getCellValue(row, 1);
        String email = getCellValue(row, 2);
        String phone = getCellValue(row, 3);
        String address = getCellValue(row, 4);

        String html = htmlGeneratorService.generateHtml(userId, name, email, phone, address);
        File pdfFile = pdfConverter.convertHtmlToPdf(html, userId+name + ".pdf");
        return processUpload(bucketName, pdfFile);
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        return cell != null ? cell.toString() : "";
    }

    private String processUpload(String bucketName, File tempFile) {
        String fileTypeFolder = determineFolder(tempFile.getName());
        String path = fileTypeFolder + "/" + tempFile.getName();
        String uniqueId = UUID.randomUUID().toString();

        String storedId = documentService.saveOrUpdateDocument(Document.builder()
                .id(uniqueId)
                .fileName(path)
                .build());

        try {
            log.info("Uploading file {} to bucket {}, key: {}", tempFile.getName(), bucketName, path);
            s3Client.putObject(bucketName, path, tempFile);
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("File upload failed", e);
        } finally {
            if (!tempFile.delete()) {
                log.warn("Temporary file {} could not be deleted", tempFile.getName());
            }
        }

        return tempFile.getName()+" uploaded successfully with id: "+storedId;
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
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private File convertMultipartToFile(MultipartFile file) {
        File convertedFile = new File(requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            log.error("Error converting MultipartFile to File", e);
            throw new RuntimeException("Error converting file", e);
        }
        return convertedFile;
    }

    public byte[] downloadFile(String bucketName, String id) {
        Document document = ofNullable(documentService.findDocumentById(id))
                .orElseThrow(() -> new IllegalArgumentException("Document with id " + id + " not found"));

        String fileName = document.getFileName();
        try (S3Object s3Object = s3Client.getObject(bucketName, fileName);
             S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
            return toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Error downloading file with id {} from bucket {}", id, bucketName, e);
            throw new RuntimeException("File download failed", e);
        }
    }

    public String deleteFile(String bucketName, String id) {
        Document document = ofNullable(documentService.findDocumentById(id))
                .orElseThrow(() -> new IllegalArgumentException("Document with id " + id + " not found"));

        String fileName = document.getFileName();
        s3Client.deleteObject(bucketName, fileName);
        return fileName + " removed.";
    }

    public List<String> listFiles(String bucketName) {
        try {
            return iterate(s3Client.listObjects(bucketName), ObjectListing::isTruncated, s3Client::listNextBatchOfObjects)
                    .flatMap(objectListing -> objectListing.getObjectSummaries().stream())
                    .map(S3ObjectSummary::getKey)
                    .toList();
        } catch (Exception e) {
            log.error("Error listing files in bucket {}", bucketName, e);
            return emptyList();
        }
    }
}
