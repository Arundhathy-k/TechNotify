package com.kovan.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Document {

    @Id
    private String id;
    private String fileName;

    @CreatedDate
    private String createdDate;

    @LastModifiedDate
    private String updatedDate;

    @Builder.Default
    private String createdBy = "Arundhathy";

    @Builder.Default
    private String updatedBy = "Arundhathy";

}
