package com.kovan.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import static java.util.Objects.nonNull;

@RestController
@RequestMapping("/api/user")
public class UserInfoController {

    @GetMapping("/info")
    public ResponseEntity<String> getUserInfo(Principal principal) {
        if (nonNull(principal)) {
            return ResponseEntity.ok("User Info: " + principal.getName());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }
    }
}
