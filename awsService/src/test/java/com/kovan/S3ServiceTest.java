package com.kovan;

import com.kovan.app.service.S3Service;
import com.kovan.entity.Document;
import com.kovan.app.exception.FileException;
import com.kovan.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private DocumentService documentService;

    @Value("${bucketName}")
    private String bucketName;

    @InjectMocks
    private S3Service s3Service;

    @Test
    void testCreateBucket() {

        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();

        when(s3Client.createBucket((createBucketRequest))).thenReturn(null);

        String result = s3Service.createBucket(bucketName);
        verify(s3Client, times(1)).createBucket(createBucketRequest);
        assertNotNull(result, "Result should not be null");
        assertEquals("Bucket created successfully: " + bucketName, result, "Expected success message");

    }

    @Test
    void testCreateBucket_S3Exception_ThrowsError() {

        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error occurred").build());

        String result = s3Service.createBucket(bucketName);

        assertNotNull(result);
        assertTrue(result.contains("Error creating bucket: S3 error occurred"));
        verify(s3Client, times(1)).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void testDeleteBucket() {

        s3Service.createBucket(bucketName);

        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Iterable mockPaginator = mock(ListObjectsV2Iterable.class);
        when(s3Client.listObjectsV2Paginator(listObjectsV2Request)).thenReturn(mockPaginator);

        ListObjectVersionsIterable mockVersionPaginator = mock(ListObjectVersionsIterable.class);
        when(s3Client.listObjectVersionsPaginator(any(ListObjectVersionsRequest.class)))
                .thenReturn(mockVersionPaginator);

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(bucketName)
                .build();
        when(s3Client.deleteBucket(deleteBucketRequest)).thenReturn(null);

        String result = s3Service.deleteBucket(bucketName);

        verify(s3Client, times(1)).deleteBucket(deleteBucketRequest);
        assertEquals("Bucket deleted successfully: " + bucketName, result, "Expected success message");
    }

    @Test
    void testDeleteBucket_S3Exception_ThrowsError() {

        ListObjectsV2Iterable mockPaginator = mock(ListObjectsV2Iterable.class);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(mockPaginator);

        ListObjectVersionsIterable mockVersionPaginator = mock(ListObjectVersionsIterable.class);
        when(s3Client.listObjectVersionsPaginator(any(ListObjectVersionsRequest.class))).thenReturn(mockVersionPaginator);

        doThrow(S3Exception.builder().message("S3 error occurred").build())
                .when(s3Client).deleteBucket(any(DeleteBucketRequest.class));

        String result = s3Service.deleteBucket(bucketName);

        assertNotNull(result);
        assertTrue(result.contains("Error deleting bucket: S3 error occurred"));
        verify(s3Client, times(1)).deleteBucket(any(DeleteBucketRequest.class));
        verify(s3Client, times(1)).listObjectsV2Paginator(any(ListObjectsV2Request.class));
        verify(s3Client, times(1)).listObjectVersionsPaginator(any(ListObjectVersionsRequest.class));
    }

    @Test
    void testDeleteAllObjects() {

        String testBucketName = "test-bucket";

        S3Object mockObject1 = S3Object.builder().key("object1").build();
        S3Object mockObject2 = S3Object.builder().key("object2").build();
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(Arrays.asList(mockObject1, mockObject2))
                .build();

        ListObjectsV2Iterable mockPaginator = mock(ListObjectsV2Iterable.class);
        when(mockPaginator.stream()).thenReturn(Stream.of(mockResponse));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(mockPaginator);

        ObjectVersion mockVersion1 = ObjectVersion.builder().key("object1").versionId("v1").build();
        ObjectVersion mockVersion2 = ObjectVersion.builder().key("object2").versionId("v2").build();
        ListObjectVersionsResponse mockVersionResponse = ListObjectVersionsResponse.builder()
                .versions(Arrays.asList(mockVersion1, mockVersion2))
                .build();

        ListObjectVersionsIterable mockVersionPaginator = mock(ListObjectVersionsIterable.class);
        when(mockVersionPaginator.stream()).thenReturn(Stream.of(mockVersionResponse));
        when(s3Client.listObjectVersionsPaginator(any(ListObjectVersionsRequest.class))).thenReturn(mockVersionPaginator);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);
        s3Service.deleteAllObjects(testBucketName);

        verify(s3Client, times(1)).listObjectsV2Paginator(any(ListObjectsV2Request.class));
        verify(s3Client, times(1)).listObjectVersionsPaginator(any(ListObjectVersionsRequest.class));

        verify(s3Client).deleteObject(argThat((DeleteObjectRequest request) ->
                testBucketName.equals(request.bucket()) && "object1".equals(request.key()) && "v1".equals(request.versionId())));
        verify(s3Client).deleteObject(argThat((DeleteObjectRequest request) ->
                testBucketName.equals(request.bucket()) && "object2".equals(request.key()) && "v2".equals(request.versionId())));

    }

    @Test
    void testRenameBucket() {

        String oldBucketName = "old-bucket";
        String newBucketName = "new-bucket";

        s3Service.createBucket(oldBucketName);

        ListObjectsV2Iterable mockPaginator = mock(ListObjectsV2Iterable.class);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(mockPaginator);

        ObjectVersion mockVersion1 = ObjectVersion.builder().key("file1.txt").build();
        ObjectVersion mockVersion2 = ObjectVersion.builder().key("file2.txt").build();
        List<ObjectVersion> mockVersions = Arrays.asList(mockVersion1, mockVersion2);

        ListObjectVersionsResponse mockVersionResponse = ListObjectVersionsResponse.builder()
                .versions(mockVersions).build();

        ListObjectVersionsIterable mockVersionPaginator = mock(ListObjectVersionsIterable.class);
        when(mockVersionPaginator.stream()).thenAnswer(invocation -> Stream.of(mockVersionResponse));
        when(s3Client.listObjectVersionsPaginator(any(ListObjectVersionsRequest.class))).thenReturn(mockVersionPaginator);

        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(null);

        String result = s3Service.renameBucket(oldBucketName, newBucketName);

        verify(s3Client, times(1)).deleteBucket(any(DeleteBucketRequest.class));
        assertEquals("Bucket renamed successfully from " + oldBucketName + " to " + newBucketName,
                result, "Expected success message");

    }

    @Test
    void testRenameBucket_S3ExceptionDuringObjectCopy() {

        String oldBucketName = "old-bucket";
        String newBucketName = "new-bucket";
        String objectKey = "test-object";
        String expectedErrorMessage = "Failed to copy object: " + objectKey;

        ListObjectsV2Iterable mockPaginator = mock(ListObjectsV2Iterable.class);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(mockPaginator);

        S3Object mockObject = S3Object.builder().key(objectKey).build();
        when(mockPaginator.stream()).thenReturn(Stream.of(ListObjectsV2Response.builder().contents(List.of(mockObject)).build()));

        doThrow(new FileException(expectedErrorMessage)).when(s3Client).copyObject(any(CopyObjectRequest.class));
        String result = s3Service.renameBucket(oldBucketName, newBucketName);
        assertTrue(result.contains(expectedErrorMessage),
                "Result should contain the expected error message. Actual: " + result);
        verify(s3Client).listObjectsV2Paginator(any(ListObjectsV2Request.class));
        verify(s3Client, never()).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    void testUploadFile() throws Exception {

        String fileName = "test.pdf";
        String filePath = "pdfs/" + fileName;

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(mockFile.getSize()).thenReturn(11L);

        PutObjectRequest expectedRequest = PutObjectRequest.builder().bucket(bucketName)
                .key(filePath).build();

        when(s3Client.putObject(eq(expectedRequest), any(RequestBody.class))).thenReturn(null);

        when(documentService.saveDocument(any(Document.class))).thenReturn(null);

        String result = s3Service.uploadFile(mockFile);

        assertNotNull(result);

    }

    @Test
    void testUploadFile_InvalidFileName_ThrowsException() {

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("invalidfile");

        FileException exception = assertThrows(FileException.class, () -> s3Service.uploadFile(mockFile));

        assertEquals("Invalid file name: invalidfile", exception.getMessage());
    }

    @Test
    void testUploadFile_S3UploadFails_ThrowsException() throws Exception {

        String fileName = "test.pdf";
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(mockFile.getSize()).thenReturn(11L);

        doThrow(SdkException.builder().message("S3 upload failed").build())
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        FileException exception = assertThrows(FileException.class, () -> s3Service.uploadFile(mockFile));

        assertEquals("File upload failed", exception.getMessage());
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testDownloadFile_Success() {

        String fileId = "test-id";
        String fileName = "test.pdf";
        byte[] fileContent = "Test content".getBytes();

        Document mockDocument = Document.builder()
                .id(fileId)
                .fileName(fileName)
                .build();

        when(documentService.findDocumentById(fileId)).thenReturn(mockDocument);

        GetObjectRequest expectedRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        ResponseInputStream<GetObjectResponse> mockInputStream = new ResponseInputStream<>(
                mock(GetObjectResponse.class),new ByteArrayInputStream(fileContent));

        when(s3Client.getObject((expectedRequest))).thenReturn(mockInputStream);

        byte[] result = s3Service.downloadFile(fileId);

        assertArrayEquals(fileContent, result, "Expected file content to match");

        verify(documentService).findDocumentById(fileId);
        verify(s3Client).getObject((expectedRequest));
    }

    @Test
    void testDownloadFile_FileNotFound_ThrowsException() {

        String fileId = "invalid-id";
        when(documentService.findDocumentById(fileId)).thenThrow(new FileException("Document not found"));

        FileException exception = assertThrows(FileException.class, () -> s3Service.downloadFile(fileId));

        assertEquals("Failed to retrieve document metadata", exception.getMessage());
        verify(documentService).findDocumentById(fileId);
        verifyNoInteractions(s3Client);
    }

    @Test
    void testDownloadFile_IOError_ThrowsException() throws IOException {

        String fileId = "test-id";
        String fileName = "test.pdf";

        Document mockDocument = Document.builder()
                .id(fileId)
                .fileName(fileName)
                .build();

        when(documentService.findDocumentById(fileId)).thenReturn(mockDocument);

        GetObjectRequest expectedRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        ResponseInputStream<GetObjectResponse> mockInputStream = mock(ResponseInputStream.class);

        when(s3Client.getObject((expectedRequest))).thenReturn(mockInputStream);
        when(mockInputStream.read(any(byte[].class))).thenThrow(new IOException("Read error"));

        FileException exception = assertThrows(FileException.class, () -> s3Service.downloadFile(fileId));

        assertEquals("File download failed due to I/O error", exception.getMessage());
        verify(documentService).findDocumentById(fileId);
        verify(s3Client).getObject((expectedRequest));
    }

    @Test
    void testDeleteFile() {
        String id = "test-id";
        String fileName = "pdfs/test.pdf";
        when(documentService.findDocumentById(id)).thenReturn(Document.builder().fileName(fileName).build());
        String result = s3Service.deleteFile(id);
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        verify(documentService, times(1)).deleteFile(id);
        assertEquals(fileName + " removed successfully.", result, "Expected success message");
    }

    @Test
    void testDeleteFile_FileNotFoundInDatabase_ThrowsException() {
        String fileId = "nonexistent-id";

        when(documentService.findDocumentById(fileId))
                .thenThrow(new FileException("File not found in database"));

        FileException exception = assertThrows(FileException.class, () -> s3Service.deleteFile(fileId));

        assertEquals("Failed to retrieve file details for deletion.", exception.getMessage());
        verify(documentService).findDocumentById(fileId);
        verifyNoInteractions(s3Client);
    }

    @Test
    void testDeleteFile_S3DeletionFails_ThrowsException() {
        String fileId = "test-id";
        String fileName = "pdfs/test.pdf";

        when(documentService.findDocumentById(fileId))
                .thenReturn(Document.builder().fileName(fileName).build());

        doThrow(SdkException.builder().message("S3 deletion failed").build())
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        FileException exception = assertThrows(FileException.class, () -> s3Service.deleteFile(fileId));

        assertEquals("File deletion failed for pdfs/test.pdf", exception.getMessage());
        verify(documentService).findDocumentById(fileId);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteFile_IdIsBlank_ThrowsIllegalArgumentException() {
        String blankId = "";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> s3Service.deleteFile(blankId));

        assertEquals("File ID cannot be null or empty.", exception.getMessage());
    }

    @Test
    void testDeleteFile_FileNameIsBlank_ThrowsFileException() {
        String id = "test-id";

        when(documentService.findDocumentById(id)).thenReturn(Document.builder().fileName("").build());

        FileException exception = assertThrows(FileException.class, () -> s3Service.deleteFile(id));

        assertEquals("Failed to retrieve file details for deletion.", exception.getMessage());
        verify(documentService).findDocumentById(id);
        verifyNoInteractions(s3Client);
    }

    @Test
    void testListFiles() {
        List<S3Object> mockObjects = List.of( S3Object.builder().key("file1.txt").build(),
                S3Object.builder().key("file2.txt").build() );

        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder().contents(mockObjects).build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);

        List<String> result = s3Service.listFiles();
        assertEquals(2, result.size(), "Expected 2 files in the result");
        assertTrue(result.contains("file1.txt"), "Expected file1.txt in the result");
        assertTrue(result.contains("file2.txt"), "Expected file2.txt in the result");
    }

    @Test
    void testListFiles_Exception() {

        String expectedErrorMessage = "Simulated exception";
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message(expectedErrorMessage).build());

        FileException exception = assertThrows(FileException.class, () -> s3Service.listFiles());

        assertEquals("Failed to list files in bucket: " + bucketName, exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(expectedErrorMessage, exception.getCause().getMessage());

        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void testDetermineFolder() {

        assertEquals("pdfs", s3Service.determineFolder("file.pdf"), "Should return 'pdfs' for PDF files");
        assertEquals("docs", s3Service.determineFolder("document.doc"), "Should return 'docs' for DOC files");
        assertEquals("docs", s3Service.determineFolder("presentation.docx"), "Should return 'docs' for DOCX files");
        assertEquals("images", s3Service.determineFolder("picture.jpg"), "Should return 'images' for JPG files");
        assertEquals("images", s3Service.determineFolder("photo.jpeg"), "Should return 'images' for JPEG files");
        assertEquals("images", s3Service.determineFolder("graphic.png"), "Should return 'images' for PNG files");
        assertEquals("texts", s3Service.determineFolder("notes.txt"), "Should return 'texts' for TXT files");
        assertEquals("others", s3Service.determineFolder("archive.zip"), "Should return 'others' for unknown file types");
    }

}