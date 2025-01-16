package com.kovan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.exception.SqsServiceException;
import com.kovan.app.service.SqsPublisher;
import com.kovan.app.util.MyMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsPublisherTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SqsPublisher sqsPublisher;

    @Test
    void testSendMessage() throws JsonProcessingException {
        MyMessage message = new MyMessage();
        message.setContent("Test Message");

        when(objectMapper.writeValueAsString(message)).thenReturn("{\"content\":\"Test Message\"}");

        CompletableFuture<SendMessageResponse> future = CompletableFuture.completedFuture(
                SendMessageResponse.builder().messageId("123").build());
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(future);

        sqsPublisher.sendMessage(message);

        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testSendMessage_JsonProcessingException() throws JsonProcessingException {

        MyMessage message = MyMessage.builder().content("Test Message").build();
        when(objectMapper.writeValueAsString(message)).thenThrow(new JsonProcessingException("Mocked Exception") {});

        assertThrows(SqsServiceException.class, () -> sqsPublisher.sendMessage(message).join());
        verify(objectMapper).writeValueAsString(message);

    }
}
