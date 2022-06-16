package com.wisdge.cloud.calculate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("交易记录信息表")
@TableName("t_trade_info")
public class TradeInfo {

    private String tradeRequestId;
    private String businessType ;
    private String businessState;
    private String tradeAccountNo;
    private String taAccountNo;
    private String fundCode;
    private String targetFundCode;
    private String applicationAmount;
    private String applicationVolume;
    private String transferState;
    private String businessTime;
}
