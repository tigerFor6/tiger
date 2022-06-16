package com.wisdge.cloud.redis;

/**
 * RedisKeys类
 *
 * @author tiger
 * @since: 2021/11/25
 */

public class RedisKeys {
    /**
     * 客户信息Key
     */
    public static String getCustomerListKey() {
        return "customerList";
    }
    /**
     * ods联系信息Key
     */
    public static String getOdsContractKey(String customerId) {
        return "odsContract:" + customerId;
    }

    /**
     * 分红记录Key
     */
    public static String getBonusKey(String customerId) {
        return "bonus:" + customerId;
    }

    /**
     * 交易明细Key
     */
    public static String getAffirmBaseKey(String customerId) {
        return "affirmBase:" + customerId;
    }

    /**
     * 定投计划Key
     */
    public static String getPlanKey(String customerId) {
        return "plan:" + customerId;
    }

    /**
     * 最近账户动态Key
     */
    public static String getRecentAccountDevKey(String customerId) {
        return "recentAccountDev:" + customerId;
    }

    /**
     * 基金产品信息Key
     */
    public static String getFundKey() {
        return "fundInfo";
    }

    /**
     * 基金产品的最新净值信息Key
     */
    public static String getDayFundNavKey() {
        return "dayFundNav";
    }

    /**
     * 基金产品的历史所有净值信息Key
     */
    public static String getFundNavKey() {
        return "fundNav";
    }

    /**
     * 基金渠道信息Key
     */
    public static String getAgencyKey() {
        return "agency";
    }

    /**
     * 美元汇率Key
     */
    public static String getErValueKey() {
        return "erValue";
    }

    /**
     * 历史所有的美元汇率Key
     */
    public static String getAllErValueKey() {
        return "allErValue";
    }

    /**
     * 客户持有基金产品的名称和金额Key
     */
    public static String getHoldFundNameKey(String customerId) {
        return "holdFundNameInfo:" + customerId;
    }

    /**
     * 客户持有基金产品的类型和金额Key
     */
    public static String getHoldFundTypeKey(String customerId) {
        return "holdFundTypeInfo:" + customerId;
    }

    /**
     * 客户持有的渠道和金额Key
     */
    public static String getHoldAgencyKey(String customerId) {
        return "holdAgencyInfo:" + customerId;
    }

    /**
     * 持仓情况-总资产时间曲线数据Key
     */
    public static String getHoldChartInfoKey(String customerId, String timeFlag) {
        return "holdChartInfo:" + customerId + ":" + timeFlag;
    }

    /**
     * 客户持有基金产品详情Key
     */
    public static String getHoldFundDetailKey(String customerId) {
        return "holdFundDetailInfo:" + customerId;
    }

    /**
     * 损益详情-基金产品名称Key
     */
    public static String getFundNameProfitKey(String customerId) {
        return "fundNameProfit:" + customerId;
    }

    /**
     * 损益详情-基金产品类型Key
     */
    public static String getFundTypeProfitKey(String customerId) {
        return "fundTypeProfit:" + customerId;
    }

    /**
     * 损益详情-渠道Key
     */
    public static String getFundAgencyProfitKey(String customerId) {
        return "fundAgencyProfit:" + customerId;
    }

    /**
     * 损益详情-累计收益时间曲线数据Key
     */
    public static String getIncomeChartInfoKey(String customerId, String timeFlag) {
        return "incomeChartInfo:" + customerId + ":" + timeFlag;
    }

    /**
     * 持有基金收益数据Key
     */
    public static String getHoldFundProfitKey(String customerId,String type) {
        return "holdFundProfitInfo:" + customerId + ":" + type;
    }

    /**
     * 企业微信账户分析数据Key
     */
    public static String getWechatKey(String customerId) {
        return "wechatInfo:" + customerId;
    }

    /**
     * 企业微信账户展开分析数据Key
     */
    public static String getWechatDetailKey(String customerId,String fundCode) {
        return "wechatDetailInfo:" + customerId + ":" + fundCode;
    }

    /**
     * 企业微信基金产品累计收益图表数据Key
     */
    public static String getWechatProfitChartKey(String customerId, String fundCode, String chartDay) {
        return "wechatProfitChart:" + customerId + ":" + fundCode + ":" + chartDay;
    }

    /**
     * 企业微信基金产品七日年化收益率图表数据Key
     */
    public static String getWechatWeekChartKey(String fundCode, String chartDay) {
        return "wechatWeekChart:" + fundCode + ":" + chartDay;
    }

    /**
     * 交易账号集合
     * @return
     */
    public static String getTraAccountKey() {
        return "traAccountKey";
    }

    /**
     * 在管客户规模变动信息
     *
     * @param timeFlag 1:按日，2：按周，3：按月
     * @param fundType 1：货币，2：非货币
     * @return
     */
    public static String getFundTypeCountKey(int timeFlag, int fundType) {
        return "fundTypeCountKey:" + timeFlag + ":" + fundType;
    }

    /**
     * 流失客户集合
     * @return
     */
    public static String getLoseCustomerKey() {
        return "loseCustomerKey";
    }
}
