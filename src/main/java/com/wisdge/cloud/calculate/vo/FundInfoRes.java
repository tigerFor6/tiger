package com.wisdge.cloud.calculate.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 基金产品信息响应模型
 *
 * @author tiger
 * @date 2021-11-04 11:07:12
 */
@Data
public class FundInfoRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fundCode;
    private String fundName;
    private String currencyTypeid;
    private String fundType;
}