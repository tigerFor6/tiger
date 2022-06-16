package com.wisdge.cloud.calculate.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 持有基金产品信息响应模型
 *
 * @author tiger
 * @date 2021-11-04 11:07:12
 */
@Data
public class HoldFundInfoRes implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fundCode;
    private String dividendMethod;
    private String agencyCode;
    private String holdShares;
    private String midwayVolume;
}