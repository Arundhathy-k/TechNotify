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
import java.util.Date;


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
    private String createdOn = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    private String createdBy;
    private String updatedOn = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    private String updatedBy;


}
