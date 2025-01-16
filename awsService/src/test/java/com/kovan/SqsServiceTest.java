package com.kovan;

import com.kovan.app.service.SqsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static java.util.List.of;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsServiceTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @InjectMocks
    private SqsService sqsService;

    @Test
    void testCreateQueue() {

        String queueName = "TestQueue";
        String queueUrl = "http://example.com/test-queue";

        CreateQueueRequest expectedRequest = CreateQueueRequest.builder()
                .queueName(queueName)
                .build();

        when(sqsAsyncClient.createQueue(expectedRequest))
                .thenReturn(CompletableFuture.completedFuture(CreateQueueResponse.builder().queueUrl(queueUrl).build()));

        sqsService.createQueue(queueName).join();

        verify(sqsAsyncClient).createQueue((expectedRequest));
    }

    @Test
    void testDeleteQueue() {

        String queueUrl = "http://example.com/test-queue";

        DeleteQueueRequest expectedRequest = DeleteQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();

        when(sqsAsyncClient.deleteQueue(expectedRequest))
                .thenReturn(CompletableFuture.completedFuture(DeleteQueueResponse.builder().build()));

        sqsService.deleteQueue(queueUrl).join();

        verify(sqsAsyncClient).deleteQueue((expectedRequest));
    }

    @Test
    void testListQueues() {
        List<String> queueUrls = of("http://example.com/queue1", "http://example.com/queue2");
        when(sqsAsyncClient.listQueues()).thenReturn(CompletableFuture.completedFuture(ListQueuesResponse.builder().queueUrls(queueUrls).build()));

        sqsService.listQueues().join();

        verify(sqsAsyncClient).listQueues();
    }

}
