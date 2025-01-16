package com.kovan;

import com.kovan.app.service.SqsService;
import com.kovan.app.util.MyMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SqsServiceIT {

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    @Autowired
    private SqsService sqsService;

    @Test
    void testSendMessage() {
        MyMessage message = MyMessage.builder().content("Integration Test Message").build();

        CompletableFuture<Void> response = sqsService.sendMessage(message);
        response.join();

        assertNotNull(response);
        assertDoesNotThrow(response::join);
    }

    @Test
    void testCreateQueue() {
        String queueName = "test-queue";
        CompletableFuture<String> response = sqsService.createQueue(queueName);
        String qUrl = response.join();

        assertNotNull(qUrl);
        assertTrue(qUrl.contains(queueName));
    }

    @Test
    void testDeleteQueue() {

        String queueName = "test-queueName";
        CompletableFuture<String> createResponse = sqsService.createQueue(queueName);
        String createdQueueUrl = createResponse.join();

        CompletableFuture<Void> deleteResponse = sqsService.deleteQueue(createdQueueUrl);
        deleteResponse.join();

        assertNotNull(deleteResponse);
        assertDoesNotThrow(deleteResponse::join);
    }

    @Test
    void testListQueues() {
        CompletableFuture<ListQueuesResponse> response = sqsService.listQueues();
        ListQueuesResponse result = response.join();

        assertNotNull(result);
        assertFalse(result.queueUrls().isEmpty());
    }

}
