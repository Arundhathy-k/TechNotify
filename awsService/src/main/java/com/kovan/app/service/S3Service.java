package com.kovan.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.kovan.app.util.PdfConverter;
import com.kovan.app.util.User;
import com.kovan.entity.Document;
import com.kovan.exception.FileException;
import com.kovan.service.DocumentService;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static com.amazonaws.util.IOUtils.toByteArray;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Slf4j
public class S3Service {

    @Value("${bucketName}")
    private String bucketName;

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

    public List<String> uploadFile(MultipartFile file) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        return "xlsx".equalsIgnoreCase(fileExtension)
                ? processExcelFile(file)
                : of(processUpload(convertMultipartToFile(file)));
    }

    private List<String> processExcelFile(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            return stream(sheet.spliterator(), false)
                    .skip(1)
                    .map(row -> {
                        try {
                            return processRow(row);
                        } catch (IOException e) {
                            throw new FileException("Error processing row", e);
                        }
                    }).toList();
        }
    }

    private String processRow(Row row) throws IOException {
        User user = User.builder()
                .firstName(getCellValue(row, 0))
                .lastName(getCellValue(row, 1))
                .gender(getCellValue(row, 2))
                .phone(getCellValue(row, 3))
                .primaryAddress1(getCellValue(row, 4))
                .primaryAddress2(getCellValue(row, 5))
                .primaryCity(getCellValue(row, 6))
                .primaryState(getCellValue(row, 7))
                .primaryZip(getCellValue(row, 8))
                .secondaryAddress1(getCellValue(row, 9))
                .secondaryAddress2(getCellValue(row, 10))
                .secondaryCity(getCellValue(row, 11))
                .secondaryState(getCellValue(row, 12))
                .secondaryZip(getCellValue(row, 13))
                .companyName(getCellValue(row, 14))
                .companyLocation(getCellValue(row, 15))
                .companyDesignation(getCellValue(row, 16))
                .dateOfJoining(getCellValue(row, 17))
                .experience(getIntCellValue(row,18))
                .build();

        String html = htmlGeneratorService.generateHtml(user);
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = user.getFirstName() + "_" + timestamp + ".pdf";
        File pdfFile = pdfConverter.convertHtmlToPdf(html, fileName);
        return processUpload(pdfFile);
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        return cell != null ? cell.toString() : "";
    }
    private double getIntCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell != null) {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    try {
                        return Double.parseDouble(cell.getStringCellValue());
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                default:
                    return 0.0;
            }
        }
        return 0.0;
    }

    private String processUpload(File tempFile) {
        String fileTypeFolder = determineFolder(tempFile.getName());
        String path = fileTypeFolder + "/" + tempFile.getName();
        String uniqueId = randomUUID().toString();

        String storedId = documentService.saveDocument(Document.builder()
                .id(uniqueId)
                .fileName(path)
                .build());

        try {
            log.info("Uploading file {} to bucket {}, key: {}", tempFile.getName(), bucketName, path);
            s3Client.putObject(bucketName, path, tempFile);
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new FileException("File upload failed", e);
        } finally {
            if (!tempFile.delete()) {
                log.warn("Temporary file {} could not be deleted", tempFile.getName());
            }
        }

        return tempFile.getName() + " uploaded successfully with id: " + storedId;
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
        File convertedFile = new File(requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            log.error("Error converting MultipartFile to File", e);
            throw new FileException("Error converting file", e);
        }
        return convertedFile;
    }

    public byte[] downloadFile(String id) {
        Document document = ofNullable(documentService.findDocumentById(id))
                .orElseThrow(() -> new FileException("Document with id " + id + " not found"));

        String fileName = document.getFileName();
        try (S3Object s3Object = s3Client.getObject(bucketName, fileName);
             S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
            return toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Error downloading file with id {} from bucket {}", id, bucketName, e);
            throw new FileException("File download failed", e);
        }
    }

    public String deleteFile(String id) {

        String fileName = documentService.findDocumentById(id).getFileName();
        s3Client.deleteObject(bucketName, fileName);
        documentService.deleteFile(id);
        return fileName + " removed.";
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
