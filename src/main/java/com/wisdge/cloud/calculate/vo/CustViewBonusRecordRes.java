package com.wisdge.cloud.calculate.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户360视图分红记录信息响应模型
 *
 * @author tiger
 * @date 2021-11-04 11:07:12
 */
@Data
public class CustViewBonusRecordRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String confirmDate;
    private String fundCode;
    private String fundName;
    private String wayId;
    private String way;
    private String distributionAmount;
    private String distributionPortion;
    private String agencyCode;
    private String tradingInstitution;
}