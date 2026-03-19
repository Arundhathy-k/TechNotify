package com.kovan.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserInfoController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getUserInfo(Principal principal) {

        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauthUser = oauthToken.getPrincipal();

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("Email",oauthUser.getAttribute("email"));
            userInfo.put("UserName",oauthUser.getAttribute("name"));

            return ResponseEntity.ok(userInfo);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User is not authenticated"));
        }
    }
}
