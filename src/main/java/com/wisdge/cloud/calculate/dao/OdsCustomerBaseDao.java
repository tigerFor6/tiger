package com.wisdge.cloud.calculate.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.wisdge.cloud.calculate.constant.DatasourceConstants;
import com.wisdge.cloud.calculate.vo.FundInfoRes;
import com.wisdge.cloud.calculate.vo.HoldFundInfoRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Author tiger
 * @Date 2021/11/23
 */
@Mapper
@DS(DatasourceConstants.ORACLE)
public interface OdsCustomerBaseDao {

    List<Map<String, Object>> getOdsCustomer(@Param("custIds") List<String> custIdList);

    /**
     * 获取基金账号
     * @param custId
     * @return
     */
    List<String> getFundAccountId(@Param("custId") String custId);

    /**
     * 获取损益可以查询到的基金账号
     * @param custId
     * @return
     */
    List<String> getSyFundAccountId(@Param("custId") String custId);

    /**
     * 获取风险等级
     * @param custId
     * @return
     */
    List<Map<String, String>> getRiskLevel(@Param("custId") String custId);

    /**
     * 获取交易账号
     * @param custId
     * @return
     */
    List<String> getTradeAccount(@Param("custId") String custId);

    /**
     * 获取持有的基金产品
     * @param custId
     * @return
     */
    List<HoldFundInfoRes> getHoldFund(@Param("custId") String custId);

    /**
     * 获取权益类持有的基金产品
     * @return
     */
    List<HoldFundInfoRes> getRightsFund(@Param("custId") String custId);

    /**
     * 获取基金的持有份额
     * @param fundCode
     * @return
     */
    Map<String,String> getHoldShares(@Param("fundCode") String fundCode);

    /**
     * 获取基金的最新净值
     * @return
     */
    List<Map<String,Object>> getDayFundNav();

    /**
     * 获取基金的历史所有净值
     * @return
     */
    List<Map<String,Object>> getFundNav();

    /**
     * 获取某个基金的非交易日的净值
     * @return
     */
    Map<String,Object> getNotWorkFundNav(String fundCode, String fundNavDate);

    /**
     * 获取基金的七日年化率图表数据
     * @return
     */
    List<Map<String,Object>> getWeekProfitRatio(@Param("fundCode") String fundCode,@Param("beginDate") String beginDate,@Param("endDate") String endDate);

    /**
     * 获取基金渠道信息
     * @return
     */
    List<Map<String,String>> getAgency();

    /**
     * 获取基金的名称和分类信息
     * @param fundCode
     * @return
     */
    List<FundInfoRes> getFundInfo(@Param("fundCode") String fundCode);

    /**
     * 获取最新的美元汇率
     * @return
     */
    String getErvalue();

    /**
     * 获取历史所有的美元汇率
     * @return
     */
    List<Map<String,Object>> getAllErvalue();

    /**
     * 获取非交易日的美元汇率
     * @return
     */
    List<Map<String,Object>> getNotWorkErValue(String erDate);

    /**
     * 获取最近交易时间，最近交易次数，最近一年交易金额
     * @param custId
     * @return
     */
    Map<String, Object> getTraInfo(@Param("custId") String custId, @Param("beginTime") String beginTime);
    /**
     * 获取直销最近交易次数
     * @param custId
     * @return
     */
    Map<String, Object> getDirectTraInfo(@Param("custId") String custId, @Param("beginTime") String beginTime);

    /**
     * 根据流水号获取交易记录中的付款方式和申请时间
     * @param tradeRequestId
     * @return
     */
    Map<String, Object> getTradeRequestInfo(@Param("tradeRequestId") String tradeRequestId);

    /**
     * 根据交易账号获取定投信息
     * @param tradeAccountNo
     * @return
     */
    List<Map<String, Object>> getPlanInfo(@Param("tradeAccountNo") String tradeAccountNo);

    /**
     * 根据定投计划id获取累计定投金额和已投期数
     * @param tradePlanId
     * @return
     */
    Map<String, Object> getPlanInfoExp(@Param("tradePlanId") String tradePlanId);

    /**
     * 账户信息变动
     * @param custId
     * @return
     */
    List<Map<String, Object>> getAccountChangeInfo(@Param("custId") String custId);

    List<Map<String, Object>> getFundTypeCount(@Param("map")Map map);
}
