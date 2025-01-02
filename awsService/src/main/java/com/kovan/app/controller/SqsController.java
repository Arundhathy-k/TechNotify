package com.kovan.app.controller;

import com.kovan.app.service.SqsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/sqs")
public class SqsController {

    private final SqsService sqsService;

    public SqsController(SqsService sqsService) {
        this.sqsService = sqsService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestParam String message) {
        return ResponseEntity.ok(sqsService.sendMessage(message));
    }

    @PostMapping("/sendBatchMessages")
    public ResponseEntity<String> sendBatchMessage(@RequestParam List<String> messages) {
        return ResponseEntity.ok( sqsService.sendBatchMessages(messages));
    }

    @GetMapping("/receive")
    public ResponseEntity<List<String>> receiveMessages() {

        return ResponseEntity.ok(sqsService.receiveMessages());
    }

    @PostMapping("/create/{queueName}")
    public ResponseEntity<String> createQueue(@PathVariable String queueName) {
        return ResponseEntity.ok( "Queue created: " + sqsService.createQueue(queueName));
    }

    @DeleteMapping("/delete/{queueName}")
    public ResponseEntity<String> deleteQueue(@PathVariable String queueName) {
        return ResponseEntity.ok(sqsService.deleteQueue(queueName));
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listQueues() {
        return ResponseEntity.ok(sqsService.listQueues());
    }
}

