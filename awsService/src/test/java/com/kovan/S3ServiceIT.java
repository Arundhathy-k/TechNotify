package com.kovan;

import com.kovan.app.service.S3Service;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class S3ServiceIT {

    @Autowired
    private S3Service s3Service;

    @Value("${bucketName}")
    private String bucketName;

    @Test
    void testCreateBucket() {
        String result = s3Service.createBucket("arundhathytestbucket");
        assertThat(result).isEqualTo("Bucket created successfully: arundhathytestbucket");
        s3Service.deleteBucket("arundhathytestbucket");
    }

    @Test
    void testDeleteBucket() {
        s3Service.createBucket(bucketName);
        String result = s3Service.deleteBucket(bucketName);
        assertThat(result).isNotNull();
    }

    @Test
    void renameBucket(){
        s3Service.createBucket(bucketName);
        MockMultipartFile file = new MockMultipartFile("file", "test1.txt",
                "text/plain", "test content".getBytes());
        s3Service.uploadFile(file);
        String result = s3Service.renameBucket(bucketName,"newawsbucketname1");
        assertThat(result).isNotNull();
        s3Service.deleteBucket(bucketName);
    }

    @Test
    void testUploadFile() throws Exception {
        File excelFile = new File("test-file.xlsx");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Test Excel Data");

        try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
            workbook.write(fileOut);
        }
        workbook.close();
        MultipartFile multipartFile = new MockMultipartFile("file", "test-file.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new FileInputStream(excelFile));

        List<String> result = s3Service.uploadFile(multipartFile);
        assertNotNull(result);
    }

    @Test
    void testDownloadFile(){

        MockMultipartFile file = new MockMultipartFile("file", "test4.txt",
                "text/plain", "test content".getBytes());
        List<String> result =  s3Service.uploadFile(file);

        String fileId = extractId(result.getFirst());

        byte[] fileData = s3Service.downloadFile(fileId);

        assertNotNull(fileData);

    }

    @Test
    void testDeleteFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test4.txt",
                "text/plain", "test content".getBytes());
        List<String> response =  s3Service.uploadFile(file);
        String fileId = extractId(response.getFirst());
        String result = s3Service.deleteFile(fileId);
        assertThat(result).isNotNull();
    }

    @Test
    void testListFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test5.txt",
                "text/plain", "test content".getBytes());
        s3Service.uploadFile(file);
        List<String> fileNames = s3Service.listFiles();
        assertThat(fileNames).isNotEmpty();

    }

    @Test
    void testUpdateFile()  {
        MockMultipartFile initialFile = new MockMultipartFile("file", "initial.txt",
                "text/plain", "initial file content".getBytes());

        List<String> initialResponse = s3Service.uploadFile(initialFile);
        String fileId = extractId(initialResponse.getFirst());

        MockMultipartFile updatedFile = new MockMultipartFile("file", "updated.txt",
                "text/plain", "updated file content".getBytes());

        String updatedFileId = s3Service.updateFile(fileId, updatedFile);

        assertThat(updatedFileId).isEqualTo(fileId);

        String deleteResponse = s3Service.deleteFile(fileId);
        assertThat(deleteResponse).isNotNull();
    }

    public String extractId(String input){
        String[] parts = input.split("id: ");
                return parts[1].trim();
    }
}
