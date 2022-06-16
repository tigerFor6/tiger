package com.wisdge.cloud.calculate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("星环大数据客户持仓信息表")
@TableName("hd_hold_arc_new")
public class HdHoldArc {

    private String taCode;
    private String customerId;
    private String acctId;
    private String fundCode ;
    private String agencyCode;
    private String agencyTrustChannel;
    private String agencySeatCode;
    private String dividendMode;
    private String holdShare;
    private String frozenShare;
    private Date confirmDate;
}
