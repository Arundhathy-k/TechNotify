package com.kovan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kovan.app.service.SqsConsumer;
import com.kovan.app.util.MyMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SqsConsumer sqsConsumer;

    @Test
    void testReceiveMessages() throws Exception {
        String messageBody = "{\"content\":\"Test Message\"}";
        MyMessage message = MyMessage.builder().content("Test Message").build();
        when(objectMapper.readValue(messageBody, MyMessage.class)).thenReturn(message);

        sqsConsumer.receiveMessages(messageBody);

        verify(objectMapper).readValue(messageBody, MyMessage.class);
    }
}
