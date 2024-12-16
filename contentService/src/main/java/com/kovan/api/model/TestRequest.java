package com.kovan.api.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestRequest {

    private Long id;
    private String fileName;
    @Builder.Default
    private String createdBy = "Arundhathy";

    @Builder.Default
    private String updatedBy = "Arundhathy";

}