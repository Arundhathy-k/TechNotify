package com.kovan.app.service;

import com.kovan.app.util.PdfConverter;
import com.kovan.app.util.User;
import com.kovan.entity.Document;
import com.kovan.app.exception.FileException;
import com.kovan.service.DocumentService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import static io.micrometer.common.util.StringUtils.isBlank;
import static java.lang.Double.parseDouble;
import static java.time.Instant.now;
import static java.util.List.of;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import static java.util.stream.StreamSupport.stream;

@Service
@Slf4j
public class S3Service {

    @Value("${bucketName}")
    private String bucketName;

    private final S3Client s3Client;
    private final DocumentService service;
    private final HtmlGeneratorService htmlGeneratorService;
    private final PdfConverter pdfConverter;
    private final Validator validator;
    private final ExecutorService executorService = newFixedThreadPool(10);
    private final ExecutorService cpuExecutor = newFixedThreadPool(5);  // CPU-bound tasks like PDF generation
    private final ExecutorService ioExecutor = newFixedThreadPool(10);  // IO-bound tasks like file upload

    public S3Service(S3Client s3Client, DocumentService service, HtmlGeneratorService htmlGeneratorService, PdfConverter pdfConverter, Validator validator) {
        this.s3Client = s3Client;
        this.service = service;
        this.htmlGeneratorService = htmlGeneratorService;
        this.pdfConverter = pdfConverter;
        this.validator = validator;
    }

    /**
     * Creates an Amazon S3 bucket with the specified name.
     *
     * @param bucketName the name of the bucket to be created.
     *
     * @return a message indicating the result of the bucket creation:
     *         - "Bucket created successfully: <bucketName>" on success.
     *         - "Error creating bucket: <errorMessage>" on failure.
     * @throws S3Exception if an error occurs during the S3 bucket creation process,
     *                     such as insufficient permissions or naming conflicts or AWS service errors.
     * Logs:
     * - INFO: Logs a success message with the bucket name if the operation succeeds.
     * - ERROR: Logs an error message with details if the operation fails.
     */

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

    /**
     * Deletes an Amazon S3 bucket and all its contents.
     *
     * @param bucketName the name of the bucket to be deleted.
     *
     * @return a message indicating the result of the bucket deletion:
     *         - "Bucket deleted successfully: <bucketName>" on success.
     *         - "Error deleting bucket: <errorMessage>" on failure.
     * @throws S3Exception if:
     *         - The specified bucket does not exist in the user's AWS account.
     *         - The user lacks permissions to delete the bucket or its contents.
     *         - The bucket is not empty and contains objects.
     *         - There are network or AWS service-related issues during the operation.
     * Logs:
     * - INFO: Logs a success message with the bucket name if the operation succeeds.
     * - ERROR: Logs an error message with details if the operation fails.
     */
    public String deleteBucket(String bucketName) {
        try {
            // Deletes all objects within the specified bucket.
            // This ensures that the bucket is empty, as S3 buckets cannot be deleted unless empty.
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
    /**
     * Deletes all objects and their versions from the specified S3 bucket.
     *
     * <p>This method performs the following tasks:</p>
     * <ul>
     *   <li>Retrieves all objects stored in the S3 bucket using a paginator.</li>
     *   <li>Deletes each object asynchronously using a thread pool (executor service).</li>
     *   <li>Retrieves all object versions (for version-enabled buckets) and deletes them asynchronously.</li>
     *   <li>Ensures all deletions are completed before returning from the method.</li>
     *   <li>Shuts down the executor service gracefully after completion.</li>
     * </ul>
     * <p>Detailed explanation:</p>
     * CompletableFuture is used to manage and execute asynchronous tasks. In this method,
     * each object or version deletion is wrapped in a CompletableFuture. This allows multiple deletion operations
     * to occur concurrently without blocking the main thread, making the process faster and more efficient.
     * <ul>
     * <li>ListObjectsV2Request: Create a request to list all objects in the S3 bucket.
     *    It returns a paginated list, which means we can retrieve large sets of data in smaller chunks.</li>
     * <li>listObjectsV2Paginator: A paginator that handles the retrieval of all objects in a bucket
     *             across multiple pages, ensuring all objects are listed, even if the result set is large.</li>
     * <li>flatMap: Used to flatten the paginated responses. Since listObjectsV2Paginator returns a stream
     *             of pages (each page containing a list of objects), flatMap transforms this into a stream of objects.</li>
     * <li>map: Converts each S3 object into a CompletableFuture that deletes the object asynchronously.</li>
     * <li>runAsync: Used to execute a task asynchronously. For each S3 object or version, a CompletableFuture is
     * created and executed in a background thread provided by the executorService. This ensures the deletion tasks do
     * not block the main execution flow.</li>
     * <li>executorService: Manages the execution of asynchronous tasks. It allows for multi-threading and parallelism.</li>
     * <li>ListObjectVersionsRequest: Used to request a list of all versions of objects in the bucket.
     *             This is important for version-enabled buckets where objects may have multiple versions.</li>
     * <li>CompletableFuture.allOf(): Used to wait for multiple asynchronous tasks to complete.
     * It takes a collection of CompletableFuture instances and returns a new CompletableFuture that completes
     * when all provided futures complete. This is important because we want to ensure all object and version
     * deletions are finished before the method exits.</li>
     * <li>join: Used to block the current thread until all CompletableFuture instances have completed.
     * This ensures the method will only proceed once all deletions have been fully executed, and no tasks are
     * left unfinished.</li>
     *</ul>
     * @param bucketName The name of the S3 bucket from which to delete all objects and versions.
     * @throws FileException If any error occurs during the deletion of objects or their versions,
     *                       or if the executor service fails to shut down properly.
     */

    public void deleteAllObjects(String bucketName) {
        try {

            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).build();

            List<CompletableFuture<Void>> objectDeletionFutures = s3Client.listObjectsV2Paginator(listObjectsV2Request).stream()
                    .flatMap(response -> response.contents().stream())
                    .map(s3Object -> CompletableFuture.runAsync(() -> {
                        try {
                            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                                    .bucket(bucketName).key(s3Object.key()).build();

                            s3Client.deleteObject(deleteObjectRequest);
                            log.info("Deleted object: {}", s3Object.key());
                        } catch (S3Exception e) {
                            log.error("Error deleting object {}: {}", s3Object.key(), e.getMessage());
                            throw new FileException("Error deleting object: " + s3Object.key(), e);
                        }
                    }, executorService)) // Execute each deletion task using the executor service.
                    .toList();

            ListObjectVersionsRequest listObjectVersionsRequest = ListObjectVersionsRequest.builder()
                    .bucket(bucketName).build();

            List<CompletableFuture<Void>> versionDeletionFutures = s3Client.listObjectVersionsPaginator(listObjectVersionsRequest).stream()
                    .flatMap(response -> response.versions().stream()) // Flatten the list of versions from each page.
                    .map(version -> CompletableFuture.runAsync(() -> {
                        try {
                            DeleteObjectRequest deleteVersionRequest = DeleteObjectRequest.builder()
                                    .bucket(bucketName).key(version.key()).versionId(version.versionId()).build();

                            s3Client.deleteObject(deleteVersionRequest);
                            log.info("Deleted version: {} for object: {}", version.versionId(), version.key());
                        } catch (S3Exception e) {
                            log.error("Error deleting version {} for object {}: {}", version.versionId(), version.key(), e.getMessage());
                            throw new FileException("Error deleting version: " + version.versionId() + " for object: " + version.key(), e);
                        }
                    }, executorService)).toList();

            // Wait for all deletion tasks to complete before proceeding.
            CompletableFuture.allOf(
                    CompletableFuture.allOf(objectDeletionFutures.toArray(new CompletableFuture[0])), // Ensure all object deletions complete.
                    CompletableFuture.allOf(versionDeletionFutures.toArray(new CompletableFuture[0]))).join(); // Ensure all version deletions complete.

            log.info("All objects and versions deleted from bucket: {}", bucketName);

        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Renames an S3 bucket by creating a new bucket, copying all objects from the old bucket to the new one,
     * and then deleting the old bucket.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Creates a new bucket with the specified new name.</li>
     *   <li>Lists all objects in the old bucket and copies each object to the new bucket asynchronously.</li>
     *   <li>Waits for all copy operations to complete using {@code CompletableFuture.allOf()}.</li>
     *   <li>Deletes the old bucket after all objects have been copied successfully.</li>
     *   <li>Logs each operation, including any errors encountered during the process.</li>
     * </ol>
     *<p>Detailed explanation</p>
     * ListObjectsV2Request - is used to request a list of all objects in the old bucket.The result is paginated,
     *  allowing for retrieval of large sets of objects.
     *  ListObjectsV2Paginator retrieves all objects from the old bucket, even if the result set is large.
     * flatMap - Flattens the paginated responses, converting them into a stream of individual objects.
     * map - Converts each object into a CompletableFuture that copies the object to the new bucket asynchronously.
     * executorService - Handles the execution of asynchronous tasks in parallel, improving performance.
     * CopyObjectRequest specifies the source (old bucket) and destination (new bucket) for each object that needs to be copied.
     * futures.toArray(new CompletableFuture[0]) - converts the list of CompletableFuture objects into an array,
     *    as CompletableFuture.allOf() requires an array rather than a list.
     * Join - Used to block the current thread until all CompletableFuture instances have completed.
     *  This ensures the method will only proceed once all deletions have been fully executed, and no tasks are
     *  left unfinished.
     * <p><strong>About {@code CompletableFuture}:</strong>
     * {@code CompletableFuture} is used to perform asynchronous tasks, in this case, copying the objects from
     * the old bucket to the new one. Each copy operation is executed asynchronously in a separate thread using
     * {@code runAsync}, allowing the method to copy multiple objects concurrently.
     * The {@code CompletableFuture.allOf()} method is then used to ensure that all copy operations are
     * completed before proceeding with the deletion of the old bucket.</p>
     *
     * @param oldBucketName The name of the existing S3 bucket that will be renamed.
     * @param newBucketName The name of the new S3 bucket that will be created.
     * @return A message indicating whether the bucket renaming was successful or not.
     *        - "Bucket renamed successfully from old-bucket-name to new-bucket-name" on success.
     *        - "Error renaming bucket: <error-message>" on failure.
     */
    public String renameBucket(String oldBucketName, String newBucketName) {
        try {
            createBucket(newBucketName);
            log.info("Created new bucket: {}", newBucketName);

            // List all objects in the old bucket.
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(oldBucketName).build();

            // Copy all objects from the old bucket to the new bucket asynchronously.
            List<CompletableFuture<Void>> futures = s3Client.listObjectsV2Paginator(listObjectsV2Request).stream()
                    .flatMap(response -> response.contents().stream()) // Flatten the list of objects from all pages.
                    .map(object -> CompletableFuture.runAsync(() -> {
                        try {
                            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                                    .sourceBucket(oldBucketName).sourceKey(object.key())
                                    .destinationBucket(newBucketName).destinationKey(object.key()).build();

                            s3Client.copyObject(copyObjectRequest);
                            log.info("Copied object {} from {} to {}", object.key(), oldBucketName, newBucketName);
                        } catch (S3Exception e) {
                            log.error("Error copying object {} from {} to {}: {}", object.key(), oldBucketName, newBucketName, e.getMessage());
                            throw new FileException("Failed to copy object: " + object.key(), e);
                        }
                    }, executorService)) // Execute the copy task asynchronously.
                    .toList();

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
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Uploads a file asynchronously based on its extension.
     * <p>
     * This method checks if the file is an Excel file (with ".xlsx" extension). If so, it processes the Excel
     * file and generates PDFs for each row. If the file is not an Excel file, it converts the uploaded
     * {@link MultipartFile} into a {@link File} and uploads it to a storage service.
     * </p>
     *
     * @param file the {@link MultipartFile} representing the uploaded file.
     * @return a {@link List} of results:
     *         <ul>
     *           <li>For an Excel file: a list of unique identifiers of the uploaded PDF files.</li>
     *           <li>For non-Excel files: a list containing the result of the file upload process.</li>
     *         </ul>
     * @throws FileException if an error occurs during the file processing or upload.
     */
    public List<String> uploadFile(MultipartFile file) {
        return "xlsx".equalsIgnoreCase(getFileExtension(file.getOriginalFilename()))
                ? processExcelFile(file)
                : CompletableFuture.supplyAsync(() -> {
            File convertedFile = convertMultipartToFile(file);
            String result = processUpload(convertedFile);
            return of(result);
        }, ioExecutor).join();
    }

    /**
     * Processes an Excel file, reads its rows asynchronously, generates PDFs for each row,
     * and uploads them to a storage service.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Opens the Excel file as a workbook.</li>
     *   <li>Reads the first sheet and processes each row starting from the second row (skipping the header).</li>
     *   <li>For each row, creates an asynchronous task to process the row and generate a PDF.</li>
     *   <li>Waits for all asynchronous tasks to complete and collects the results.</li>
     * </ol>
     * </p>
     *
     * @param file the {@link MultipartFile} representing the uploaded Excel file.
     * @return a {@link List} of unique identifiers of the uploaded PDF files.
     * @throws FileException if an error occurs while processing the Excel file.
     */
    private List<String> processExcelFile(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            List<CompletableFuture<String>> futures = stream(sheet.spliterator(), true)
                    .skip(1) // Skip header row
                    .map(row -> CompletableFuture.supplyAsync(() ->
                            processRowAndGeneratePdf(row,headerRow), cpuExecutor))
                    .toList();

            // Wait for all futures to complete and collect the results
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(response -> futures.stream()
                            .map(CompletableFuture::join)
                            .toList()) // Collect the results into a list
                    .join(); // Block and return the result
        } catch (IOException e) {
            throw new FileException("Error processing file", e);
        }
    }

    /**
     * Processes a row from an Excel sheet, creates a {@link User} object, generates an HTML representation,
     * converts it into a PDF, and uploads the PDF file.
     * <p>
     * The method performs the following steps:
     * <ol>
     *   <li>Extracts cell data from the provided row to build a {@link User} object.</li>
     *   <li>Generates an HTML representation of the user data using the {@code htmlGeneratorService}.</li>
     *   <li>Converts the generated HTML into a PDF file using the {@code pdfConverter}.</li>
     *   <li>Uploads the PDF file to a storage service using the {@code processUpload} method.</li>
     * </ol>
     * </p>
     *
     * @param row the {@link Row} object from the Excel sheet to be processed.
     * @return the unique identifier of the uploaded PDF file.
     * @throws FileException if an error occurs during PDF generation or file upload.
     */
     public String processRowAndGeneratePdf(Row row, Row headerRow) {

         Map<String, Integer> headerMapping = createHeaderMapping(headerRow);
         User user = User.builder().build();
         headerMapping.forEach((fieldName, columnIndex) -> {
             try {
                 Field field = User.class.getDeclaredField(fieldName);
                 field.setAccessible(true);
                 String cellValue = getCellValue(row, columnIndex);
                 if (field.getType().equals(double.class)) {
                     field.set(user,parseDouble(cellValue));
                 } else {
                     field.set(user,cellValue);
                 }
             } catch (NoSuchFieldException | IllegalAccessException e) {
                 throw new FileException("Error mapping field: " + fieldName, e);
             }
         });

         // Validate the user object
         Set<ConstraintViolation<User>> violations = validator.validate(user);
         if (!violations.isEmpty()) {
             // Handle validation errors: return the error messages or skip the row
             String errorMessages = violations.stream()
                     .map(ConstraintViolation::getMessage)
                     .collect(joining(" "));
             // Log errors or collect them for further processing
             return "Validation failed for user: " + user.getFirstName() + ". Errors: " + errorMessages;
         }

         // If validation passes, proceed with generating HTML and PDF
         String html = htmlGeneratorService.generateHtml(user);
         String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
         StringBuilder fileNameBuilder = new StringBuilder()
                 .append(user.getFirstName()).append("_").append(timestamp).append(".pdf");

         try {
             File pdfFile = pdfConverter.convertHtmlToPdf(html, fileNameBuilder.toString());
             return processUpload(pdfFile);
         } catch (IOException e) {
             throw new FileException("Error generating PDF file for user: " + user.getFirstName(), e);
         }
     }

    private Map<String, Integer> createHeaderMapping(Row headerRow) {
        return range(0, headerRow.getLastCellNum())
                .boxed()
                .collect(toMap(i -> Optional.ofNullable(headerRow.getCell(i))
                                .map(Cell::toString).map(String::trim)
                                .orElseThrow(() -> new RuntimeException("Header cell is empty at index: " + i)), i -> i));
    }

    /**
     * Processes the upload of a file by saving its metadata and uploading it to an S3 bucket.
     * This method performs the following steps:

     * 1)Determines the target folder for the file based on its name.
     * 2)Generates a unique identifier for the file and saves its metadata asynchronously.
     * 3)Uploads the file to an S3 bucket asynchronously.
     * The method waits for both the metadata saving and the file upload to complete and returns the
     * unique identifier of the saved document.
     *
     * @param file the {@link File} to be processed and uploaded.
     * @return the unique identifier of the uploaded document.
     * @throws FileException if an error occurs during the upload process or metadata saving.
     */
    public String processUpload(File file) {
        String fileTypeFolder = determineFolder(file.getName());
        String path = fileTypeFolder + "/" + file.getName();
        String uniqueId = randomUUID().toString();

        CompletableFuture<String> storedIdFuture = CompletableFuture.supplyAsync(() -> service.saveDocument(
                Document.builder().id(uniqueId).fileName(path)
                        .createdDate(now().toString()).updatedDate(now().toString()).build()), ioExecutor);

        CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
            try {
                log.info("Uploading file {} to bucket {}, key: {}", file.getName(), bucketName, path);

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName).key(path).build();

                s3Client.putObject(putObjectRequest, RequestBody.fromFile(file.toPath()));
            } catch (S3Exception e) {
                log.error("Error uploading file to S3: {}", e.awsErrorDetails().errorMessage());
                throw new FileException("S3 file upload failed: " + e.awsErrorDetails().errorMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error uploading file to S3", e);
                throw new FileException("File upload failed", e);
            }
        }, ioExecutor);

        storedIdFuture.join();
        uploadFuture.join();

        return file.getName() + " uploaded successfully with id: " + storedIdFuture.join();
    }

    /**
     * Retrieves the value of a specified cell in a row as a string.
     * This method extracts the value of a cell and converts it to its string representation.
     * If the cell is null, an empty string is returned.
     * @param row the {@link Row} containing the cell.
     * @param cellIndex the index of the cell within the row.
     * @return the string representation of the cell's value, or an empty string if the cell is null.
     */
    public String getCellValue(Row row, int cellIndex) {
        return ofNullable(row.getCell(cellIndex))
                .map(Cell::toString)
                .orElse("");
    }

    /**
     * Converts a {@link MultipartFile} to a {@link File}.
     * <p>
     * This method takes a MultipartFile, writes its contents to a temporary File on the local filesystem,
     * and returns the resulting File object.
     * </p>
     * @param file the {@link MultipartFile} to be converted.
     * @return the converted {@link File}.
     * @throws FileException if an I/O error occurs during the conversion.
     */
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

    /**
     * Updates an existing file in the S3 bucket and its metadata in the database.

     * This method replaces the existing file with a new one. It first retrieves
     * the metadata of the current file from the database. The old file is then
     * removed from the S3 bucket, and the new file is uploaded to a new location.
     * The metadata in the database is updated to reflect the new file path and
     * the updated timestamp.
     *
     * @param fileId the ID of the file to be updated
     * @param newFile the new file to replace the existing one
     * @return the ID of the updated file
     * @throws FileException if there is an error during the update process
     */
    public String updateFile(String fileId, MultipartFile newFile) {
        // Fetch the existing document metadata from the database.
        Document existingDocument = service.findDocumentById(fileId);

        String oldPath = existingDocument.getFileName();
        String newPath = determineFolder(newFile.getOriginalFilename()) + "/" + newFile.getOriginalFilename();

        try {
            log.info("Updating file {} in bucket {}, old key: {}, new key: {}",
                    newFile.getOriginalFilename(), bucketName, oldPath, newPath);

            // Remove the old file from the S3 bucket.
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(oldPath)
                    .build());

            // Upload the new file to the S3 bucket.
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(newPath)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(newFile.getInputStream(), newFile.getSize()));

            // Update metadata in the database.
            existingDocument.setFileName(newPath);
            existingDocument.setUpdatedDate(now().toString());
            service.updateDocument(existingDocument);

            log.info("File updated successfully. ID: {}", fileId);
        } catch (S3Exception e) {
            log.error("Error updating file in S3: {}", e.awsErrorDetails().errorMessage());
            throw new FileException("S3 file update failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error updating file in S3", e);
            throw new FileException("File update failed", e);
        }

        return fileId;
    }

    /**
     * Determines the folder name based on the file extension of the given file name.
     *
     * @param fileName the name of the file whose folder type needs to be determined.
     *
     * @return the folder name as a {@link String}, corresponding to the file's extension:
     *         - "pdf" files are mapped to the "pdfs" folder.
     *         - "doc" and "docx" files are mapped to the "docs" folder.
     *         - "jpg", "jpeg", and "png" files are mapped to the "images" folder.
     *         - "txt" files are mapped to the "texts" folder.
     *         - Any other extensions are mapped to the "others" folder.
     *
     * @implNote This method uses the Java `switch` expression introduced in Java 14,
     *           and it is case-insensitive as file extensions are converted to lowercase.
     *
     */
    public String determineFolder(String fileName) {

        String fileExtension = getFileExtension(fileName);
        return switch (fileExtension.toLowerCase()) {
            case "pdf" -> "pdfs";
            case "doc", "docx" -> "docs";
            case "jpg", "jpeg", "png" -> "images";
            case "txt" -> "texts";
            default -> "others";
        };
    }

    /**
     * Extracts the file extension from the given file name.
     *
     * @param fileName the name of the file from which the extension is to be extracted.
     *
     * @return the file extension as a {@link String}, excluding the period `.`.
     *
     * @throws FileException if the file name is null, blank, or does not contain a valid extension (i.e., no period `.`).

     * Example Usage:
     * - For fileName "document.pdf", this method returns "pdf".
     * - For fileName "image.jpeg", this method returns "jpeg".

     * Detailed Example:
     * Given the file name `"document.pdf"`:
     * 1. `fileName.lastIndexOf('.')` returns `8`, which is the position of the last period (`.`) in `"document.pdf"`.
     * 2. Adding `+1` to `8` results in `9`, which is the position of the first character of the extension (`p` in `"pdf"`).
     * 3. `fileName.substring(9)` extracts the substring starting at index `9`, which gives `"pdf"`.
     *
     * The result of this operation is `"pdf"`, which is the file extension without the period.
     *
     */
    private String getFileExtension(String fileName) {
        if (isBlank(fileName) || !fileName.contains(".")) {
            throw new FileException("Invalid file name: " + fileName);
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    /**
     * Downloads a file from an S3 bucket based on the provided document ID.
     *
     * <p>Workflow:
     * <ul>
     *   <li>Retrieve the document associated with the given ID from the database.</li>
     *   <li>Verify the document exists. If not, throw an exception.</li>
     *   <li>Extract the file name from the document.</li>
     *   <li>Create an S3 get request using the file name and bucket name.</li>
     *   <li>Send the request to the S3 client to fetch the file as an input stream.</li>
     *   <li>Read the input stream and write its contents into a byte array output stream.</li>
     *   <li>Log a success message and return the byte array representing the file's content.</li>
     * </ul>
     *
     * @param id the unique identifier of the file to be downloaded
     * @return a byte array containing the file's content
     * @throws FileException if the document is not found, the file is missing in S3,
     *                       or if any error occurs during the download process.

     * - The inputStream.read(buffer) method reads up to buffer.length (1024) bytes from the input stream,
     *   storing the data in the buffer array and returning the number of bytes read.
     * - If the buffer isn't fully filled (e.g., during the last read operation), bytesRead ensures only the
     *   valid portion of the buffer is written to the output stream.
     * - This approach efficiently handles large files by processing them in chunks instead of loading the
     *   entire file into memory, reducing memory usage and enhancing performance.
     */
    public byte[] downloadFile(String id){

        // Retrieve the document using the given ID from the database.
        Document document = service.findDocumentById(id);
        if (isNull(document)) {
            log.warn("No document found with id: {}", id);
            throw new FileException("Document with ID " + id + " not found");
        }

        // Extract the file name from the retrieved document.
        String fileName = document.getFileName();

        // Build a request object to fetch the file from the S3 bucket.
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName).key(fileName).build();

        try {
            // Send the request to S3 and get the file as an input stream.
            ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(getObjectRequest);

            // Create an output stream to store the file's contents as bytes.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];

            int bytesRead; // Declare a variable to store the number of bytes read in each iteration of the loop.
            // Read data from the input stream into the buffer in chunks.
            // Continue looping until the end of the input stream is reached (indicated by read() returning -1).
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Write the data from the buffer to the output stream.
                outputStream.write(buffer, 0, bytesRead); //buffer contains the bytes read from the inputStream, starting at index 0.
            }

            log.info("File with id {} (name: {}) downloaded successfully from bucket {}", id, fileName, bucketName);
            return outputStream.toByteArray();

        } catch (NoSuchKeyException noSuchKeyEx) {
            log.warn("File not found in S3 bucket {} for id {} (name: {})", bucketName, id, fileName);
            throw new FileException("File not found in bucket: " + fileName, noSuchKeyEx);

        } catch (SdkClientException sdkEx) {
            log.error("AWS SDK error while downloading file with id {} (name: {}) from bucket {}: {}",
                    id, fileName, bucketName, sdkEx.getMessage(), sdkEx);
            throw new FileException("AWS SDK error during file download", sdkEx);

        } catch (Exception ex) {
            log.error("Unexpected error while downloading file with id {} (name: {}) from bucket {}: {}",
                    id, fileName, bucketName, ex.getMessage(), ex);
            throw new FileException("Unexpected error during file download", ex);
        }
    }

    /**
     * Deletes a file from an S3 bucket and removes its associated record from the database.
     *
     * @param id the unique identifier of the file to be deleted
     * @return a message confirming the successful deletion of the file
     *          (e.g., "example-file.txt removed successfully.")
     * @throws IllegalArgumentException if the file ID is null or empty
     * @throws FileException if the file is not found, the file name is blank,
     *                       or if an error occurs during file deletion
     */
    public String deleteFile(String id){

        if (isBlank(id)) {
            log.warn("File ID is null or empty");
            throw new IllegalArgumentException("File ID cannot be null or empty.");
        }

            var document = service.findDocumentById(id);
            if (isNull(document)) {
                log.warn("No document found for ID: {}", id);
                throw new FileException("No file found for the given ID: " + id);
            }

        String fileName = document.getFileName();
            if (isBlank(fileName)) {
                log.warn("File name is blank for document with ID: {}", id);
                throw new FileException("File name not found for the given ID: " + id);
            }
        try{
             DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName).key(fileName).build();

             s3Client.deleteObject(deleteObjectRequest);
             log.info("File '{}' deleted successfully from bucket '{}'", fileName, bucketName);
        } catch (SdkClientException sdkEx) {
            log.error("AWS SDK error deleting file '{}' from bucket '{}'", fileName, bucketName, sdkEx);
            throw new FileException("AWS SDK error during file deletion for " + fileName, sdkEx);
        } catch (Exception e) {
            log.error("Unexpected error deleting file '{}' from bucket '{}'", fileName, bucketName, e);
            throw new FileException("File deletion failed for " + fileName, e);
        }

        service.deleteFile(id);

        return fileName + " removed successfully.";
    }

    /**
     * This method interacts with AWS S3 using the {@link S3Client} to retrieve the list of objects (files)
     * in the specified bucket and fetches their corresponding IDs from the database.
     *
     * @return a {@link List} of {@link String} where each entry represents a file name and its associated ID
     * in the format "fileName:id". If an ID is not found for a file, the value will be null.
     *
     * @throws FileException if any error occurs while listing files, such as network issues,
     * S3 service errors, or database access problems.
     * Example:
     * For a bucket with the following files:
     * - `document.pdf`
     * - `image.jpg`
     * - `notes.txt`
     * And corresponding database entries:
     * - `document.pdf` → `ID123`
     * - `image.jpg` → `ID456`
     * - `notes.txt` → Not found
     * The returned list might look like:
     * - `["document.pdf:ID123", "image.jpg:ID456", "notes.txt:null"]`
     * Implementation Notes:
     * - Uses {@link ListObjectsV2Request} to request a list of objects from S3.
     * - Extracts the file names (keys) of the objects in the bucket using a stream.
     * - Maps each file name to its corresponding ID retrieved using the database service.
     * - The result is a list of strings combining file names and IDs.
     */

    public List<String> listFiles() {
        try {
            // Fetch the list of objects from the S3 bucket
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName).build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            log.debug("S3 Response: {}", response);

            // Extract the file names from the S3 response
            List<String> fileNames = response.contents().stream()
                    .map(S3Object::key)
                    .toList();

            // Create a list of strings in the format "fileName:id"
            List<String> fileInfoList = fileNames.stream()
                    .map(fileName -> {
                        String fileId = service.findIdByFileName(fileName); // Fetch ID
                        return fileName + ":" + fileId;
                    })
                    .toList();

            log.info("Listed {} files with IDs in bucket {}", fileInfoList.size(), bucketName);
            return fileInfoList;
        } catch (S3Exception e) {
            log.error("Error listing files in bucket {}", bucketName, e);
            throw new FileException("Failed to list files in bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error while listing files in bucket {}", bucketName, e);
            throw new FileException("Unexpected error while listing files in bucket: " + bucketName, e);
        }
    }
}
