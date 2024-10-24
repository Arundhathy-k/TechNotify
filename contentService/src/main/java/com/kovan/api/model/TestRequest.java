package com.kovan.api.model;

import lombok.Data;


@Data
public class TestRequest {


    private Long id;
    private String description;
    private String createdBy;
    private String updatedBy;

}
