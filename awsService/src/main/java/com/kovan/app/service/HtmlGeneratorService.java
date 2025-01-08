package com.kovan.app.service;

import com.kovan.app.util.User;
import com.kovan.app.exception.FileException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;
import java.io.StringWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class HtmlGeneratorService {

    private final Configuration freemarkerConfig;

    public HtmlGeneratorService(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }

    public String generateHtml(User user){

        Map<String, Object> data = new HashMap<>();
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("gender", user.getGender());
        data.put("phone", user.getPhone());

        data.put("primaryAddress1", user.getPrimaryAddress1());
        data.put("primaryAddress2", user.getPrimaryAddress2());
        data.put("primaryCity", user.getPrimaryCity());
        data.put("primaryState", user.getPrimaryState());
        data.put("primaryZip", user.getPrimaryZip());

        data.put("secondaryAddress1", user.getSecondaryAddress1());
        data.put("secondaryAddress2", user.getSecondaryAddress2());
        data.put("secondaryCity", user.getSecondaryCity());
        data.put("secondaryState", user.getSecondaryState());
        data.put("secondaryZip", user.getSecondaryZip());

        data.put("companyName", user.getCompanyName());
        data.put("companyLocation", user.getCompanyLocation());
        data.put("companyDesignation", user.getCompanyDesignation());
        data.put("dateOfJoining", user.getDateOfJoining());
        data.put("experience", user.getExperience());

        Template template = null;
        try {
            template = freemarkerConfig.getTemplate("userTemplate.ftl");
        } catch (IOException e) {
            throw new FileException("Error loading FreeMarker template: userTemplate.ftl", e);
        }

        try (StringWriter writer = new StringWriter()) {
            template.process(data, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new FileException("Error processing FreeMarker template", e);
        }
    }
}
