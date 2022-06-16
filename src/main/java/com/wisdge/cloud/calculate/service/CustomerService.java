package com.wisdge.cloud.calculate.service;

import com.wisdge.cloud.calculate.entity.CustomerPosition;
import com.wisdge.cloud.calculate.entity.CustomerPositionCount;
import com.wisdge.cloud.calculate.vo.*;

import java.util.List;
import java.util.Map;

/**
 * CustomerService
 * @author tiger
 * @date 2021-11-22
 */
public interface CustomerService {

    void synCustomerInfo(List<String> custIds) throws Exception;

    void synCustomerInfoJob() throws Exception;

    void updateFundTypeCountJob() throws Exception;

    void updateCustomerLoseJob() throws Exception;

    void updateHoldArc() throws Exception;

    void historyInterceptor(List<String> custIds) throws Exception;

    void hdHoldArcToPosition() throws Exception;

    void updateBaseInfo(String customerId) throws Exception;

    CustViewOdsContactInfoRes getOdsContractInfo(String customerId) throws Exception;

    void updateCustPosition(String customerId,String custId, List<CustomerPosition> customerPositions) throws Exception;

    void updateCustPositions() throws Exception;

    void insertCustPosition(String holdTime) throws Exception;

    void updateCustPositionCount(String customerId, String custId, List<CustomerPositionCount> customerPositionCounts) throws Exception;

    void updateCustPositionCounts() throws Exception;

    List<CustViewBonusRecordRes> getBonusInfo(String customerId) throws Exception;

    List<CustViewTraRecordRes> getAffirmBaseInfo(String customerId) throws Exception;

    List<CustViewInvestPlanRes> getPlanInfo(String customerId) throws Exception;

    List<CustViewRecentDevelpRes> getAccountChangeInfo(String customerId) throws Exception;

    Map<String, Object> getFundProfitInfo(String customerId, String holdType) throws Exception;

    Map<String, Object> getIncomeChartInfo(String customerId, String startTime, String endTime, String timeFlag) throws Exception;

    List<CustViewFundProfitDetailRes> getFundProfitDetail(String customerId, String type) throws Exception;

    List<Map<String, Object>> getWechatFundDetail(String customerId) throws Exception;

    List<Map<String, Object>> getWechatAgencyFundDetail(String customerId, String fundCode) throws Exception;

    List<Map<String, Object>> getWechatProfitChart(String customerId, String fundCode, String chartDay) throws Exception;

    List<Map<String, Object>> getWechatWeekChart(String fundCode, String chartDay) throws Exception;

    int insertPosition(CustomerPosition customerPosition);
}
