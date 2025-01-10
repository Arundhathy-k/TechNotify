package com.kovan.app.service;

import com.kovan.app.util.User;
import com.kovan.app.exception.FileException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import java.io.StringWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;

@Service
public class HtmlGeneratorService {

    private final Configuration freemarkerConfig;
    private final List<Field> userFields;

    public HtmlGeneratorService(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.userFields = initializeUserFields();
    }

    /**
     * Dynamically generate HTML for the given user object synchronously.
     *
     * @param user the user object
     * @return the generated HTML string
     */
    public String generateHtml(@Valid User user) {
        try {
            // Extract fields and load the template
            Map<String, Object> data = extractFields(user);
            Template template = freemarkerConfig.getTemplate("userTemplate.ftl");

            // Process the template with the extracted data
            return processTemplate(template, data);
        } catch (IOException e) {
            throw new FileException("Error loading FreeMarker template: userTemplate.ftl", e);
        } catch (Exception e) {
            throw new FileException("Error generating HTML", e);
        }
    }

    /**
     * Extracts fields from the given user object and populates a map.
     *
     * @param user the object to extract fields from
     * @return a map of field names and non-null values
     */
    private Map<String, Object> extractFields(@Valid User user) {
        return userFields.stream()
                .filter(field -> {
                    try {
                        return nonNull(field.get(user)); // Include only non-null fields
                    } catch (IllegalAccessException e) {
                        throw new FileException("Error accessing field: " + field.getName(), e);
                    }
                })
                .collect(toMap(Field::getName, field -> {
                    try {
                        return field.get(user);
                    } catch (IllegalAccessException e) {
                        throw new FileException("Error accessing field value: " + field.getName(), e);
                    }
                }));
    }

    /**
     * Processes the FreeMarker template with the provided data map.
     *
     * @param template the FreeMarker template
     * @param data the data map for the template
     * @return the processed template as a string
     */
    private String processTemplate(Template template, Map<String, Object> data) {
        try (StringWriter writer = new StringWriter()) {
            template.process(data, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new FileException("Error processing FreeMarker template", e);
        }
    }

    /**
     * Initializes the list of fields for the User class.
     *
     * @return a list of accessible fields in the User class
     */
    private List<Field> initializeUserFields() {
        return stream(User.class.getDeclaredFields())
                .peek(field -> field.setAccessible(true)) // Enable access to private fields
                .toList();
    }
}
