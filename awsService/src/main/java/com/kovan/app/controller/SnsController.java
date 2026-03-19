package com.kovan.app.controller;

import com.kovan.app.service.SnsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sns")
public class SnsController {

    private final SnsService snsService;

    public SnsController(SnsService snsService) {
        this.snsService = snsService;
    }

    @PostMapping("/createTopic/{topicName}")
    public ResponseEntity<String> createTopic(@PathVariable String topicName){
        return ResponseEntity.ok(snsService.createSNSTopic(topicName));
    }

    @DeleteMapping("/deleteTopic/{topicArn}")
    public ResponseEntity<String> deleteTopic(@PathVariable String topicArn){
        return ResponseEntity.ok(snsService.deleteSNSTopic(topicArn));
    }

    @GetMapping("/addSub/{email}")
    public ResponseEntity<String> addSubscription(@PathVariable String email){
        snsService.addSubscription(email);
        return ResponseEntity.ok("Subscription request is pending. To confirm the subscription, check your email : "+email);

    }

    @GetMapping("/publish")
    public ResponseEntity<String> publishMessageToTopic(@RequestParam String message){
        return ResponseEntity.ok(snsService.publishTopic(message));

    }
}
