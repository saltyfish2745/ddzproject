package com.saltyfish.backend.pojo.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class UserLoginDTO implements Serializable {

    private String account;

    private String password;
}
