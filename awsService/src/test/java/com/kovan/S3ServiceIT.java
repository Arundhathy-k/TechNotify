package com.kovan;

import com.kovan.app.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import org.springframework.mock.web.MockMultipartFile;
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
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "test content".getBytes());
        s3Service.uploadFile(file);
        String result = s3Service.renameBucket(bucketName,"newawsbucketname1");
        assertThat(result).isNotNull();
        s3Service.deleteBucket(bucketName);
    }

    @Test
    void testUploadFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "test content".getBytes());
        String result = s3Service.uploadFile(file);
        assertThat(result).isNotNull();
    }

    @Test
    void testDownloadFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "test content".getBytes());
        String fileId = s3Service.uploadFile(file);
        byte[] result = s3Service.downloadFile(fileId);
        assertThat(result).isNotEmpty();
    }

    @Test
    void testDeleteFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "test content".getBytes());
        String fileId =  s3Service.uploadFile(file);
        String result = s3Service.deleteFile(fileId);
        assertThat(result).isNotNull();
    }

    @Test
    void testListFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "test content".getBytes());
        s3Service.uploadFile(file);
        List<String> fileNames = s3Service.listFiles();
        assertThat(fileNames).isNotEmpty();
    }
}