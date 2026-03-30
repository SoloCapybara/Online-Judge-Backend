package com.dxd.oj.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileVO implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String studentNo;

    private String email;

    private String phone;

    private String avatar;

    private Integer gender;

    private Integer status;
}
