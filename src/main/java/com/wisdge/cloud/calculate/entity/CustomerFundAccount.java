package com.wisdge.cloud.calculate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("客户id和基金账号关联表")
@TableName("customer_fund_account")
public class CustomerFundAccount {

    private String customerId;
    private String fundAccount;
}
