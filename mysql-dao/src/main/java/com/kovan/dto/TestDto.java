package com.kovan.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.Instant;

@Data
@Builder
public class TestDto {

        private Long id;
        private String fileName;

        @Builder.Default
        private String createdBy = "Arundhathy";

        @Builder.Default
        private String updatedBy = "Arundhathy";

        @CreatedDate
        private Instant createdDate;

        @LastModifiedDate
        private Instant updatedDate;
}
