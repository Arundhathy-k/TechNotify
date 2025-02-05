package com.kovan.controller;

import com.kovan.service.PublisherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/publisher")
public class PublisherController {

    private final PublisherService publisherService;

    public PublisherController(PublisherService publisherService) {
        this.publisherService = publisherService;
    }


    @PostMapping("/{topic}")
    public String publish(@PathVariable String topic, @RequestBody String message) {
        publisherService.sendMessage(topic, message);
        return "Message sent to Kafka topic: " + message;
    }
}
