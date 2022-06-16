package com.wisdge.cloud.calculate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;


/**
 * 客户MOT实体
 *
 * @author lsy
 * @date 2021-11-18
 */

@Data
@ApiModel("客户MOT信息")
@TableName(value = "customer_mot")
public class CustomerMotEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("客户ID")
    @TableField(value = "customer_id")
    private Long customerId;

    @ApiModelProperty("内容")
    @TableField(value = "content")
    private String content;

    @ApiModelProperty("类型")
    @TableField(value = "type")
    private String type;

    @ApiModelProperty("处理情况")
    @TableField(value = "treatment")
    private String treatment;

    @ApiModelProperty("处理人id")
    @TableField(value = "handler_id")
    private Long handlerId;

    @ApiModelProperty("处理人")
    @TableField(value = "handler")
    private String handler;

    @ApiModelProperty("处理时间")
    @TableField(value = "handle_time")
    private Date handleTime;

    @ApiModelProperty("状态")
    @TableField(value = "status")
    private Integer status;

    @ApiModelProperty("创建时间")
    @TableField(value = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date createTime;
}
