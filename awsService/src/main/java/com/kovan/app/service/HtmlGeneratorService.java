package com.kovan.app.service;

import com.kovan.app.util.User;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.util.HashMap;
import static java.util.Locale.getDefault;
import java.util.Map;

@Service
public class HtmlGeneratorService {

    private final SpringTemplateEngine templateEngine;

    public HtmlGeneratorService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String generateHtml(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("address", user.getAddress());

        return templateEngine.process("userTemplate", new Context(getDefault(), data));
    }
}
