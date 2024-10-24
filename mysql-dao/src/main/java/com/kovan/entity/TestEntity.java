package com.kovan.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class TestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
//    private String createdDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
//    private String createdBy;
//    private String updatedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
//    private String updatedBy;


    private LocalDate createdDate;
    private String createdBy;
    private LocalDate updatedDate;
    private String updatedBy;

}
