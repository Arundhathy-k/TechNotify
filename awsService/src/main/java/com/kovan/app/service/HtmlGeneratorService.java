package com.kovan.app.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class HtmlGeneratorService {

    private final SpringTemplateEngine templateEngine;

    public HtmlGeneratorService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String generateHtml(String userId, String name, String email, String phone, String address) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("address", address);

        return templateEngine.process("userTemplate", new Context(Locale.getDefault(), data));
    }
}
