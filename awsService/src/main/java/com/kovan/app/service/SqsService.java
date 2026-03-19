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

    /**
     * Creates a new SQS queue with the given name.
     *
     * @param queueName The name of the queue to create.
     * @return A CompletableFuture that resolves to the URL of the newly created queue.
     * @throws SqsServiceException if queue creation fails.
     */
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

    /**
     * Deletes the SQS queue at the given URL.
     *
     * @param queueUrl The URL of the queue to delete.
     * @return A CompletableFuture that completes when the queue is successfully deleted.
     * @throws SqsServiceException if queue deletion fails.
     */
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

    /**
     * Lists all SQS queues.
     *
     * @return A CompletableFuture that resolves to a ListQueuesResponse containing the list of queues.
     * @throws SqsServiceException if listing queues fails.
     */
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
