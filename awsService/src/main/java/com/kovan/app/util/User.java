package com.kovan.app.util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {

    private String firstName;
    private String lastName;
    private String gender;
    private String phone;

    private String primaryAddress1;
    private String primaryAddress2;
    private String primaryCity;
    private String primaryState;
    private String primaryZip;

    private String secondaryAddress1;
    private String secondaryAddress2;
    private String secondaryCity;
    private String secondaryState;
    private String secondaryZip;

    private String companyName;
    private String companyLocation;
    private String companyDesignation;
    private String dateOfJoining;
    private double experience;

}
