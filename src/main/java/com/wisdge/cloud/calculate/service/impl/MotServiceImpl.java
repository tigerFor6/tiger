package com.wisdge.cloud.calculate.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wisdge.cloud.calculate.dao.*;
import com.wisdge.cloud.calculate.dto.UserCustomerDTO;
import com.wisdge.cloud.calculate.entity.*;
import com.wisdge.cloud.calculate.enums.MotTaksCodeEnum;
import com.wisdge.cloud.calculate.ots.MeteorClientProxy;
import com.wisdge.cloud.calculate.ots.OtsConst;
import com.wisdge.cloud.calculate.service.MotService;

import com.wisdge.cloud.calculate.vo.FundInfoRes;
import com.wisdge.cloud.calculate.vo.HoldFundInfoRes;
import com.wisdge.cloud.redis.RedisKeys;
import com.wisdge.cloud.redis.RedisUtils;
import com.wisdge.cloud.util.DateUtil;
import com.wisdge.utils.StringUtils;
import meteor.api.exception.service.ConnectionNotReadyException;
import meteor.api.exception.service.ResponseFailureException;
import meteor.api.exception.service.UnexpectedException;
import meteor.client4j.handler.ServiceInfo;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * MotServiceImpl
 *
 * @author lsy
 * @date 2021-11-16
 */
@Service("motService")
public class MotServiceImpl implements MotService {

    private static Logger log = LoggerFactory.getLogger(MotServiceImpl.class);

    @Autowired
    private MotDao motDao;
    @Autowired
    private CustomerMotDao customerMotDao;
    @Autowired
    private UserCustomerDao userCustomerDao;
    @Autowired
    private TradeInfoDao tradeInfoDao;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private MeteorClientProxy meteorClientProxy;
    @Autowired
    private OdsCustomerBaseDao odsCustomerBaseDao;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private NoticeDao noticeDao;
    @Autowired
    private NoticeUserDao noticeUserDao;

    /**
     * 身份证过期
     * <p>
     * 1、过期前10天预警提醒； 2、过期当天提醒
     * </p>
     *
     * @return
     */
    @Override
    public List<CustomerMotEntity> idCardExpired(String custId) {
        return motDao.idCardExpired(custId);
    }

    /**
     * 客户生日
     * <p>
     * 1、提前5天提醒；2、客户生日当天提醒
     * </p>
     *
     * @return
     */
    @Override
    public List<CustomerMotEntity> birthday(String custId) {
        return motDao.birthday(custId);
    }

    /**
     * 风险问卷
     * <p>
     * 风险问卷过期
     * 1、提前10天提醒；2、当天提醒
     * </p>
     *
     * @return
     */
    @Override
    public List<CustomerMotEntity> riskRuestionnaire(String custId) {
        return motDao.riskRuestionnaire(custId);
    }


    /**
     * 百万资产
     * <p>
     * 客户资产首次过百万
     * T+1日提醒
     * </p>
     *
     * @param custId
     * @return
     */
    @Override
    public List<CustomerMotEntity> millionAssets(String custId) {
        return motDao.millionAssets(custId);
    }

    /**
     * 流失预警
     * <p>
     * 当前资产小于近一个月最高资产的一半
     * 每周一次
     * </P>
     *
     * @param custId
     * @return
     */
    @Override
    public List<CustomerMotEntity> customerChurnWarning(String custId) {
        return motDao.customerChurnWarning(custId);
    }

    /**
     * 持仓波动
     * <p>
     * 当前持仓金额大于30万&持有收益创近一月新低&持仓收益<20%
     * 每周提示一次
     * </p>
     *
     * @param custId
     * @return
     */
    @Override
    public List<CustomerMotEntity> positionWave(String custId) {
        return motDao.positionWave(custId);
    }

    /**
     * 高点提示
     * <p>
     * 持仓金额大于10万&持有产品收益创近一月新高&持仓收益>20%
     * 每周提示一次
     * </p>
     *
     * @param custId
     * @return
     */
    @Override
    public List<CustomerMotEntity> highPoint(String custId) {
        return motDao.highPoint(custId);
    }

    /**
     * mot任务
     *
     * @param taskId 任务id
     * @return
     * @see MotTaksCodeEnum
     */
    @Override
    public Integer motTask(String taskId) {

        int resultCount = 0;
        MotTaksCodeEnum code = MotTaksCodeEnum.getEnum(taskId);
        if (null == code) {
            log.info("任务ID不存在！");
            return resultCount;
        }

        //所有客户和专员的集合
        List<UserCustomerDTO> userCustomers = userCustomerDao.list();
        if (userCustomers.size() <= 0) {
            log.info("暂无Mot客户！");
            return resultCount;
        }

        resultCount = getResultCount(code, userCustomers);
        return resultCount;
    }

    @Override
    public Integer updateTradeInfo() {
        log.info("mot updateTradeInfo date is {}", DateUtil.formatDate(new Date(),2));
        List<Customer> customers = customerDao.selectList(null);
        List<String> tradingAccounts = new ArrayList<String>();
        Long expire = redisUtils.getExpire(RedisKeys.getTraAccountKey());
        if (expire < 0){
            List<String> tradingAccountList = customers.stream().map(Customer::getTradingAccount).collect(Collectors.toList());
            for (String traAccount : tradingAccountList){
                tradingAccounts.addAll(Arrays.asList(traAccount.split(",")));
            }
            redisUtils.set(RedisKeys.getTraAccountKey(), tradingAccounts, 60 * 60 * 8);
        }else {
            tradingAccounts = redisUtils.get(RedisKeys.getTraAccountKey());
        }
        ServiceInfo serviceBo = new ServiceInfo(OtsConst.SYSTEM_CODE, OtsConst.SERVICE_TEN_MINUTES);
        String result = "";
        try {
            result = meteorClientProxy.callService(serviceBo, null);
        } catch (ResponseFailureException e) {
            e.printStackTrace();
        } catch (ConnectionNotReadyException e) {
            e.printStackTrace();
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }
//        log.info("mot updateTradeInfo result is {}", result);
        JSONObject jsonObject = JSONObject.parseObject(result);
        JSONArray ja = jsonObject.getJSONArray("content");
        List<TradeInfo> tradeInfos = new ArrayList<TradeInfo>();
        for(int i = 0; i < ja.size(); i++) {
            JSONObject childObject = ja.getJSONObject(i);
            String tradeRequestId = childObject.getString("tradeRequestId");
            String tradeAccountNo = childObject.getString("tradeAccountNo");
            if (!tradingAccounts.contains(tradeAccountNo)){
                continue;
            }
            log.info("mot insert tradeInfos tradeRequestId is {}", tradeRequestId);
            QueryWrapper<TradeInfo> queryWrapper = new QueryWrapper<TradeInfo>();
            queryWrapper.eq("trade_request_id", tradeRequestId);
            List<TradeInfo> trades = tradeInfoDao.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(trades)){
                TradeInfo tra = new TradeInfo();
                tra.setTradeRequestId(tradeRequestId);
                tra.setBusinessType(childObject.getString("businessType"));
                tra.setBusinessState(childObject.getString("businessState"));
                tra.setTradeAccountNo(tradeAccountNo);
                tra.setTaAccountNo(childObject.getString("taAccountNo"));
                tra.setFundCode(childObject.getString("fundCode"));
                tra.setTargetFundCode(childObject.getString("targetFundCode"));
                tra.setApplicationAmount(childObject.getString("applicationAmount"));
                tra.setApplicationVolume(childObject.getString("applicationVolume"));
                tra.setTransferState(childObject.getString("transferState"));
                tra.setBusinessTime(DateUtil.formatDate(new Date(),1));
                tradeInfos.add(tra);
            }
        }
        int res = 0;
        if (CollectionUtils.isNotEmpty(tradeInfos)){
            res = tradeInfoDao.addTradeInfos(tradeInfos);
            log.info("updateTradeInfo num is {}", res);
        }
        return res;
    }

    /**
     * 执行成功结果集
     *
     * @param code          任务code
     * @param userCustomers 用户专员关系集合
     * @return
     */
    private int getResultCount(MotTaksCodeEnum code, List<UserCustomerDTO> userCustomers) {
        int resultCount = 0;
        //客户专员关系集合
        Map<Long, List<UserCustomerDTO>> userIds = userCustomers.stream().collect(Collectors.groupingBy((UserCustomerDTO::getCustomerId)));
        for (Map.Entry<Long, List<UserCustomerDTO>> entryUser : userIds.entrySet()) {
            //客户ID
            Long customerId = entryUser.getKey();
            List<UserCustomerDTO> users = entryUser.getValue();
            //ods客户ID
            String custId = users.get(0).getCustId();
            //当前用户mot信息
            List<CustomerMotEntity> list = getCustomerMotEntities(code, custId);
            if (CollectionUtils.isNotEmpty(list)){
                //专员集合
                Map<String, List<UserCustomerDTO>> collect = users.stream().collect(Collectors.groupingBy((UserCustomerDTO::getCustId)));
                List<UserCustomerDTO> userDtos = collect.get(custId);
                for (UserCustomerDTO userCustomerDTO : userDtos) {
                    CustomerMotEntity customerMotEntity = list.get(0);
                    customerMotEntity.setCustomerId(customerId);
                    customerMotEntity.setHandlerId(userCustomerDTO.getUserId());
                    customerMotEntity.setHandler(userCustomerDTO.getFullname());
                    customerMotEntity.setCreateTime(new Date());
                    customerMotDao.insert(customerMotEntity);
                    resultCount++;
                }
            }
        }
        return resultCount;
    }

    /**
     * 根据任务code得到mot的结果集
     *
     * @param code   任务code
     * @param custId ods的客户id
     * @return
     */
    private List<CustomerMotEntity> getCustomerMotEntities(MotTaksCodeEnum code, String custId) {
        List<CustomerMotEntity> list = new ArrayList<>();
        switch (code) {
            case RIGHTS_EXCEED:
                list = rightsExceed(custId,code);
                break;
            case MILLION_ASSETS:
                list = millionAssets(custId);
                break;
            case BIG_PURCHASE:
                list = bigPurchase(custId);
                break;
            case BIG_REDEEM:
                list = bigRedeem(custId);
                break;
            case CUSTOMER_CHURN_WARNING:
                list = customerChurnWarning(custId);
                break;
            case POSITION_WAVE:
                list = positionWave(custId);
                break;
            case HIGH_POINT:
                list = highPoint(custId);
                break;
            case BIRTHDAY:
                list = birthday(custId);
                break;
            case ID_CARD_EXPIRED:
                list = idCardExpired(custId);
                break;
            case RISK_RUESTIONNAIRE:
                list = riskRuestionnaire(custId);
                break;
            default:
                list = new ArrayList<>();
                break;
        }
        return list;
    }

    /**
     * 权益超配
     * <p>
     * 每个风险等级，匹配权益比例。按照超出比例定义高中低
     * T+1提醒
     * </p>
     *
     * @param custId
     * @param code
     * @return
     */
    @Override
    public List<CustomerMotEntity> rightsExceed(String custId, MotTaksCodeEnum code) {
        log.info("rightsExceed enter");
        //TODO 用户总资产， 用户权益类的总资产，最新风险问卷的风险等级
        //1-股票型，2-混合型，8-指数型（指数/被动）
        //权益类总资产
        List<HoldFundInfoRes> holdRightsFund = odsCustomerBaseDao.getRightsFund(custId);
        //总资产
        List<HoldFundInfoRes> holdFund = odsCustomerBaseDao.getHoldFund(custId);
        // 净值
        List<HashMap<String,String>> dayFundNavRedis = redisUtils.get(RedisKeys.getDayFundNavKey());
        // 汇率
        String erValueRedis = redisUtils.get(RedisKeys.getErValueKey());
        //总资产
        float totalAssets = 0;
        //权益总资产
        float totalRightAssets = 0;
        extracted(holdRightsFund, dayFundNavRedis, erValueRedis, totalRightAssets);
        log.info("totalAssets:" + totalAssets);
        extracted(holdFund, dayFundNavRedis, erValueRedis, totalAssets);
        log.info("totalRightAssets:" + totalRightAssets);
        //获取风险等级阈值
        if(totalAssets != 0){
            //风险等级阈值
            String riskValue = null;
            List<Map<String, String>> riskLevelList = odsCustomerBaseDao.getRiskLevel(custId);
            if (!CollectionUtils.isEmpty(riskLevelList)){
                // 风险等级取最新的一条
                Map<String, String> map = riskLevelList.get(0);
                String riskRearingRank = map.get("riskRearingRank");
                log.info("riskRearingRank:" + riskRearingRank);
                String riskLevel = null;
                if(StringUtils.equals("1",riskRearingRank)){
                    riskValue =  "0.9";
                    riskLevel = "低风险";
                }else if(StringUtils.equals("2",riskRearingRank)){
                    riskValue = "0.9";
                    riskLevel = "中低风险";
                }else if(StringUtils.equals("3",riskRearingRank)){
                    riskValue =  "0.9";
                    riskLevel = "中风险";
                }else if(StringUtils.equals("4",riskRearingRank)){
                    riskValue =  "0.9";
                    riskLevel = "中高风险";
                }else if(StringUtils.equals("5",riskRearingRank)){
                    riskValue =  "0.9";
                    riskLevel = "高风险";
                }
                if(StringUtils.isNotEmpty(riskValue)){
                    BigDecimal assetsBigDecimal = new BigDecimal(Float.toString(totalAssets));
                    BigDecimal rightsAssetsBigDecimal = new BigDecimal(Float.toString(totalRightAssets));
                    BigDecimal riskBigDecimal = new BigDecimal(riskValue);
                    BigDecimal divide = assetsBigDecimal.divide(rightsAssetsBigDecimal);
                    if(divide.compareTo(riskBigDecimal) > -1){
                        //插入mot
                        QueryWrapper<Customer> wrapper = new QueryWrapper<>();
                        wrapper.eq("CUSTID",custId);
                        List<Customer> customersList = customerDao.selectList(wrapper);
                        if (!CollectionUtils.isEmpty(customersList)){
                            Customer customer = customersList.get(0);
                            String content= "【权益超配】客户" + customer.getCustomerName() +" 风险等级"+riskLevel+"，权益配置"+riskBigDecimal.multiply(new BigDecimal("100"))+"%，请提示均衡配置";
                            List<UserCustomerDTO> userCustomerList = userCustomerDao.list4CustomerId(Long.valueOf(customer.getId()));
                            if (!CollectionUtils.isEmpty(userCustomerList)){
                                userCustomerList.stream().forEach(userCustomer -> {
                                    log.info("insert mot");
                                    //新增mot
                                    CustomerMotEntity customerMotEntity = new CustomerMotEntity();
                                    customerMotEntity.setCustomerId(Long.valueOf(customer.getId()));
                                    customerMotEntity.setStatus(0);
                                    customerMotEntity.setContent(content);
                                    customerMotEntity.setType(code.getCode());
                                    customerMotEntity.setHandlerId(userCustomer.getUserId());
                                    customerMotEntity.setHandler(userCustomer.getFullname());
                                    customerMotEntity.setCreateTime(new Date());
                                    customerMotDao.insert(customerMotEntity);
                                    log.info("insert notice");
                                    //插入通知
                                    Notice notice = new Notice();
                                    notice.setCatalogId("2");
                                    notice.setStatus(1);
                                    notice.setResult("/MOT");
                                    notice.setSubject(content);
                                    notice.setDelayTime(new Date(new Date().getTime() + 1 * 86400000L));
                                    notice.setContent(content);
                                    notice.setCreateTime(new Date());
                                    noticeDao.insert(notice);
                                    //插入通知人
                                    NoticeUser noticeUser = new NoticeUser();
                                    noticeUser.setNoticeId(notice.getId());
                                    noticeUser.setUserId(String.valueOf(userCustomer.getUserId()));
                                    noticeUserDao.insert(noticeUser);
                                });
                            }
                        }

                    }
                }
            }
        }
        return null;
    }

    private void extracted(List<HoldFundInfoRes> holdFund, List<HashMap<String, String>> dayFundNavRedis, String erValueRedis, float totalAssets) {
        //根据fundcode分类
        Map<String, List<HoldFundInfoRes>> holdFundInfogroup = holdFund.stream().collect(Collectors.groupingBy(HoldFundInfoRes::getFundCode));
        List<String> fundcodeList = new ArrayList<>();
        for (String fundCode : holdFundInfogroup.keySet()){
            fundcodeList.add(fundCode);
        }
        // 所有基金产品信息
        String fundInfo = redisUtils.get(RedisKeys.getFundKey());
        List<FundInfoRes> fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        Map<String, List<FundInfoRes>> fundInfoGroup = fundInfoRes.stream().collect(Collectors.groupingBy(FundInfoRes::getFundCode));

        for (String fundCode : fundcodeList){
            // A,B基金产品
            List<FundInfoRes> resList = fundInfoGroup.get(fundCode);
            if (CollectionUtils.isEmpty(resList)) {
                log.error("持有的基金产品不存在，{}",fundCode);
                continue;
            }
            List<HoldFundInfoRes> holdCodeFund = holdFund.stream().filter(e -> e.getFundCode().equals(fundCode)).collect(Collectors.toList());
            float fundMoney = CustomerServiceImpl.getHoldFundMoney(holdCodeFund, dayFundNavRedis, fundInfoGroup, erValueRedis);
            // 资产总额
            totalAssets += fundMoney;
        }
    }

    /**
     * 大额赎回
     * <p>
     * 赎回金额≥10W提醒，金额为按当日净值估算
     * 每十分钟跑一次
     * </p>
     *
     * @param custId
     * @return
     */
    @Override
    public List<CustomerMotEntity> bigRedeem(String custId) {
        List<CustomerMotEntity> bigRedeemMots = new ArrayList<CustomerMotEntity>();
        // 净值
        List<HashMap<String,String>> dayFundNavRedis = redisUtils.get(RedisKeys.getDayFundNavKey());
        // 汇率
        String erValueRedis = redisUtils.get(RedisKeys.getErValueKey());
        // 所有基金产品信息
        String fundInfo = redisUtils.get(RedisKeys.getFundKey());
        List<FundInfoRes> fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        Map<String, List<FundInfoRes>> fundInfoGroup = fundInfoRes.stream().collect(Collectors.groupingBy(FundInfoRes::getFundCode));
        QueryWrapper<Customer> queryWrapper = new QueryWrapper<Customer>();
        queryWrapper.eq("CUSTID", custId);
        List<Customer> customers = customerDao.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(customers)){
            Customer customer = customers.get(0);
            String tradingAccount = customer.getTradingAccount();
            if (StringUtils.isNotBlank(tradingAccount)){
                List<String> tradeAccounts = Arrays.asList(tradingAccount.split(","));
                QueryWrapper<TradeInfo> qw = new QueryWrapper<TradeInfo>();
                qw.in("trade_account_no", tradeAccounts);
                qw.eq("business_time",DateUtil.formatDate(new Date(), 1));
                qw.gt("application_volume",0);
                List<TradeInfo> tradeInfos = tradeInfoDao.selectList(qw);
                float amount = 0;
                List<String> fundNameDesList = new ArrayList<String>();
                Map<String, List<TradeInfo>> tradeInfoGroup = tradeInfos.stream().collect(Collectors.groupingBy(TradeInfo::getFundCode));
                for (Map.Entry<String, List<TradeInfo>> trade : tradeInfoGroup.entrySet()) {
                    String key = trade.getKey();
                    List<TradeInfo> value = trade.getValue();
                    List<FundInfoRes> resList = fundInfoGroup.get(key);
                    if (CollectionUtils.isEmpty(resList)) {
                        log.error("mot基金产品不存在，{}",key);
                        continue;
                    }
                    String fundName = resList.get(0).getFundName();
                    float volume = 0;
                    for (TradeInfo tradeInfo : value){
                        String applicationVolume = tradeInfo.getApplicationVolume();
                        volume += Float.valueOf(applicationVolume);
                        // 计算金额
                        // 净值
                        List<HashMap<String,String>> dayFundNavList = dayFundNavRedis.stream().filter(e -> e.get("fundcode").equals(key)).collect(Collectors.toList());
                        if (CollectionUtils.isEmpty(dayFundNavList)){
                            log.error("mot基金产品净值不存在，{}",key);
                            continue;
                        }
                        String dayFundNav = String.valueOf(dayFundNavList.get(0).get("dayfundnav"));
                        // currencytypeid 156人民币，840美元
                        String ervalue = "1";
                        String currencyTypeid = resList.get(0).getCurrencyTypeid();
                        if (currencyTypeid.equals("840")) {
                            // 美元汇率
                            ervalue = erValueRedis;
                        }
                        float fundAssets = Float.valueOf(applicationVolume) * Float.parseFloat(dayFundNav) * Float.parseFloat(ervalue);
                        amount += fundAssets;
                    }
                    fundNameDesList.add(fundName + volume + "份");
                }
                if (amount >= 100000){
                    String content = "【大额赎回】 客户"+ customer.getCustomerName() + "于" + DateUtil.formatDate(new Date(), 1) + "申请赎回"+ String.join(",", fundNameDesList) + ",合计" + amount + "元";
                    CustomerMotEntity customerMotEntity = new CustomerMotEntity();
                    customerMotEntity.setContent(content);
                    customerMotEntity.setType(MotTaksCodeEnum.BIG_REDEEM.getCode());
                    bigRedeemMots.add(customerMotEntity);
                }
            }
        }
        return bigRedeemMots;
    }

    /**
     * 大额申购
     * <p>
     * 当日累计申购金额≥10W提醒
     * 每十分钟跑一次
     * </p>
     *
     * @return
     */
    @Override
    public List<CustomerMotEntity> bigPurchase(String custId) {
        List<CustomerMotEntity> bigPurchaseMots = new ArrayList<CustomerMotEntity>();
        // 所有基金产品信息
        String fundInfo = redisUtils.get(RedisKeys.getFundKey());
        List<FundInfoRes> fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        Map<String, List<FundInfoRes>> fundInfoGroup = fundInfoRes.stream().collect(Collectors.groupingBy(FundInfoRes::getFundCode));
        QueryWrapper<Customer> queryWrapper = new QueryWrapper<Customer>();
        queryWrapper.eq("CUSTID", custId);
        List<Customer> customers = customerDao.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(customers)){
            Customer customer = customers.get(0);
            String tradingAccount = customer.getTradingAccount();
            if (StringUtils.isNotBlank(tradingAccount)){
                List<String> tradeAccounts = Arrays.asList(tradingAccount.split(","));
                QueryWrapper<TradeInfo> qw = new QueryWrapper<TradeInfo>();
                qw.in("trade_account_no", tradeAccounts);
                qw.eq("business_time",DateUtil.formatDate(new Date(), 1));
                qw.gt("application_amount",0);
                List<TradeInfo> tradeInfos = tradeInfoDao.selectList(qw);
                float amount = 0;
                List<String> fundNameDesList = new ArrayList<String>();
                Map<String, List<TradeInfo>> tradeInfoGroup = tradeInfos.stream().collect(Collectors.groupingBy(TradeInfo::getFundCode));
                for (Map.Entry<String, List<TradeInfo>> trade : tradeInfoGroup.entrySet()) {
                    String key = trade.getKey();
                    List<TradeInfo> value = trade.getValue();
                    float amountTemp = 0;
                    List<FundInfoRes> resList = fundInfoGroup.get(key);
                    if (CollectionUtils.isEmpty(resList)) {
                        log.error("mot基金产品不存在，{}",key);
                        continue;
                    }
                    String fundName = resList.get(0).getFundName();
                    for (TradeInfo tradeInfo : value){
                        String applicationAmount = tradeInfo.getApplicationAmount();
                        amountTemp += Float.valueOf(applicationAmount);
                        amount += Float.valueOf(applicationAmount);
                    }
                    fundNameDesList.add(fundName + amountTemp + "元");
                }
                if (amount >= 100000){
                    String content = "【大额申购】 客户"+ customer.getCustomerName() + "于" + DateUtil.formatDate(new Date(), 1) + "申请申购"+ String.join(",", fundNameDesList) + ",合计" + amount + "元";
                    CustomerMotEntity customerMotEntity = new CustomerMotEntity();
                    customerMotEntity.setContent(content);
                    customerMotEntity.setType(MotTaksCodeEnum.BIG_PURCHASE.getCode());
                    bigPurchaseMots.add(customerMotEntity);
                }
            }
        }
        return bigPurchaseMots;
    }
}
