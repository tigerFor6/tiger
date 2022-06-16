package com.wisdge.cloud.calculate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("客户持仓信息表")
@TableName("customer_position")
public class CustomerPosition {

    /**
     * 唯一ID
     */
    @ApiModelProperty("唯一ID")
    private String id;

    /**
     * 客户ID
     */
    @ApiModelProperty(value = "客户ID")
    private String customerId;

    /**
     * 总资产
     */
    @ApiModelProperty("总资产")
    private String totalAssets ;

    /**
     * 直销总资产
     */
    @ApiModelProperty("直销总资产")
    private String directTotalAssets ;

    /**
     * 直销货币类总资产
     */
    @ApiModelProperty("直销货币类总资产")
    private String directMoneyTotalAssets ;

    /**
     * 昨日盈亏
     */
    @ApiModelProperty(value = "昨日盈亏")
    private String dayIncome;

    /**
     * 持仓盈亏
     */
    @ApiModelProperty("持仓盈亏")
    private String holdProfit;

    /**
     * 持仓盈亏
     */
    @ApiModelProperty("持仓盈亏率")
    private String profitRate;

    /**
     * 持有产品
     */
    @ApiModelProperty(value = "持有产品")
    private String holdFund;

    /**
     * 持有产品名称
     */
    @ApiModelProperty(value = "持有产品名称")
    private String holdFundName;

    /**
     * 创建人
     */
    @JsonIgnore
    @ApiModelProperty("创建人")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonIgnore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;

    /**
     * 持仓时间
     */
    @ApiModelProperty("持仓时间")
    private String holdTime;
}
