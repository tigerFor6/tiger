package com.wisdge.cloud.feign;

import com.wisdge.cloud.calculate.dto.JobDTO;
import com.wisdge.cloud.dto.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 调度任务接口
 */
@FeignClient(name = "cloud-job", path = "/job/api", url = "${service-api.job:}")
public interface JobServiceFeign {

    @PostMapping("/createJob")
    ApiResult createJob(JobDTO jobDTO);
}
