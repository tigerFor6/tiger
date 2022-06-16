package com.wisdge.cloud.calculate.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 用户-客户关联信息dto
 *
 * @author lsy
 * @date 2021-11-19
 */

@Data
@ApiModel("用户-客户关联信息")
public class UserCustomerDTO {

    @ApiModelProperty("客服id")
    private Long userId;

    @ApiModelProperty("客户ID")
    private Long customerId;

    @ApiModelProperty("华夏ods库中的客户ID")
    private String custId;

    @ApiModelProperty("客服名字")
    private String fullname;

    @ApiModelProperty("风险等级")
    private String riskRearingRank;

    @ApiModelProperty("总资产")
    private Integer totalAssets;

    @ApiModelProperty("权益类总资产")
    private Integer rightsTotalAssets;

}
