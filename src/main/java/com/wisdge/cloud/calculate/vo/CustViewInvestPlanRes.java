package com.wisdge.cloud.calculate.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户360视图定投计划响应模型
 *
 * @author tiger
 * @date 2021-11-04 11:07:12
 */
@Data
public class CustViewInvestPlanRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tradePlanId;
    private String fundName;
    private String totalPlanMount;
    private String amount;
    private String planMount;
    private String periods;
    private String profit;
    private String proChannel;
    private String deductChannel;
    private String status;
    private String planDate;
    private String planDay;
    private String target;
}