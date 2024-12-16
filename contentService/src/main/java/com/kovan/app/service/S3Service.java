package com.kovan.app.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.kovan.api.model.TestRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class S3Service {

    private final AmazonS3 s3Client;
    private final TestService service;

    public S3Service(AmazonS3 s3Client, TestService service) {
        this.s3Client = s3Client;
        this.service = service;
    }

    public void uploadFile(String bucketName, MultipartFile file) throws AmazonClientException, SdkClientException,IOException {
        ObjectMetadata metaData = new ObjectMetadata();
        service.addOrUpdateData(TestRequest.builder()
                .fileName(file.getOriginalFilename()).build());
        s3Client.putObject(bucketName,file.getOriginalFilename(),file.getInputStream(),metaData);

    }

    public Resource downloadFile(String bucketName,Long id) throws IOException{

        S3Object s3Object = s3Client.getObject(bucketName,service.getById(id).getFileName());
        var bytes = s3Object.getObjectContent().readAllBytes();
        return new ByteArrayResource(bytes);
    }
}
