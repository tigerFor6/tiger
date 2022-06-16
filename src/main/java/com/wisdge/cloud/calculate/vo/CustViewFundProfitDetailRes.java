package com.wisdge.cloud.calculate.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 客户360视图账户分析-持有基金收益详情响应模型
 *
 * @author tiger
 * @date 2021-11-04 11:07:12
 */
@Data
public class CustViewFundProfitDetailRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fundCode;
    private String fundName;
    private String holdPortion;
    private String averageCost;
    private String holdInvestment;
    private String netWorthDate;
    private String netWorth;
    private String latestMarket;
    private String profit;
}