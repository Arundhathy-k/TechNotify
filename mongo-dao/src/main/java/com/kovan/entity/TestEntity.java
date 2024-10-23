package com.kovan.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "TestData")
@Data
@Builder
public class TestEntity {

    @Id
    private String id;
    private String description;


}
