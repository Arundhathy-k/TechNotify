package com.kovan.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Repository;

import java.util.List;

@Document(collection = "News")
@Data
public class NewsEntity {

    @Id
    private String id;
    private String description;


}
