package com.kovan.app.controller;

import com.kovan.app.service.SqsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/sqs")
public class SqsController {

    private final SqsService sqsService;

    public SqsController(SqsService sqsService) {
        this.sqsService = sqsService;
    }

    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<String>> sendMessage(@RequestBody String messageBody) {
        return sqsService.sendMessage(messageBody)
                .thenApply(result -> ResponseEntity.ok("Message sent successfully."));
    }

    @GetMapping("/receive")
    public CompletableFuture<ResponseEntity<?>> receiveMessages() {
        return sqsService.receiveMessages()
                .thenApply(result -> ResponseEntity.ok("Message received successfully."));
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

