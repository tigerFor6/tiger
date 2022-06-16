package com.wisdge.cloud.calculate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("客户资产累计信息表")
@TableName("customer_position_count")
public class CustomerPositionCount {

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
     * 资产峰值
     */
    @ApiModelProperty("资产峰值")
    private String assetPeak ;

    /**
     * 直销资产峰值
     */
    @ApiModelProperty("直销资产峰值")
    private String directAssetPeak ;

    /**
     * 峰值日期
     */
    @ApiModelProperty(value = "峰值日期")
    private String peakDate;

    /**
     * 累计盈亏
     */
    @ApiModelProperty("累计盈亏")
    private String totalProfit;

    /**
     * 最近一年交易金额
     */
    @ApiModelProperty("最近一年交易金额")
    private String lastYearTradMoney;

    /**
     * 最近一年交易次数
     */
    @ApiModelProperty("最近一年交易次数")
    private String lastYearTradNum;

    /**
     * 直销最近一年交易次数
     */
    @ApiModelProperty("直销最近一年交易次数")
    private String directLastYearTradNum;

    /**
     * 最近一年交易日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone="GMT+8")
    @ApiModelProperty("最近一年交易日期")
    private Date lastTradTime;

    @JsonIgnore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty("更新时间")
    private Date updateTime;

    /**
     * 创建时间
     */
    @JsonIgnore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
