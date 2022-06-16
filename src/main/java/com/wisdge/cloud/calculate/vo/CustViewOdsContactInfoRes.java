package com.wisdge.cloud.calculate.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustViewOdsContactInfoRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String mobile;
    private String phone;
    private String email;
    private String postCode;
    private String address;
}
