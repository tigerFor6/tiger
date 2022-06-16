package com.wisdge.cloud.calculate.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 任务入参
 *
 * @author lsy @57127127
 * @date 2021-11-16
 */
@Data
@ApiModel("任务入参")
public class JobDTO {
    private static final long serialVersionUID = 1L;

    // 服务名称
    private String triggerName;

    // 归属系统名称
    private String triggerGroupName;

    // 执行周期
    private String cron;

    // 入参
    private String jobData;

    // 执行的逻辑module类名称
    private String moduleName;
}
