package com.kovan.app.util;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {

    @NotBlank(message = "First name is required.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    private String lastName;

    @Pattern(regexp = "Male|Female", message = "Gender must be Male or Female.")
    private String gender;

    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits.")
    private String phone;

    @NotBlank(message = "Primary address line 1 is required.")
    private String primaryAddress1;

    @NotNull
    private String primaryAddress2;

    @NotBlank(message = "Primary city is required.")
    private String primaryCity;

    @NotBlank(message = "Primary state is required.")
    private String primaryState;

    @Pattern(regexp = "\\d{5,6}", message = "Primary ZIP code must be 5 or 6 digits.")
    private String primaryZip;

    @NotNull
    private String secondaryAddress1;

    @NotNull
    private String secondaryAddress2;

    @NotNull
    private String secondaryCity;

    @NotNull
    private String secondaryState;

    @Pattern(regexp = "\\d{5,6}", message = "Secondary ZIP code must be 5 or 6 digits.")
    private String secondaryZip;

    @NotBlank(message = "Company name is required.")
    private String companyName;

    @NotNull
    private String companyLocation;

    @NotNull
    private String companyDesignation;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date of joining must be in the format YYYY-MM-DD.")
    private String dateOfJoining;

    @Min(value = 0, message = "Experience cannot be negative.")
    private double experience;

}
