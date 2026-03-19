package com.kovan.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.exception.SqsServiceException;
import com.kovan.app.util.MyMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SqsPublisher {

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    public SqsPublisher(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a message to the SQS queue.
     *
     * @param message The message to send.
     * @return A CompletableFuture that completes when the message has been sent.
     * @throws SqsServiceException If the message cannot be serialized to JSON or if sending the message fails.
     */
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
        } catch (SqsException e) {
            throw new SqsServiceException("Failed to send message", e);
        }
    }
}
