package com.wisdge.cloud.calculate.controller;

import com.wisdge.cloud.calculate.entity.CustomerPosition;
import com.wisdge.cloud.calculate.service.CustomerService;
import com.wisdge.cloud.controller.BaseController;
import com.wisdge.cloud.dto.ApiResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/customer")
@Api(tags="CustBaseController")
public class CustomerController extends BaseController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/synCustomerInfo")
    @ApiOperation(value = "同步客户")
    public ApiResult synCustomerInfo(@RequestBody List<String> custIds) throws Exception {
        customerService.synCustomerInfo(custIds);
        return ApiResult.ok();
    }

    @PostMapping("/updateHoldArc")
    @ApiOperation(value = "更新customerId到hd_hold_arc中")
    public ApiResult updateHoldArc() throws Exception {
        customerService.updateHoldArc();
        return ApiResult.ok();
    }

    @PostMapping("/synCustomerInfoJob")
    @ApiOperation(value = "定时计算客户历史仓数据")
    public ApiResult synCustomerInfoJob() throws Exception {
        customerService.synCustomerInfoJob();
        return ApiResult.ok();
    }

    @PostMapping("/updateFundTypeCountJob")
    @ApiOperation(value = "定时计算在管客户规模变动数据")
    public ApiResult updateFundTypeCountJob() throws Exception {
        customerService.updateFundTypeCountJob();
        return ApiResult.ok();
    }

    @PostMapping("/updateCustomerLoseJob")
    @ApiOperation(value = "定时计算客户流失数据")
    public ApiResult updateCustomerLoseJob() throws Exception {
        customerService.updateCustomerLoseJob();
        return ApiResult.ok();
    }

    @PostMapping("/historyInterceptor")
    @ApiOperation(value = "星环大数据获取最近一年的数据")
    public ApiResult historyInterceptor(@RequestBody List<String> custIds) throws Exception {
        customerService.historyInterceptor(custIds);
        return ApiResult.ok();
    }

    @PostMapping("/hdHoldArcToPosition")
    @ApiOperation(value = "星环大数据封装到position中")
    public ApiResult hdHoldArcToPosition() throws Exception {
        customerService.hdHoldArcToPosition();
        return ApiResult.ok();
    }

    @PostMapping("/updateBaseInfo")
    @ApiOperation(value = "更新客户信息")
    public ApiResult updateBaseInfo(String customerId) throws Exception {
        customerService.updateBaseInfo(customerId);
        return ApiResult.ok();
    }

    @GetMapping("/getOdsContractInfo")
    @ApiOperation(value = "获取Ods中客户的联系信息")
    public ApiResult getOdsContractInfo(String customerId) throws Exception {
        return ApiResult.ok("ods联系信息", customerService.getOdsContractInfo(customerId));
    }

    @PostMapping("/insertCustPosition")
    @ApiOperation(value = "插入遗漏的客户持仓信息")
    public ApiResult insertCustPosition(String holdTime) throws Exception {
        customerService.insertCustPosition(holdTime);
        return ApiResult.ok();
    }

    @PostMapping("/updateCustPositions")
    @ApiOperation(value = "批量更新客户持仓信息")
    public ApiResult updateCustPositions() throws Exception {
        customerService.updateCustPositions();
        return ApiResult.ok();
    }

    @PostMapping("/updateCustPositionCounts")
    @ApiOperation(value = "批量更新客户持仓信息")
    public ApiResult updateCustPositionCounts() throws Exception {
        customerService.updateCustPositionCounts();
        return ApiResult.ok();
    }

    @GetMapping("/getBonusInfo")
    @ApiOperation(value = "获取分红信息")
    public ApiResult getBonusInfo(String customerId) throws Exception {
        return ApiResult.ok("分红信息", customerService.getBonusInfo(customerId));
    }

    @GetMapping("/getAffirmBaseInfo")
    @ApiOperation(value = "获取交易记录信息")
    public ApiResult getAffirmBaseInfo(String customerId) throws Exception {
        return ApiResult.ok("交易记录信息", customerService.getAffirmBaseInfo(customerId));
    }

    @GetMapping("/getPlanInfo")
    @ApiOperation(value = "获取定投计划信息")
    public ApiResult getPlanInfo(String customerId) throws Exception {
        return ApiResult.ok("获取定投计划信息", customerService.getPlanInfo(customerId));
    }

    @GetMapping("/getAccountChangeInfo")
    @ApiOperation(value = "获取账户变动信息")
    public ApiResult getAccountChangeInfo(String customerId) throws Exception {
        return ApiResult.ok("获取账户变动信息", customerService.getAccountChangeInfo(customerId));
    }

    @GetMapping("/getFundProfitInfo")
    @ApiOperation(value = "获取损益详情饼图信息")
    public ApiResult getFundProfitInfo(String customerId, String holdType) throws Exception {
        return ApiResult.ok("获取损益详情饼图信息", customerService.getFundProfitInfo(customerId, holdType));
    }

    @GetMapping("/getIncomeChartInfo")
    @ApiOperation(value = "获取累计收益曲线表数据")
    public ApiResult getIncomeChartInfo(String customerId, String startTime, String endTime, String timeFlag) throws Exception {
        return ApiResult.ok("获取累计收益曲线表数据", customerService.getIncomeChartInfo(customerId, startTime, endTime, timeFlag));
    }

    @GetMapping("/getFundProfitDetail")
    @ApiOperation(value = "持有基金收益详情数据")
    public ApiResult getFundProfitDetail(String customerId, String type) throws Exception {
        return ApiResult.ok("持有基金收益详情数据", customerService.getFundProfitDetail(customerId, type));
    }

    @GetMapping("/getWechatFundDetail")
    @ApiOperation(value = "企微-账户分析")
    public ApiResult getWechatFundDetail(String customerId) throws Exception {
        return ApiResult.ok("企微-账户分析数据", customerService.getWechatFundDetail(customerId));
    }

    @GetMapping("/getWechatAgencyFundDetail")
    @ApiOperation(value = "企微-账户分析-展开详情")
    public ApiResult getWechatAgencyFundDetail(String customerId, String fundCode) throws Exception {
        return ApiResult.ok("企微-账户分析-展开详情", customerService.getWechatAgencyFundDetail(customerId, fundCode));
    }

    @GetMapping("/getWechatProfitChart")
    @ApiOperation(value = "企微-账户分析-展开详情-累计收益图表")
    public ApiResult getWechatProfitChart(String customerId, String fundCode, String chartDay) throws Exception {
        return ApiResult.ok("企微-账户分析-展开详情-累计收益图表", customerService.getWechatProfitChart(customerId, fundCode, chartDay));
    }

    @GetMapping("/getWechatWeekChart")
    @ApiOperation(value = "企微-账户分析-展开详情-七日年化收益率图表")
    public ApiResult getWechatWeekChart(String fundCode, String chartDay) throws Exception {
        return ApiResult.ok("企微-账户分析-展开详情-七日年化收益率图表", customerService.getWechatWeekChart(fundCode, chartDay));
    }

}
