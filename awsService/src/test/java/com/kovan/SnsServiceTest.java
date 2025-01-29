package com.kovan;

import com.kovan.app.exception.SnsServiceException;
import com.kovan.app.service.SnsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnsServiceTest {

    @Mock
    private SnsClient snsClient;

    @InjectMocks
    private SnsService snsService;

    @Value("${topicArn}")
    private String topicArn;

    @Test
    void testCreateSNSTopic_Success() {
        String topicName = "test-topic";

        CreateTopicRequest expectedRequest = CreateTopicRequest.builder()
                .name(topicName)
                .build();
        CreateTopicResponse response = CreateTopicResponse.builder()
                .topicArn(topicArn)
                .build();

        when(snsClient.createTopic(any(CreateTopicRequest.class))).thenReturn(response);

        String result = snsService.createSNSTopic(topicName);

        assertEquals("Topic created with ARN: " + topicArn, result);
        verify(snsClient).createTopic(expectedRequest);
    }

    @Test
    void testCreateSNSTopic_Failure() {
        String topicName = "test-topic";

        when(snsClient.createTopic(any(CreateTopicRequest.class)))
                .thenThrow(SnsException.builder().message("SNS Error").build());

        assertThrows(SnsServiceException.class, () -> snsService.createSNSTopic(topicName));
    }

    @Test
    void testDeleteSNSTopic_Success() {
        String topicArn = "arn:aws:sns:us-east-1:123456789012:test-topic";

        DeleteTopicRequest expectedRequest = DeleteTopicRequest.builder()
                .topicArn(topicArn)
                .build();

        when(snsClient.deleteTopic(any(DeleteTopicRequest.class))).thenReturn(null);

        String result = snsService.deleteSNSTopic(topicArn);

        assertEquals(topicArn + " deleted successfully", result);
        verify(snsClient).deleteTopic(expectedRequest);
    }

    @Test
    void testDeleteSNSTopic_Failure() {

        doThrow(SnsException.builder().message("SNS Error").build())
                .when(snsClient).deleteTopic(any(DeleteTopicRequest.class));

        assertThrows(SnsServiceException.class, () -> snsService.deleteSNSTopic(topicArn));
    }

    @Test
    void testAddSubscription_Success() {
        String email = "test@example.com";

        when(snsClient.subscribe(any(SubscribeRequest.class))).thenReturn(null);

        assertDoesNotThrow(() -> snsService.addSubscription(email));

    }

    @Test
    void testAddSubscription_Failure() {
        String email = "test@example.com";

        doThrow(SnsException.builder().message("SNS Error").build())
                .when(snsClient).subscribe(any(SubscribeRequest.class));

        assertThrows(SnsServiceException.class, () -> snsService.addSubscription(email));
    }

    @Test
    void testPublishTopic_Success() {
        String message = "Test Message";

        doReturn(PublishResponse.builder().build())
                .when(snsClient).publish(any(PublishRequest.class));

        String result = snsService.publishTopic(message);

        assertEquals("Message published Successfully", result);
    }

    @Test
    void testPublishTopic_Failure() {
        String message = "Test Message";

        doThrow(SnsException.builder().message("SNS Error").build())
                .when(snsClient).publish(any(PublishRequest.class));

        assertThrows(SnsServiceException.class, () -> snsService.publishTopic(message));
    }
}
