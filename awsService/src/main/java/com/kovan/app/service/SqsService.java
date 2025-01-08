package com.kovan.app.service;

import com.kovan.app.exception.SqsServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.List;
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

    public CompletableFuture<Void> sendMessage(String messageBody) {
        try {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            return sqsAsyncClient.sendMessage(sendMessageRequest)
                    .thenAccept(response -> log.info("Message sent successfully. Message ID: {}", response.messageId()))
                    .thenCompose(result -> {
                        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .maxNumberOfMessages(10)
                                .build();

                        return sqsAsyncClient.receiveMessage(receiveMessageRequest)
                                .thenAccept(receiveResponse -> {
                                    List<Message> messages = receiveResponse.messages();
                                    log.info("Received {} messages.", messages.size());
                                    messages.forEach(message -> log.info("Message body: {}", message.body()));
                                });
                    });
        } catch (SqsException e) {
            throw new SqsServiceException("Failed to send and retrieve messages", e);
        }
    }

    private CompletableFuture<Void> deleteMessage(String receiptHandle) {
              try {
              DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();

              return sqsAsyncClient.deleteMessage(deleteMessageRequest)
                    .thenRun(() -> log.info("Message deleted successfully. Receipt Handle: {}", receiptHandle));
              } catch (SqsException e) {
                  throw new SqsServiceException("Failed to delete message", e);
        }
    }

        public CompletableFuture<String> createQueue(String queueName) {
            try {
                CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                        .queueName(queueName)
                        .build();

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
                        .queueUrl(queueUrl)
                        .build();

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
