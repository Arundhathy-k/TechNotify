package com.kovan.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.exception.SqsServiceException;
import com.kovan.app.util.MyMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
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
        private final ObjectMapper objectMapper;

    public SqsService(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<Void> sendMessage(MyMessage message) {
        try {

            // Serialize the object to JSON string
            String messageBody = objectMapper.writeValueAsString(message);
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            return sqsAsyncClient.sendMessage(sendMessageRequest)
                    .thenAccept(response -> log.info("Message sent successfully. Message ID: {}", response.messageId()));
        } catch (JsonProcessingException e) {
            throw new SqsServiceException("Failed to serialize message to JSON", e);
        }
        catch (SqsException e) {
            throw new SqsServiceException("Failed to send message", e);
        }
    }

    @SqsListener("MessageQueue")
    public void receiveMessages(String messageBody) {
        try {
            // convert the JSON string to a Java object
            MyMessage message = objectMapper.readValue(messageBody, MyMessage.class);
            log.info("Received Message: {}", message);
        } catch (Exception e) {
            log.error("Error processing message: {}", messageBody, e);
            throw new SqsServiceException("Failed to receive messages", e);
        }
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
