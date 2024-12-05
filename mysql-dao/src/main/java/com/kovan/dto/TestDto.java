package com.kovan.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class TestDto {

        private Long id;
        private String description;
        private String createdBy;
        private String updatedBy;
        private LocalDate createdDate;
        private LocalDate updatedDate;
    }

