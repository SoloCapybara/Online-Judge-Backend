package com.dxd.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LoginUserVO implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String avatar;

    private Integer status;

    private List<String> roles;
}
