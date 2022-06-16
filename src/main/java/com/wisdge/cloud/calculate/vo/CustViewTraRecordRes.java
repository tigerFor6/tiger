package com.wisdge.cloud.calculate.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户360视图交易记录信息响应模型
 *
 * @author tiger
 * @date 2021-11-04 11:07:12
 */
@Data
public class CustViewTraRecordRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fundAccountId;
    private String applyDate;
    private String businessTime;
    private String confirmDate;
    private String transactionType;
    private String fundCode;
    private String fundName;
    private String applyAccount;
    private String applyPortion;
    private String confirmAccount;
    private String confirmPortion;
    private String agencyCode;
    private String tradingInstitution;
    private String way;
    private String costAmount;
}