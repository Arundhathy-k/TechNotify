package com.kovan.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
@Document(collection = "TestData")
public class TestRequest {

    @Id
    private String id;
    private String description;


}
