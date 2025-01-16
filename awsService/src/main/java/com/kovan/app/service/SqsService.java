package com.kovan.app.service;

import com.kovan.app.exception.SqsServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class SqsService {

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

        private final SqsAsyncClient sqsAsyncClient;

    public SqsService(SqsAsyncClient sqsAsyncClient) {
        this.sqsAsyncClient = sqsAsyncClient;
    }

    public CompletableFuture<String> createQueue(String queueName) {
        try {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName).build();

            return sqsAsyncClient.createQueue(createQueueRequest)
                    .thenApply(response -> {
                        log.info("Queue created successfully. Queue URL: {}", response.queueUrl());
                        return response.queueUrl();
                    });
        } catch (SqsException e) {
                throw new SqsServiceException("Failed to create queue", e);
        }
    }

    public CompletableFuture<Void> deleteQueue(String queueUrl) {
        try {
            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl).build();

            return sqsAsyncClient.deleteQueue(deleteQueueRequest)
                    .thenRun(() -> log.info("Queue deleted successfully: {}", queueUrl));
        } catch (SqsException e) {
            throw new SqsServiceException("Failed to delete queue", e);
        }
    }

    public CompletableFuture<ListQueuesResponse> listQueues() {
        try {
            return sqsAsyncClient.listQueues()
                    .thenApply(response -> {
                        log.info("Retrieved {} queues.", response.queueUrls().size());
                        return response;
                    });
        } catch (SqsException e) {
            throw new SqsServiceException("Failed to list queues", e);
        }
    }
}
