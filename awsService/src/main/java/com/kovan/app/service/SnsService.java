package com.kovan.app.service;

import com.kovan.app.exception.SnsServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

@Service
@Slf4j
public class SnsService {

    @Value("${topicArn}")
    private String topicArn;

    private final SnsClient snsClient;

    public SnsService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    /**
     * Creates a new SNS topic with the specified name.
     * <p>
     * This method sends a request to Amazon SNS to create a topic with the given name.
     * If the topic is created successfully, the method returns the Amazon Resource Name (ARN)
     * of the newly created topic. If the operation fails, a {@link SnsServiceException} is thrown.
     *
     * @param topicName the name of the topic to create. Must not be {@code null} or empty.
     * @return a message containing the ARN of the created topic in the format:
     *         "Topic created with ARN: [topicArn]".
     * @throws SnsServiceException if the topic creation fails due to an SNS-related error.
     *                             The exception will contain the original {@link SnsException}.
     */
    public String createSNSTopic(String topicName) {
        try {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();

            CreateTopicResponse result = snsClient.createTopic(request);
            return "Topic created with ARN: " + result.topicArn();
        } catch (SnsException e) {
            throw new SnsServiceException("Failed to create snsTopic", e);
        }
    }

    /**
     * Deletes an SNS topic with the specified Amazon Resource Name (ARN).
     * <p>
     * This method sends a request to Amazon SNS to delete the topic identified by the given ARN.
     * If the deletion is successful, a confirmation message is returned. If the operation fails,
     * a {@link SnsServiceException} is thrown.
     *
     * @param topicArn the ARN of the topic to delete. Must not be {@code null} or empty.
     * @return a confirmation message in the format: "[topicArn] deleted successfully".
     * @throws SnsServiceException if the topic deletion fails due to an SNS-related error.
     *                             The exception will contain the original {@link SnsException}.
     */
    public String deleteSNSTopic(String topicArn) {
        try {
            DeleteTopicRequest request = DeleteTopicRequest.builder()
                    .topicArn(topicArn)
                    .build();

            snsClient.deleteTopic(request);
            return topicArn + " deleted successfully";
        } catch (SnsException e) {
            throw new SnsServiceException("Failed to delete snsTopic", e);
        }
    }

    /**
     * Adds an email subscription to the SNS topic specified by the {@code topicArn}.
     * <p>
     * This method subscribes the provided email address to the SNS topic. Once subscribed,
     * the email address will receive notifications when messages are published to the topic.
     * If the subscription fails, a {@link SnsServiceException} is thrown.
     *
     * @param email the email address to subscribe to the topic. Must not be {@code null} or empty.
     * @throws SnsServiceException if the subscription fails due to an SNS-related error.
     *                             The exception will contain the original {@link SnsException}.
     */
    public void addSubscription(String email) {
        try {
            SubscribeRequest request = SubscribeRequest.builder()
                    .protocol("email")
                    .endpoint(email)
                    .topicArn(topicArn)
                    .build();

            snsClient.subscribe(request);
        } catch (SnsException e) {
            throw new SnsServiceException("Failed to Subscribe", e);
        }
    }

    /**
     * Publishes a message to the SNS topic specified by the {@code topicArn}.
     * <p>
     * This method sends a message to the SNS topic. The message will be delivered to all
     * subscribers of the topic. If the publication is successful, a confirmation message
     * is returned. If the operation fails, a {@link SnsServiceException} is thrown.
     *
     * @param message the message to publish. Must not be {@code null} or empty.
     * @return a confirmation message: "Message published Successfully".
     * @throws SnsServiceException if the message publication fails due to an SNS-related error.
     *                             The exception will contain the original {@link SnsException}.
     */
    public String publishTopic(String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .topicArn(topicArn)
                    .subject("Notification from Amazon SNS")
                    .build();

            snsClient.publish(request);
            return "Message published Successfully";
        } catch (SnsException e) {
            throw new SnsServiceException("Failed to publish the message", e);
        }
    }
}