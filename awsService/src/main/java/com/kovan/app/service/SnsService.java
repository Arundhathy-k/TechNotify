package com.kovan.app.service;

import com.kovan.app.exception.SnsServiceException;
import com.kovan.app.exception.SqsServiceException;
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

    public String createSNSTopic(String topicName) {

        try {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();

            CreateTopicResponse  result = snsClient.createTopic(request);
            return "Topic created with ARN: " +  result.topicArn();

        } catch (SnsException e) {
            throw new SnsServiceException("Failed to create snsTopic", e);
        }
    }

    public String deleteSNSTopic(String topicArn) {
        try {
            DeleteTopicRequest request = DeleteTopicRequest.builder()
                    .topicArn(topicArn)
                    .build();

            snsClient.deleteTopic(request);
            return topicArn + " deleted successfully";
        } catch (SnsException e) {
            throw new SnsServiceException("Failed to delete snsTopic",e);
        }
    }

    public void addSubscription(String email) {
        try {
            SubscribeRequest request = SubscribeRequest.builder()
                    .protocol("email")
                    .endpoint(email)
                    .topicArn(topicArn)
                    .build();

            snsClient.subscribe(request);

        } catch (SnsException e) {
            throw new SnsServiceException("Failed to Subscribe",e);
        }
    }

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
           throw new SnsServiceException("Failed to publish the message",e);
        }
    }
}
