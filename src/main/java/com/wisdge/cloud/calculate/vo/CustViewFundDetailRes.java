package com.wisdge.cloud.calculate.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户360视图账户分析-持有基金详情响应模型
 *
 * @author tiger
 * @date 2021-11-04 11:07:12
 */
@Data
public class CustViewFundDetailRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fundName;
    private String way;
    private String holdPortion;
    private String todoPortion;
    private String netWorthDate;
    private String netWorth;
    private String balance;
    private String institution;
}