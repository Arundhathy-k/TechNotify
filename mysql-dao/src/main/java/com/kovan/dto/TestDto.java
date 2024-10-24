package com.kovan.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class TestDto {

        private Long id;
        private String description;
        private String createdOn;
        private String createdBy;
        private String updatedOn;
        private String updatedBy;

    }

