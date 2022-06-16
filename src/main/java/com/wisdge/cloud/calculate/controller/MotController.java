package com.wisdge.cloud.calculate.controller;


import com.wisdge.cloud.calculate.enums.MotTaksCodeEnum;
import com.wisdge.cloud.calculate.service.MotService;
import com.wisdge.cloud.controller.BaseController;
import com.wisdge.cloud.dto.ApiResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * mot服务
 *
 * @author lsy
 * @date 2021-11-16
 */
@Slf4j
@RestController
@RequestMapping("/customer/mot")
@Api(tags = "MotController")
public class MotController extends BaseController {

    @Autowired
    private MotService motService;

    /**
     *
     * @param taskId
     * @return
     * @see MotTaksCodeEnum
     */
    @PostMapping("/task")
    @ApiOperation(value = "客户mot")
    public ApiResult task(String taskId) {
        return ApiResult.ok("ok", motService.motTask(taskId));
    }

    @PostMapping("/updateTradeInfo")
    @ApiOperation(value = "更新客户交易信息")
    public ApiResult updateTradeInfo() {
        return ApiResult.ok("ok", motService.updateTradeInfo());
    }

}
