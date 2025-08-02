package com.saltyfish.backend.pojo.dto;

import java.io.Serializable;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class UserDTO implements Serializable {

    private String username;

    private String account;

    private String password;

    private String email;
}
