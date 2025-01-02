package com.kovan.app.service;

import com.kovan.app.exception.SqsServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class SqsService {

    private final SqsClient sqsClient;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public SqsService(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    public String sendMessage(String message) {
        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .delaySeconds(0)
                    .build();

            log.info("Message sent to SQS: {}", message);
            sqsClient.sendMessage(request);
            return "Message sent to SQS: " + message;
        } catch (SqsException e) {
            log.error("Error sending message to SQS: {}", e.getMessage(), e);
            throw new SqsServiceException("Failed to send message to SQS", e);
        }
    }

    public String sendBatchMessages(List<String> messages) {

        try {
            for(String msg:messages) {
                SendMessageBatchRequest sendMessageBatchRequest = SendMessageBatchRequest.builder().queueUrl(queueUrl)
                        .entries(SendMessageBatchRequestEntry.builder()
                                .id(String.valueOf(msg.hashCode())).messageBody(msg).build()).build();
                sqsClient.sendMessageBatch(sendMessageBatchRequest);
            }
            return "Messages sent successfully";
        } catch (SqsException e) {
            log.error("Error sending batch messages to SQS: {}", e.getMessage(), e);
            throw new SqsServiceException("Failed to send batch messages to SQS", e);
        }
    }

    public List<String> receiveMessages() {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(10)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(request).messages();
            List<String> messageBodies = messages.stream()
                    .map(Message::body).toList();

            log.info("Received messages: {}", messageBodies);

            deleteMessage(messages);

            return messageBodies;

        } catch (SqsException e) {
            log.error("Error receiving messages from SQS: {}", e.getMessage(), e);
            throw new SqsServiceException("Failed to receive messages from SQS", e);
        }
    }

    public void deleteMessage(List<Message> messages) {
        try {
            for (Message message : messages) {
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();
                sqsClient.deleteMessage(deleteMessageRequest);
                log.info("Message deleted from SQS.");
            }
        }catch (SqsException e) {
            log.error("Error deleting message from SQS: {}", e.getMessage(), e);
            throw new SqsServiceException("Failed to delete message from SQS", e);
        }
    }

    public String createQueue(String queueName) {
        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();

            CreateQueueResponse response = sqsClient.createQueue(request);
            String createdQueueUrl = response.queueUrl();
            log.info("Queue created: {}", createdQueueUrl);
            return createdQueueUrl;
        } catch (SqsException e) {
            log.error("Error creating queue: {}", e.getMessage(), e);
            throw new SqsServiceException("Failed to create queue", e);
        }
    }

    public String deleteQueue(String queueName) {
        try {
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();

            String qUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
            DeleteQueueRequest request = DeleteQueueRequest.builder()
                    .queueUrl(qUrl)
                    .build();

            sqsClient.deleteQueue(request);
            log.info("Queue deleted: {}", qUrl);
            return "Queue deleted: "+ qUrl;
        } catch (SqsException e) {
            log.error("Error deleting queue: {}", e.getMessage(), e);
            throw new SqsServiceException("Failed to delete queue", e);
        }
    }

    public List<String> listQueues() {
        try {
            ListQueuesRequest request = ListQueuesRequest.builder().build();
            ListQueuesResponse response = sqsClient.listQueues(request);

            List<String> queueUrls = response.queueUrls();
            log.info("Queues listed: {}", queueUrls);
            return queueUrls;
        } catch (SqsException e) {
            log.error("Error listing queues: {}", e.getMessage(), e);
            throw new SqsServiceException("Failed to list queues", e);
        }
    }
}
