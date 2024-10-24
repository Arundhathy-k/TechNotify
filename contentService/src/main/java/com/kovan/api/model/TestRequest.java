package com.kovan.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;




@Data
public class TestRequest {

    @Id
    private Long id;
    private String description;
    private String createdBy;
    private String updatedBy;

}
