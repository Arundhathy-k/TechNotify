package com.kovan;

import com.kovan.app.service.SnsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class SnsServiceIT {

    @Autowired
    private SnsService snsService;

    @Test
    void testCreateAndDeleteSNSTopic() {
        String topicName = "integration-test-topic";

        String response = snsService.createSNSTopic(topicName);
        assertTrue(response.contains("Topic created with ARN:"));

        String topicArn = response.split(": ")[1];

        String deleteResponse = snsService.deleteSNSTopic(topicArn);
        assertEquals(topicArn + " deleted successfully", deleteResponse);
    }

    @Test
    void testSubscribeAndPublish() {
        String email = "your-email@example.com";
        assertDoesNotThrow(() -> snsService.addSubscription(email));

        String message = "Integration Test Message";
        String publishResponse = snsService.publishTopic(message);
        assertEquals("Message published Successfully", publishResponse);
    }
}
