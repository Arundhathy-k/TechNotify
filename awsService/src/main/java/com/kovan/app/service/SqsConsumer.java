package com.kovan.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.exception.SqsServiceException;
import com.kovan.app.util.MyMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SqsConsumer {
    private final ObjectMapper objectMapper;

    public SqsConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Receives messages from the SQS queue.
     *
     * @param messageBody The message body received from the queue.
     * @throws SqsServiceException If there is an error processing the message.
     */
    @SqsListener("MessageQueue")
    public void receiveMessages(String messageBody) {
        try {
            // Convert the JSON string to a Java object
            MyMessage message = objectMapper.readValue(messageBody, MyMessage.class);
            log.info("Received Message: {}", message);
        } catch (Exception e) {
            log.error("Error processing message: {}", messageBody, e);
            throw new SqsServiceException("Failed to process received message", e);
        }
    }
}
