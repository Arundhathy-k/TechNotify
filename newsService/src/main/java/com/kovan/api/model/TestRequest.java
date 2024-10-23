package com.kovan.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
public class TestRequest {

   @Id
    private String id;
    private String description;


}
