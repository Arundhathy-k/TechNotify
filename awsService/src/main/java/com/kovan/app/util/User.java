package com.kovan.app.util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String address;

}
