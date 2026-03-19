package com.kovan.app.controller;

import com.kovan.app.service.SqsPublisher;
import com.kovan.app.service.SqsService;
import com.kovan.app.util.MyMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/sqs")
public class SqsController {

    private final SqsService sqsService;
    private final SqsPublisher sqsPublisher;

    public SqsController(SqsService sqsService, SqsPublisher sqsPublisher) {
        this.sqsService = sqsService;
        this.sqsPublisher = sqsPublisher;
    }

    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<String>> sendMessage(@RequestBody MyMessage messageBody) {
        return sqsPublisher.sendMessage(messageBody)
                .thenApply(result -> ResponseEntity.ok("Message sent successfully."));
    }

    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<String>> createQueue(@RequestParam String queueName) {
        return sqsService.createQueue(queueName)
                .thenApply(queueUrl -> ResponseEntity.ok("Queue created at URL: " + queueUrl));
    }

    @DeleteMapping("/delete")
    public CompletableFuture<ResponseEntity<String>> deleteQueue(@RequestParam String queueUrl) {
        return sqsService.deleteQueue(queueUrl)
                .thenApply(result -> ResponseEntity.ok("Queue deleted successfully."));
    }

    @GetMapping("/list")
    public CompletableFuture<ResponseEntity<?>> listQueues() {
        return sqsService.listQueues()
                .thenApply(listQueuesResponse -> ResponseEntity.ok(listQueuesResponse.queueUrls()));
    }
}

