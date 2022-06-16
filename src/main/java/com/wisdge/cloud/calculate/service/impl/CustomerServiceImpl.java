package com.wisdge.cloud.calculate.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wisdge.cloud.calculate.constant.ModelDataConstants;
import com.wisdge.cloud.calculate.dao.*;
import com.wisdge.cloud.calculate.entity.*;
import com.wisdge.cloud.calculate.utils.InceptorUtils;
import com.wisdge.cloud.calculate.vo.*;
import com.wisdge.cloud.calculate.service.CustomerService;
import com.wisdge.cloud.redis.RedisKeys;
import com.wisdge.cloud.redis.RedisUtils;
import com.wisdge.cloud.util.DateUtil;
import com.wisdge.cloud.util.FrequenceUtils;
import com.wisdge.cloud.util.HttpClientUtils;
import com.wisdge.utils.SnowflakeIdWorker;
import com.wisdge.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 客户信息service
 *
 * @author tiger
 * @date 2021-11-22
 */
@Service("customerService")
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    @Value("${config.ods.cfCrmApi}")
    private String cfCrmApi;
    @Value("${config.ods.syApi}")
    private String syApi;
    @Value("${config.ods.odsApi}")
    private String odsApi;

    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private CustomerPositionDao customerPositionDao;
    @Autowired
    private CustomerPositionCountDao customerPositionCountDao;
    @Autowired
    private OdsCustomerBaseDao odsCustomerBaseDao;
    @Autowired
    private HdHoldArcDao hdHoldArcDao;
    @Autowired
    private UserCustomerDao userCustomerDao;
    @Autowired
    private CustomerFundAccountDao customerFundAccountDao;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    protected SnowflakeIdWorker snowflakeIdWorker;

    @Override
    public void synCustomerInfo(List<String> custIds) throws Exception {
        long start = System.currentTimeMillis();
        if (CollectionUtils.isEmpty(custIds)){
            custIds = userCustomerDao.getSynCustids();
        }
        List<List<String>> groupList = new ArrayList<>();
        if (custIds.size() > 1000){
            groupList = groupList(custIds);
        }else {
            groupList.add(custIds);
        }
        QueryWrapper<CustomerPosition> queryWrapper = new QueryWrapper();
        queryWrapper.eq("hold_time", DateUtil.formatDate(new Date(), 1));
        List<CustomerPosition> customerPositions = customerPositionDao.selectList(queryWrapper);
        List<CustomerPositionCount> customerPositionCountList = customerPositionCountDao.selectList(null);
        for (List<String> strList : groupList){
            List<Map<String, Object>> odsCustomerList = odsCustomerBaseDao.getOdsCustomer(strList);
            // 定义线程数量为20，可根据服务器配置适当调整大小
            // 定义几个许可
            Semaphore semaphore = new Semaphore(20);
            // 创建一个固定的线程池
            ExecutorService executorService = Executors.newFixedThreadPool(20);
            for (Map<String, Object> odsCustMap : odsCustomerList) {
                try {
                    semaphore.acquire();
                    executorService.execute(() -> {
                        Customer customer = new Customer();
                        customer.setCUSTID(odsCustMap.get("custid") == null ? "" : odsCustMap.get("custid").toString());
                        customer.setCustomerName(odsCustMap.get("ccustfname") == null ? "" : odsCustMap.get("ccustfname").toString());
                        // 知决：证件类型(1:身份证、2:军官证 、3:港澳通行证、4:机动车驾驶证、5:户口本)，要和ods中的字典对应起来
                        // ods个人证件字典表:00：身份证，02军官证，04港澳通行证，05户口本
                        if (odsCustMap.get("custpapertype") != null){
                            String idType = "";
                            String custpapertype = odsCustMap.get("custpapertype").toString();
                            switch (custpapertype){
                                case "00":
                                    idType = "1";
                                    break;
                                case "02":
                                    idType = "2";
                                    break;
                                case "04":
                                    idType = "3";
                                    break;
                                case "05":
                                    idType = "5";
                                    break;
                                default:
                                    idType = "1";
                                    break;
                            }
                            customer.setIdType(idType);
                        }
                        customer.setCustomerType(odsCustMap.get("custtype")== null ? "" : odsCustMap.get("custtype").toString());
                        customer.setIdCard(odsCustMap.get("custpaperno")== null ? "" : odsCustMap.get("custpaperno").toString());
                        customer.setAddress(odsCustMap.get("custaddress")== null ? "" : odsCustMap.get("custaddress").toString());
                        customer.setEmail(odsCustMap.get("custemail")== null ? "" : odsCustMap.get("custemail").toString());
                        customer.setCustPost(odsCustMap.get("custpost")== null ? "" : odsCustMap.get("custpost").toString());
                        customer.setPhone(odsCustMap.get("custmobile")== null ? "" : odsCustMap.get("custmobile").toString());
                        customer.setWorkPhone(odsCustMap.get("custphone")== null ? "" : odsCustMap.get("custphone").toString());
                        customer.setBirthday((Date) odsCustMap.get("custbirthday"));
                        customer.setGender(odsCustMap.get("custsex")== null ? null : odsCustMap.get("custsex").toString());
                        // 不存在就插入
                        QueryWrapper<Customer> customerQueryWrapper = new QueryWrapper<Customer>();
                        customerQueryWrapper.eq("CUSTID", odsCustMap.get("custid"));
                        List<Customer> customers = customerDao.selectList(customerQueryWrapper);
                        if (CollectionUtils.isEmpty(customers)){
                            customer.setCreateTime(new Date());
                            int insert = customerDao.insert(customer);
                            if (insert > 0 ){
                                String id = customer.getId();
                                String custId = customer.getCUSTID();
                                updateBaseInfo(id);
                                try {
                                    updateCustPositionCount(id, custId, customerPositionCountList);
                                    updateCustPosition(id, custId, customerPositions);
                                } catch (Exception e) {
                                    log.info("update exception is: {}", e.getMessage());
                                }
                            }
                        }
                        semaphore.release();
                    });
                } catch (InterruptedException e) {
                    log.info("executor exception is: {}", e.getMessage());
                }
            }
            executorService.shutdown();
        }
        long end = System.currentTimeMillis();
        log.info("初始化用户信息消耗总时间 = {}", end - start);
    }

    @Override
    public void synCustomerInfoJob() throws Exception {
        long start = System.currentTimeMillis();
        // 把所有产品的信息查询出来放入缓存
        List<FundInfoRes> fundInfo = odsCustomerBaseDao.getFundInfo(null);
        redisUtils.set(RedisKeys.getFundKey(), JSON.toJSONString(fundInfo), 60 * 60 * 24);
        // 净值放入缓存
        List<Map<String, Object>> dayFundNavList = odsCustomerBaseDao.getDayFundNav();
        redisUtils.set(RedisKeys.getDayFundNavKey(), dayFundNavList, 60 * 60 * 24);
        // 美元汇率放入缓存
        String erValue = odsCustomerBaseDao.getErvalue();
        redisUtils.set(RedisKeys.getErValueKey(), erValue, 60 * 60 * 24);
        // 渠道放入缓存
        List<Map<String, String>> agencyList = odsCustomerBaseDao.getAgency();
        redisUtils.set(RedisKeys.getAgencyKey(), agencyList, 60 * 60 * 24);
        // 获取所有的客户
        List<Customer> customerList = customerDao.selectList(null);
        QueryWrapper<CustomerPosition> queryWrapper = new QueryWrapper();
        queryWrapper.eq("hold_time", DateUtil.formatDate(new Date(), 1));
        List<CustomerPosition> customerPositions = customerPositionDao.selectList(queryWrapper);
        List<CustomerPositionCount> customerPositionCountList = customerPositionCountDao.selectList(null);
        // 定义线程数量为20，可根据服务器配置适当调整大小
        // 定义几个许可
        Semaphore semaphore = new Semaphore(20);
        // 创建一个固定的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (Customer customer : customerList) {
            try {
                semaphore.acquire();
                executorService.execute(() -> {
                    updateBaseInfo(customer.getId());
                    try {
                        FrequenceUtils.limit(1000, 5);
                        updateCustPosition(customer.getId(), customer.getCUSTID(), customerPositions);
                        updateCustPositionCount(customer.getId(), customer.getCUSTID(), customerPositionCountList);
                    } catch (Exception e) {
                        log.info("update exception is: {}", e.getMessage());
                    }
                    semaphore.release();
                });
            } catch (InterruptedException e) {
                log.info("executor exception is: {}", e.getMessage());
            }
        }
        executorService.shutdown();
        long end = System.currentTimeMillis();
        log.info("synCustomerInfoJob cost = {}", end - start);
    }

    /**
     * 在管客户规模变动数据预处理
     * @throws Exception
     */
    @Override
    public void updateFundTypeCountJob() throws Exception {
        // 认申购，赎回，定投，按日按月，按周的人数统计
        List<String> rsgCodeList = Arrays.asList("020,T220,TA20,TD20,022,T522,T822,TA22,TD22,TD22ZT".split(","));
        List<String> shCodeList = Arrays.asList("024,098,063,TD24,T024".split(","));
        List<String> dtCodeList = Arrays.asList("TA22D,T822D,TD22D,039".split(","));
        Map map = new HashMap();
        // 按日人数统计
        map.put("beginTime", DateUtil.getPastDate(5));
        map.put("endTime", DateUtil.getPastDate(5));
        List<Map<String,Object>> dayFiveCustIds = odsCustomerBaseDao.getFundTypeCount(map);
        map.put("beginTime", DateUtil.getPastDate(4));
        map.put("endTime", DateUtil.getPastDate(4));
        List<Map<String,Object>> dayFourCustIds = odsCustomerBaseDao.getFundTypeCount(map);
        map.put("beginTime", DateUtil.getPastDate(3));
        map.put("endTime", DateUtil.getPastDate(3));
        List<Map<String,Object>> dayThreeCustIds = odsCustomerBaseDao.getFundTypeCount(map);
        map.put("beginTime", DateUtil.getPastDate(2));
        map.put("endTime", DateUtil.getPastDate(2));
        List<Map<String,Object>> dayTwoCustIds = odsCustomerBaseDao.getFundTypeCount(map);
        map.put("beginTime", DateUtil.getPastDate(1));
        map.put("endTime", DateUtil.getPastDate(1));
        List<Map<String,Object>> dayOneCustIds = odsCustomerBaseDao.getFundTypeCount(map);
        // 横坐标
        List<String> xAxisList = new ArrayList<String>();
        xAxisList.add(DateUtil.getPastDate(5));
        xAxisList.add(DateUtil.getPastDate(4));
        xAxisList.add(DateUtil.getPastDate(3));
        xAxisList.add(DateUtil.getPastDate(2));
        xAxisList.add(DateUtil.getPastDate(1));
        for (int i = 1; i < 3; i++) {
            String fundType = "货币型";
            if (i == 2){
                fundType = "非货币型";
            }
            // 认申购按日，货币型人数统计
            Map<String,Object> rsgDayResultMap = dataLoad(fundType,rsgCodeList,xAxisList,dayFiveCustIds,dayFourCustIds,dayThreeCustIds,dayTwoCustIds,dayOneCustIds);
            log.info("rsg dayDataHandle success");
            // 赎回按日人数统计
            Map<String,Object> shDayResultMap = dataLoad(fundType,shCodeList,xAxisList,dayFiveCustIds,dayFourCustIds,dayThreeCustIds,dayTwoCustIds,dayOneCustIds);
            log.info("sh dayDataHandle success");
            // 定投按日人数统计
            Map<String,Object> dtDayResultMap = dataLoad(fundType,dtCodeList,xAxisList,dayFiveCustIds,dayFourCustIds,dayThreeCustIds,dayTwoCustIds,dayOneCustIds);
            log.info("dt dayDataHandle success");
            // 封装结果集放入缓存
            Map<String,Object> resultMap = new LinkedHashMap<String,Object>();
            resultMap.put("rsgMap", rsgDayResultMap);
            resultMap.put("shMap", shDayResultMap);
            resultMap.put("dtMap", dtDayResultMap);
            redisUtils.set(RedisKeys.getFundTypeCountKey(1,i), resultMap);
        }
        // 按周人数统计
        // 获取本周一的时间
        Date firstDayOfWeek = DateUtil.getFirstDayOfWeek(new Date());
        // 当前时间和本周一的差距天数
        long diffDays = DateUtil.getDiffDays(new Date(), firstDayOfWeek);
        // 上周星期一的时间
        String lastWeekMondayOne = DateUtil.getPastDate(7 + (int) diffDays);
        // 上周星期天的时间
        String lastWeekSundayOne = DateUtil.getPastDate(1 + (int) diffDays);
        // 上两周星期一的时间
        String lastWeekMondayTwo = DateUtil.getPastDate(14 + (int) diffDays);
        // 上两周星期天的时间
        String lastWeekSundayTwo = DateUtil.getPastDate(8 + (int) diffDays);
        // 上三周星期一的时间
        String lastWeekMondayThree = DateUtil.getPastDate(21 + (int) diffDays);
        // 上三周星期天的时间
        String lastWeekSundayThree = DateUtil.getPastDate(15 + (int) diffDays);
        // 上四周星期一的时间
        String lastWeekMondayFour = DateUtil.getPastDate(28 + (int) diffDays);
        // 上四周星期天的时间
        String lastWeekSundayFour = DateUtil.getPastDate(22 + (int) diffDays);
        map.put("beginTime", DateUtil.formatDate(firstDayOfWeek, 1));
        map.put("endTime", DateUtil.formatDate(new Date(), 1));
        List<Map<String,Object>> weekOne = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("weekOne sucess");
        map.put("beginTime", lastWeekMondayOne);
        map.put("endTime", lastWeekSundayOne);
        List<Map<String,Object>> weekTwo = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("weekTwo sucess");
        map.put("beginTime", lastWeekMondayTwo);
        map.put("endTime", lastWeekSundayTwo);
        List<Map<String,Object>> weekThree = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("weekThree sucess");
        map.put("beginTime", lastWeekMondayThree);
        map.put("endTime", lastWeekSundayThree);
        List<Map<String,Object>> weekFour = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("weekFour sucess");
        map.put("beginTime", lastWeekMondayFour);
        map.put("endTime", lastWeekSundayFour);
        List<Map<String,Object>> weekFive = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("weekFive sucess");

        xAxisList.clear();
        xAxisList.add(lastWeekMondayFour + "-" + lastWeekSundayFour);
        xAxisList.add(lastWeekMondayThree + "-" + lastWeekSundayThree);
        xAxisList.add(lastWeekMondayTwo + "-" + lastWeekSundayTwo);
        xAxisList.add(lastWeekMondayOne + "-" + lastWeekSundayOne);
        xAxisList.add(DateUtil.formatDate(firstDayOfWeek, 1) + "-" + DateUtil.formatDate(new Date(), 1));
        for (int i = 1; i < 3; i++) {
            String fundType = "货币型";
            if (i == 2) {
                fundType = "非货币型";
            }
            Map<String,Object> rsgWeekResultMap = dataLoad(fundType,rsgCodeList,xAxisList,weekFive,weekFour,weekThree,weekTwo,weekOne);
            log.info("rsg weekDataHandle success");
            Map<String,Object> shWeekResultMap = dataLoad(fundType,shCodeList,xAxisList,weekFive,weekFour,weekThree,weekTwo,weekOne);
            log.info("sh weekDataHandle success");
            Map<String,Object> dtWeekResultMap = dataLoad(fundType,dtCodeList,xAxisList,weekFive,weekFour,weekThree,weekTwo,weekOne);
            log.info("dt weekDataHandle success");
            // 封装结果集放入缓存
            Map<String,Object> resultWeekMap = new LinkedHashMap<String,Object>();
            resultWeekMap.put("rsgMap", rsgWeekResultMap);
            resultWeekMap.put("shMap", shWeekResultMap);
            resultWeekMap.put("dtMap", dtWeekResultMap);
            redisUtils.set(RedisKeys.getFundTypeCountKey(2,i), resultWeekMap);
        }

        // 按月人数统计
        // 获取本月一号的时间
        String currentMonthStartOne = DateUtil.getFirstDayOfMonth(0);
        // 获取上个月一号的时间
        String lastMonthStartOne = DateUtil.getFirstDayOfMonth(-1);
        // 获取上个月最后一天的时间
        String lastMonthEndOne = DateUtil.getEndDayOfMonth(-1);
        // 获取上2个月一号的时间
        String lastMonthStartTwo = DateUtil.getFirstDayOfMonth(-2);
        // 获取上2个月最后一天的时间
        String lastMonthEndTwo = DateUtil.getEndDayOfMonth(-2);
        // 获取上3个月一号的时间
        String lastMonthStartThree = DateUtil.getFirstDayOfMonth(-3);
        // 获取上3个月最后一天的时间
        String lastMonthEndThree = DateUtil.getEndDayOfMonth(-3);
        // 获取上4个月一号的时间
        String lastMonthStartFour = DateUtil.getFirstDayOfMonth(-4);
        // 获取上4个月最后一天的时间
        String lastMonthEndFour = DateUtil.getEndDayOfMonth(-4);
        map.put("beginTime", currentMonthStartOne);
        map.put("endTime", DateUtil.formatDate(new Date(), 1));
        List<Map<String,Object>> monthOne = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("monthOne sucess");
        map.put("beginTime", lastMonthStartOne);
        map.put("endTime", lastMonthEndOne);
        List<Map<String,Object>> monthTwo = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("monthTwo sucess");
        map.put("beginTime", lastMonthStartTwo);
        map.put("endTime", lastMonthEndTwo);
        List<Map<String,Object>> monthThree = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("monthThree sucess");
        map.put("beginTime", lastMonthStartThree);
        map.put("endTime", lastMonthEndThree);
        List<Map<String,Object>> monthFour = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("monthFour sucess");
        map.put("beginTime", lastMonthStartFour);
        map.put("endTime", lastMonthEndFour);
        List<Map<String,Object>> monthFive = odsCustomerBaseDao.getFundTypeCount(map);
        log.info("monthFive sucess");

        xAxisList.clear();
        xAxisList.add(lastMonthStartFour.substring(0, 7));
        xAxisList.add(lastMonthStartThree.substring(0, 7));
        xAxisList.add(lastMonthStartTwo.substring(0, 7));
        xAxisList.add(lastMonthStartOne.substring(0, 7));
        xAxisList.add(currentMonthStartOne.substring(0, 7));
        for (int i = 1; i < 3; i++) {
            String fundType = "货币型";
            if (i == 2) {
                fundType = "非货币型";
            }
            // 按月人数统计
            Map<String,Object> rsgMonthResultMap = dataLoad(fundType,rsgCodeList,xAxisList,monthFive,monthFour,monthThree,monthTwo,monthOne);
            log.info("rsg monthDataHandle success");
            Map<String,Object> shMonthResultMap = dataLoad(fundType,shCodeList,xAxisList,monthFive,monthFour,monthThree,monthTwo,monthOne);
            log.info("sh monthDataHandle success");
            Map<String,Object> dtMonthResultMap = dataLoad(fundType,dtCodeList,xAxisList,monthFive,monthFour,monthThree,monthTwo,monthOne);
            log.info("dt monthDataHandle success");
            // 封装结果集放入缓存
            Map<String,Object> resultMonthMap = new LinkedHashMap<String,Object>();
            resultMonthMap.put("rsgMap", rsgMonthResultMap);
            resultMonthMap.put("shMap", shMonthResultMap);
            resultMonthMap.put("dtMap", dtMonthResultMap);
            redisUtils.set(RedisKeys.getFundTypeCountKey(3,i), resultMonthMap);
        }
    }

    /**
     * 客户流失计算
     * 当前日期为11月9号
     *
     * 情况1：统计8月流失预警，流失预警公式，当前总持仓=8月31号持仓数据，近一个月最高点=8月1号至31号区间的最高点
     * 情况2：统计11月流失预警，流失预警公式，当前总持仓=11月8号（T-1日，实际以上游提供的最新日期为准），进一个月最高点=11月1号至8号区间的最高点
     * 当前总持仓 <  近一月最高点的一半
     *
     * @throws Exception
     */
    @Override
    public void updateCustomerLoseJob() throws Exception {
        // 获取本月一号的时间
        String currentMonthStartOne = DateUtil.getFirstDayOfMonth(0);
        // 获取上个月一号的时间
        String lastMonthStartOne = DateUtil.getFirstDayOfMonth(-1);
        // 获取上个月最后一天的时间
        String lastMonthEndOne = DateUtil.getEndDayOfMonth(-1);
        // 获取上2个月一号的时间
        String lastMonthStartTwo = DateUtil.getFirstDayOfMonth(-2);
        // 获取上2个月最后一天的时间
        String lastMonthEndTwo = DateUtil.getEndDayOfMonth(-2);
        // 获取上3个月一号的时间
        String lastMonthStartThree = DateUtil.getFirstDayOfMonth(-3);
        // 获取上3个月最后一天的时间
        String lastMonthEndThree = DateUtil.getEndDayOfMonth(-3);
        // 获取上4个月一号的时间
        String lastMonthStartFour = DateUtil.getFirstDayOfMonth(-4);
        // 获取上4个月最后一天的时间
        String lastMonthEndFour = DateUtil.getEndDayOfMonth(-4);
        List<String> customerLoseIds1 = userCustomerDao.getCustomerLoseIds(lastMonthStartFour, lastMonthEndFour);
        List<String> customerLoseIds2 = userCustomerDao.getCustomerLoseIds(lastMonthStartThree, lastMonthEndThree);
        List<String> customerLoseIds3 = userCustomerDao.getCustomerLoseIds(lastMonthStartTwo, lastMonthEndTwo);
        List<String> customerLoseIds4 = userCustomerDao.getCustomerLoseIds(lastMonthStartOne, lastMonthEndOne);
        List<String> customerLoseIds5 = userCustomerDao.getCustomerLoseIds(currentMonthStartOne, DateUtil.formatDate(new Date(),1));
        Map<String,Object> resultMap = new HashMap<String,Object>();
        List<String> xAxisList = new ArrayList<String>();
        xAxisList.add(lastMonthStartFour.substring(0,7));
        xAxisList.add(lastMonthStartThree.substring(0,7));
        xAxisList.add(lastMonthStartTwo.substring(0,7));
        xAxisList.add(lastMonthStartOne.substring(0,7));
        xAxisList.add(currentMonthStartOne.substring(0,7));
        List<List<String>> dataList = new ArrayList<List<String>>();
        dataList.add(customerLoseIds1);
        dataList.add(customerLoseIds2);
        dataList.add(customerLoseIds3);
        dataList.add(customerLoseIds4);
        dataList.add(customerLoseIds5);
        resultMap.put("xAxis", xAxisList);
        resultMap.put("data", dataList);
        redisUtils.set(RedisKeys.getLoseCustomerKey(), resultMap);
    }

    private List<String> dataHandle(List<String> codeList, String fundType,List<Map<String,Object>> dayCountList){
        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        if ("货币型".equals(fundType)){
            resultList = dayCountList.stream().
                    filter(e -> codeList.contains(e.get("businesscode").toString()) && fundType.equals(e.get("fundtype").toString())).
                    collect(Collectors.toList());
        }else {
            resultList = dayCountList.stream().
                    filter(e -> codeList.contains(e.get("businesscode").toString()) && (!fundType.equals(e.get("fundtype").toString()))).
                    collect(Collectors.toList());
        }
        Set<String> daySetCustIds = new HashSet<String>();
        List<String> dayCustIds = new ArrayList<String>();
        log.info("resultList is {}", resultList.size());
        if (CollectionUtils.isNotEmpty(resultList)){
            resultList.forEach(k ->{
                daySetCustIds.add(k.get("custid").toString());
            });
        }
        dayCustIds.addAll(daySetCustIds);
        return dayCustIds;
    }

    private Map<String, Object> dataLoad(String fundType, List<String> codeList,List<String> xAxisList, List<Map<String,Object>> dayFiveCustIds,List<Map<String,Object>> dayFourCustIds,List<Map<String,Object>> dayThreeCustIds,List<Map<String,Object>> dayTwoCustIds,List<Map<String,Object>> dayOneCustIds){
        List<String> fiveList = dataHandle(codeList, fundType, dayFiveCustIds);
        List<String> fourList = dataHandle(codeList, fundType, dayFourCustIds);
        List<String> threeList = dataHandle(codeList, fundType, dayThreeCustIds);
        List<String> twoList = dataHandle(codeList, fundType, dayTwoCustIds);
        List<String> oneList = dataHandle(codeList, fundType, dayOneCustIds);
        Map<String, Object> dayMap = new HashMap<String, Object>();
        List<List<String>> dataList = new ArrayList<List<String>>();
        dataList.add(fiveList);
        dataList.add(fourList);
        dataList.add(threeList);
        dataList.add(twoList);
        dataList.add(oneList);
        dayMap.put("xAxis", xAxisList);
        dayMap.put("data", dataList);
        return dayMap;
    }

    @Override
    public void updateHoldArc() throws Exception {
        List<CustomerFundAccount> customerFundAccounts = customerFundAccountDao.selectList(null);
        // 定义线程数量为40，可根据服务器配置适当调整大小
        // 定义几个许可
        Semaphore semaphore = new Semaphore(40);
        // 创建一个固定的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(40);
        for (CustomerFundAccount customerFundAccount: customerFundAccounts){
            try {
                semaphore.acquire();
                executorService.execute(() -> {
                    hdHoldArcDao.addHoldArcs(customerFundAccount.getFundAccount());
                    semaphore.release();
                });
            } catch (InterruptedException e) {
                log.info("executor exception is: {}", e.getMessage());
            }
        }
        executorService.shutdown();
    }

    /*
     * List分割
     */
    public static List<List<String>> groupList(List<String> list) {
        List<List<String>> listGroup = new ArrayList<List<String>>();
        int listSize = list.size();
        // 子集合的长度
        int toIndex = 1000;
        for (int i = 0; i < list.size(); i += toIndex) {
            if (i + toIndex > listSize) {
                toIndex = listSize - i;
            }
            List<String> newList = list.subList(i, i + toIndex);
            listGroup.add(newList);
        }
        return listGroup;
    }

    @Override
    public void historyInterceptor(List<String> custIds) throws Exception {
        List<String> accountList = new ArrayList<String>();
        QueryWrapper<Customer> queryWrapper = new QueryWrapper<Customer>();
        if (CollectionUtils.isNotEmpty(custIds)){
            queryWrapper.in("CUSTID", custIds);
        }
        List<Customer> customerList = customerDao.selectList(queryWrapper);
        for (Customer customer : customerList){
            String fundAccount = customer.getFundAccount();
            if (StringUtils.isNotEmpty(fundAccount)){
                List<String> fundAccountList = Arrays.asList(fundAccount.split(","));
                accountList.addAll(fundAccountList);
            }
        }
        log.info("基金账号集合：{}", accountList.toString());
        // 每5条数据开启一个线程
        int threadSize = 5;
        int remainder = accountList.size()%threadSize;
        //线程数
        int threadNum  = 0;
        if(remainder == 0){
            threadNum  = accountList.size()/threadSize;
        } else {
            threadNum  = accountList.size()/threadSize + 1;
        }
        log.info("线程数：{}", threadNum);
        // 创建一个线程池
        ExecutorService eService = Executors.newFixedThreadPool(threadNum);
        List<Callable<List<Map<String, Object>>>> cList = new ArrayList<>();
        Callable<List<Map<String, Object>>> task = null;
        List<String> sList = null;
        for(int i = 0; i < threadNum; i++){
            if(i == threadNum - 1){
                sList = accountList.subList(i*threadSize, accountList.size());
            } else {
                sList = accountList.subList(i*threadSize, (i+1)*threadSize);
            }
            final List<String> nowList = sList;
            task = new Callable<List<Map<String, Object>>>() {
                @Override
                public List<Map<String, Object>> call() throws Exception {
                    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                    for(int j = 0;j < nowList.size(); j++){
                        String acct_id = nowList.get(j);
                        list.addAll(InceptorUtils.getHistoryData(acct_id));
                    }
                    return list;
                }
            };
            cList.add(task);
        }
        List<Future<List<Map<String, Object>>>> results = eService.invokeAll(cList);
        for(Future<List<Map<String, Object>>> str:results){
            log.info("历史持仓数据结果：{}", str.get());
            List<Map<String, Object>> maps = str.get();
            int insert = userCustomerDao.addHdHoldArcs(maps);
            log.info("历史持仓插入结果：{}", insert);
        }
        eService.shutdown();
    }

    @Override
    public void hdHoldArcToPosition() throws Exception {
        Long expire = redisUtils.getExpire(RedisKeys.getFundNavKey());
        List<Map<String, Object>> fundNavList = new ArrayList<Map<String, Object>>();
        if (expire < 0){
            fundNavList = odsCustomerBaseDao.getFundNav();
            redisUtils.set(RedisKeys.getFundNavKey(), fundNavList, 60 * 60 * 24);
        }else {
            fundNavList = redisUtils.get(RedisKeys.getFundNavKey());
        }
        // 历史所有的美元汇率记录放入缓存
        List<Map<String, Object>> allErValueList = new ArrayList<Map<String, Object>>();
        Long allErValueExpire = redisUtils.getExpire(RedisKeys.getAllErValueKey());
        if (allErValueExpire < 0){
            allErValueList = odsCustomerBaseDao.getAllErvalue();
            redisUtils.set(RedisKeys.getAllErValueKey(), allErValueList, 60 * 60 * 24);
        }else {
            allErValueList = redisUtils.get(RedisKeys.getAllErValueKey());
        }
        // 所有基金产品信息
        String fundInfo = redisUtils.get(RedisKeys.getFundKey());
        List<FundInfoRes> fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        Map<String, List<FundInfoRes>> fundInfoGroup = fundInfoRes.stream().collect(Collectors.groupingBy(FundInfoRes::getFundCode));
        List<Customer> customers = customerDao.selectList(null);
        // 定义线程数量为40，可根据服务器配置适当调整大小
        // 定义几个许可
        Semaphore semaphore = new Semaphore(40);
        // 创建一个固定的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(40);
        for (Customer customer: customers){
            try {
                semaphore.acquire();
                List<Map<String, Object>> finalFundNavList = fundNavList;
                List<Map<String, Object>> finalAllErValueList = allErValueList;
                List<CustomerPosition> cp = new ArrayList<CustomerPosition>();
                QueryWrapper<HdHoldArc> hdHoldArcQueryWrapper = new QueryWrapper<HdHoldArc>();
                hdHoldArcQueryWrapper.eq("customer_id", customer.getId());
                List<HdHoldArc> hdHoldArcs = hdHoldArcDao.selectList(hdHoldArcQueryWrapper);
                if (CollectionUtils.isEmpty(hdHoldArcs)){
                    semaphore.release();
                    continue;
                }
                executorService.execute(() -> {
                    Map<Date, List<HdHoldArc>> listMap = hdHoldArcs.stream().collect(Collectors.groupingBy(HdHoldArc::getConfirmDate));
                    for (Date confirmDate : listMap.keySet()){
                        float fundMoney = 0;
                        // position表中某个客户某天不存在数据，就插入一条数据
                        CustomerPosition position = new CustomerPosition();
                        List<HdHoldArc> HdHoldArcList = listMap.get(confirmDate);
                        Map<String, List<HdHoldArc>> collect = HdHoldArcList.stream().collect(Collectors.groupingBy(HdHoldArc::getFundCode));
                        for (String fundCode : collect.keySet()){
                            float holdShares = 0;
                            float amount = 0;
                            List<HdHoldArc> holds = collect.get(fundCode);
                            for (HdHoldArc hold : holds){
                                String frozenShare = "0";
                                if (StringUtils.isBlank(hold.getFrozenShare())){
                                    frozenShare = "0";
                                }else {
                                    frozenShare = hold.getFrozenShare();
                                }
                                holdShares = holdShares + Float.valueOf(hold.getHoldShare()) + Float.valueOf(frozenShare);
                            }
                            // 获取基金产品某天的净值
                            List<Map<String, Object>> navList = finalFundNavList.stream().filter(e -> e.get("fundcode").equals(fundCode) && e.get("fundnavdate").equals(DateUtil.formatDate(confirmDate,1))).collect(Collectors.toList());
                            if (CollectionUtils.isEmpty(navList)){
                                Map<String, Object> notWorkFundNav = odsCustomerBaseDao.getNotWorkFundNav(fundCode, DateUtil.formatDate(confirmDate, 1));
                                if (notWorkFundNav == null || notWorkFundNav.isEmpty()){
                                    // 持有的基金产品净值不存在,说明是认购，就是1
                                    Map<String, Object> navMap = new HashMap<String, Object>();
                                    navMap.put("dayfundnav","1");
                                    navList.add(navMap);
                                }else {
                                    navList.add(notWorkFundNav);
                                }
                            }
                            // currencytypeid 156人民币，840美元
                            String ervalue = "1";
                            List<FundInfoRes> resList = fundInfoGroup.get(fundCode);
                            if (CollectionUtils.isEmpty(resList)) {
                                log.error("持有的基金产品不存在，{}",fundCode);
                                continue;
                            }
                            String currencyTypeid = resList.get(0).getCurrencyTypeid();
                            if (ModelDataConstants.USRATE.equals(currencyTypeid)) {
                                // 美元汇率
                                List<Map<String, Object>> erdate = finalAllErValueList.stream().filter(e -> e.get("erdate").equals(DateUtil.formatDate(confirmDate, 1))).collect(Collectors.toList());
                                if (CollectionUtils.isNotEmpty(erdate)){
                                    ervalue = String.valueOf(erdate.get(0).get("ervalue"));
                                }else {
                                    List<Map<String, Object>> notWorkErValue = odsCustomerBaseDao.getNotWorkErValue(DateUtil.formatDate(confirmDate, 1));
                                    ervalue = String.valueOf(notWorkErValue.get(0).get("ervalue"));
                                }
                            }
                            String dayFundNav = String.valueOf(navList.get(0).get("dayfundnav"));
                            amount = holdShares * Float.valueOf(dayFundNav) * Float.valueOf(ervalue);
                            fundMoney += amount;
                        }
                        position.setId(String.valueOf(snowflakeIdWorker.nextId()));
                        position.setCustomerId(customer.getId());
                        position.setHoldFund(String.join(",", collect.keySet()));
                        DecimalFormat decimalFormat = new DecimalFormat("0.00");
                        position.setTotalAssets(decimalFormat.format(fundMoney));
                        position.setCreateTime(new Date());
                        position.setHoldTime(DateUtil.formatDate(confirmDate,1));
                        cp.add(position);
                    }
                    if (CollectionUtils.isNotEmpty(cp)){
                        userCustomerDao.addCustomerPositions(cp);
                    }
                    semaphore.release();
                });
            } catch (InterruptedException e) {
                log.info("executor exception is: {}", e.getMessage());
            }
        }
        executorService.shutdown();
    }

    /**
     * 客户经理,网点,基金账号,交易账号,风险等级
     * @param customerId
     * @throws Exception
     */
    @Override
    public void updateBaseInfo(String customerId) {
        // http://10.16.1.137:8180/crm/smStaff/query?custid=F52778E4D253A8F48363E209444F8601
        // content 的name 和dept分别是财富的客户经理和网点
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        String result = null;
        try {
            result = HttpClientUtils.get(cfCrmApi + "?custid=" + custId);
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONObject jb = jsonObject.getJSONObject("content");
            String name = jb.getString("name");
            String dept = jb.getString("dept");
            // 更新客户经理和网点
            customer.setManagerName(name);
            customer.setNetwork(dept);
        } catch (Exception e) {
            log.error("调用获取客户经理和网点的接口异常，{}", e.getMessage());
        }
        // 更新基金账号
        List<String> fundAccountList = odsCustomerBaseDao.getFundAccountId(custId);
        customer.setFundAccount(String.join(",", fundAccountList));
        // 更新交易账号
        List<String> tradeAccountList = odsCustomerBaseDao.getTradeAccount(custId);
        customer.setTradingAccount(String.join(",", tradeAccountList));
        // 更新风险等级
        List<Map<String, String>> riskLevel = odsCustomerBaseDao.getRiskLevel(custId);
        if (!CollectionUtils.isEmpty(riskLevel)){
            // 风险等级取最新的一条
            Map<String, String> map = riskLevel.get(0);
            String riskRearingRank = map.get("riskRearingRank");
            customer.setRiskLevel(riskRearingRank);
        }
        customerDao.updateById(customer);
    }

    /**
     * ods 手机,固定电话,邮箱,邮编，住址
     * @param customerId
     * @throws Exception
     */
    @Override
    public CustViewOdsContactInfoRes getOdsContractInfo(String customerId) {
        // ods的联系信息
        // 调取ods的T009接口
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        String result = null;
        CustViewOdsContactInfoRes odsContract = new CustViewOdsContactInfoRes();
        try {
            result = HttpClientUtils.get(odsApi + "/v1/customer/queryCustInfo?custid=" + custId);
            log.info("odsApi queryCustInfo result is {}", result);
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONObject jb = jsonObject.getJSONObject("data");
            // 手机,固定电话,邮箱,邮编，住址
            odsContract.setMobile(jb.getString("custmobile"));
            odsContract.setPhone(jb.getString("custhomephone") == null ? jb.getString("custworkphone") : jb.getString("custhomephone"));
            odsContract.setEmail(jb.getString("custemail"));
            odsContract.setPostCode(jb.getString("custpost"));
            odsContract.setAddress(jb.getString("custaddress"));
        } catch (Exception e) {
            log.error("获取ods的T009获取联系信息的接口异常,{}", e.getMessage());
            odsContract.setMobile("--");
            odsContract.setPhone("--");
            odsContract.setEmail("--");
            odsContract.setPostCode("--");
            odsContract.setAddress("--");
        }
        redisUtils.set(RedisKeys.getOdsContractKey(customerId), JSON.toJSONString(odsContract), 60 * 60 * 8);
        return odsContract;
    }

    /**
     * 总资产，昨日收益(日收益)，持仓盈亏，持仓收益率，持有产品编码，持有产品名称
     * @param customerId
     * @throws Exception
     */
    @Override
    public void updateCustPosition(String customerId, String custId, List<CustomerPosition> customerPositions) throws Exception {
        // 更新客户持仓信息表customer_position中的数据
        List<CustomerPosition> customerPositionList = customerPositions.stream().filter(e -> e.getCustomerId().equals(customerId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(customerPositionList)){
            // 没有就插入
            CustomerPosition position = new CustomerPosition();
            position.setCustomerId(customerId);
            position = loadPosition(position, odsCustomerBaseDao, customerPositionCountDao, customerId, custId, syApi, redisUtils);
            customerPositionDao.insert(position);
        }else {
            CustomerPosition position = customerPositionList.get(0);
            position = loadPosition(position, odsCustomerBaseDao, customerPositionCountDao, customerId, custId, syApi, redisUtils);
            customerPositionDao.updateById(position);
        }
    }

    /**
     * 批量更新position表
     *
     * @throws Exception
     */
    @Override
    public void updateCustPositions() throws Exception {
        long start = System.currentTimeMillis();
        // 把所有产品的信息查询出来放入缓存
        List<FundInfoRes> fundInfo = odsCustomerBaseDao.getFundInfo(null);
        redisUtils.set(RedisKeys.getFundKey(), JSON.toJSONString(fundInfo), 60 * 60 * 24);
        // 净值放入缓存
        List<Map<String, Object>> dayFundNavList = odsCustomerBaseDao.getDayFundNav();
        redisUtils.set(RedisKeys.getDayFundNavKey(), dayFundNavList, 60 * 60 * 24);
        // 美元汇率放入缓存
        String erValue = odsCustomerBaseDao.getErvalue();
        redisUtils.set(RedisKeys.getErValueKey(), erValue, 60 * 60 * 24);
        // 渠道放入缓存
        List<Map<String, String>> agencyList = odsCustomerBaseDao.getAgency();
        redisUtils.set(RedisKeys.getAgencyKey(), agencyList, 60 * 60 * 24);
        // 客户id和custid集合放入缓存
        List<Map<String,Object>> customerList = new ArrayList<Map<String,Object>>();
        Long customerExpire = redisUtils.getExpire(RedisKeys.getCustomerListKey());
        if (customerExpire < 0){
            customerList = userCustomerDao.getCustIdRal();
            redisUtils.set(RedisKeys.getCustomerListKey(), customerList, 60 * 60 * 8);
        }else {
            customerList = redisUtils.get(RedisKeys.getCustomerListKey());
        }
        QueryWrapper<CustomerPosition> queryWrapper = new QueryWrapper();
        queryWrapper.eq("hold_time", DateUtil.formatDate(new Date(), 1));
        List<CustomerPosition> customerPositions = customerPositionDao.selectList(queryWrapper);
        List<String> upPosCustomerIds = userCustomerDao.getUpPosCustomerIds();
        for (String customerId : upPosCustomerIds){
            try {
                List<Map<String, Object>> ralList = customerList.stream().filter(e -> String.valueOf(e.get("id")).equals(customerId)).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(ralList)){
                    log.info("updateCustPosition id is not exist,{}", customerId);
                    return;
                }
                String custId = String.valueOf(ralList.get(0).get("custid"));
                updateCustPosition(customerId, custId, customerPositions);
            } catch (Exception e) {
                log.info("updateCustPositions,{}, exception is: {}",customerId, e.getMessage());
            }
        }
        long end = System.currentTimeMillis();
        log.info("updateCustPositions cost time = {}", end - start);
    }

    @Override
    public void insertCustPosition(String holdTime) throws Exception {
        // 插入遗漏的持仓数据
        List<Customer> customerList = customerDao.selectList(null);
        QueryWrapper<CustomerPosition> queryWrapper = new QueryWrapper();
        queryWrapper.eq("hold_time", DateUtil.formatDate(new Date(), 1));
        List<CustomerPosition> customerPositions = customerPositionDao.selectList(queryWrapper);
        for (Customer customer : customerList){
            CustomerPosition positionTem = customerPositions.stream().filter(e -> e.getCustomerId().equals(customer.getId())).collect(Collectors.toList()).get(0);
            CustomerPosition position = new CustomerPosition();
            position.setCustomerId(customer.getId());
            position.setCreateTime(new Date());
            position.setHoldTime(holdTime);
            position.setTotalAssets(positionTem.getTotalAssets());
            position.setDirectTotalAssets(positionTem.getDirectTotalAssets());
            position.setDayIncome(positionTem.getDayIncome());
            position.setHoldProfit(positionTem.getHoldProfit());
            position.setProfitRate(positionTem.getProfitRate());
            position.setHoldFund(positionTem.getHoldFund());
            position.setHoldFundName(positionTem.getHoldFundName());
            customerPositionDao.insert(position);
        }
    }

    public static CustomerPosition loadPosition(CustomerPosition position, OdsCustomerBaseDao odsCustomerBaseDao,
                                                CustomerPositionCountDao customerPositionCountDao, String customerId,
                                                String custId, String syApi,RedisUtils redisUtils) {
        // 总资产计算
        // 持有的基金，(持有份额+在途份额) * 净值 *汇率
        List<HoldFundInfoRes> holdFund = odsCustomerBaseDao.getHoldFund(custId);
        Map<String, List<HoldFundInfoRes>> holdFundInfogroup = holdFund.stream().collect(Collectors.groupingBy(HoldFundInfoRes::getFundCode));
        float totalAssets = 0;
        float directTotalAssets = 0;
        float directMoneyTotalAssets = 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        List<String> fundcodeList = new ArrayList<>();
        List<String> fundNameList = new ArrayList<>();
        Map<String,List<String>> fundTypeCodeMap = new HashMap<>();
        String fundcodeStr = "";
        String fundNameStr = "";
        Map<String,String> fundNameMap = new HashMap<>();
        Map<String,String> fundTypeMap = new HashMap<>();
        Map<String,String> agencyMap = new HashMap<>();
        List<CustViewFundDetailRes> fundDetailResList = new ArrayList<>();
        // 净值
        List<HashMap<String,String>> dayFundNavRedis = redisUtils.get(RedisKeys.getDayFundNavKey());
        // 汇率
        String erValueRedis = redisUtils.get(RedisKeys.getErValueKey());
        // 所有基金产品信息
        String fundInfo = redisUtils.get(RedisKeys.getFundKey());
        List<FundInfoRes> fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        Map<String, List<FundInfoRes>> fundInfoGroup = fundInfoRes.stream().collect(Collectors.groupingBy(FundInfoRes::getFundCode));
        // 从渠道1买了基金A,B;从渠道2买了基金A,B,会有4条记录
        for (String fundCode : holdFundInfogroup.keySet()){
            fundcodeList.add(fundCode);
            List<FundInfoRes> resList = fundInfoGroup.get(fundCode);
            if (CollectionUtils.isEmpty(resList)) {
                log.error("持有的基金产品不存在，{}",fundCode);
                continue;
            }
            String fundName = resList.get(0).getFundName();
            if (!fundNameList.contains(fundName)){
                fundNameList.add(fundName);
            }
            String fundType = resList.get(0).getFundType();
            if (fundTypeCodeMap.get(fundType) == null){
                List<String> codes = new ArrayList<>();
                codes.add(fundCode);
                fundTypeCodeMap.put(fundType, codes);
            }else {
                List<String> codes = fundTypeCodeMap.get(fundType);
                codes.add(fundCode);
                fundTypeCodeMap.put(fundType, codes);
            }
            // 持有基金code
            fundcodeStr = String.join(",", fundcodeList);
            // 持有基金名称
            fundNameStr = String.join(",", fundNameList);

            // 封装持有的基金产品详情
            CustViewFundDetailRes fundDetailRes = new CustViewFundDetailRes();
            fundDetailRes.setFundName(fundName);
            List<HoldFundInfoRes> collects = holdFund.stream().filter(e -> e.getFundCode().equals(fundCode)).collect(Collectors.toList());
            String dividendMethod = "";
            float holdShares = 0;
            float midwayVolume = 0;
            String agencyName = "";
            // 获取净值日期和净值
            List<HashMap<String,String>> dayFundNavList = dayFundNavRedis.stream().filter(e -> e.get("fundcode").equals(fundCode)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(dayFundNavList)){
                log.error("持有的基金产品净值不存在，{}",fundCode);
                continue;
            }
            String dayFundNav = String.valueOf(dayFundNavList.get(0).get("dayfundnav"));
            String fundNavDate = dayFundNavList.get(0).get("fundnavdate");
            fundDetailRes.setNetWorth(dayFundNav);
            fundDetailRes.setNetWorthDate(fundNavDate);
            for (HoldFundInfoRes collect : collects){
                dividendMethod = collect.getDividendMethod();
                holdShares += Float.valueOf(collect.getHoldShares());
                midwayVolume += Float.valueOf(collect.getMidwayVolume());
                String agencyCode = collect.getAgencyCode();
                List<Map<String,String>> agencyRedis = redisUtils.get(RedisKeys.getAgencyKey());
                // 如果这个产品从多个渠道购买,暂时取一个渠道展示
                agencyName = agencyRedis.stream().filter(e -> e.get("agencycode").equals(agencyCode)).collect(Collectors.toList()).get(0).get("agencyname");
            }
            // 金额计算的是多个渠道的总和
            float fundMoney = getHoldFundMoney(collects, dayFundNavRedis, fundInfoGroup, erValueRedis);
            fundDetailRes.setBalance(decimalFormat.format(fundMoney));
            fundDetailRes.setWay(dividendMethod);
            fundDetailRes.setInstitution(agencyName);
            fundDetailRes.setHoldPortion(String.valueOf(holdShares));
            fundDetailRes.setTodoPortion(String.valueOf(midwayVolume));
            fundDetailResList.add(fundDetailRes);
        }
        // fundDetailResList,基金持仓详情放入缓存
        redisUtils.set(RedisKeys.getHoldFundDetailKey(customerId), JSON.toJSONString(fundDetailResList), 60 * 60 * 24);
        // 把持有的基金产品，按照名称，类型，渠道放入缓存
        for (String fundCode : fundcodeList){
            // A,B基金产品
            List<FundInfoRes> resList = fundInfoGroup.get(fundCode);
            if (CollectionUtils.isEmpty(resList)) {
                log.error("持有的基金产品不存在，{}",fundCode);
                continue;
            }
            String fundName = resList.get(0).getFundName();
            List<HoldFundInfoRes> holdCodeFund = holdFund.stream().filter(e -> e.getFundCode().equals(fundCode)).collect(Collectors.toList());
            float fundMoney = getHoldFundMoney(holdCodeFund, dayFundNavRedis, fundInfoGroup, erValueRedis);
            // 资产总额
            totalAssets += fundMoney;
            List<HoldFundInfoRes> directHoldCodeFund = holdCodeFund.stream().filter(e -> ModelDataConstants.DIRECTCODE.equals(e.getAgencyCode())).collect(Collectors.toList());
            // 直销资产总额
            directTotalAssets += getHoldFundMoney(directHoldCodeFund, dayFundNavRedis, fundInfoGroup, erValueRedis);
            // 直销资产总额区分货币类型和非货币类型存储
            List<String> mCodes = fundTypeCodeMap.get("货币型");
            if (!CollectionUtils.isEmpty(mCodes)){
                List<HoldFundInfoRes> directHoldMoneyCodeFund = directHoldCodeFund.stream().filter(e -> mCodes.contains(e.getFundCode())).collect(Collectors.toList());
                directMoneyTotalAssets += getHoldFundMoney(directHoldMoneyCodeFund, dayFundNavRedis, fundInfoGroup, erValueRedis);
            }
            if (StringUtils.isNotBlank(fundName)){
                fundNameMap.put(fundName, decimalFormat.format(fundMoney));
                // 客户持有的基金名称和对应的金额放入缓存
                redisUtils.set(RedisKeys.getHoldFundNameKey(customerId), fundNameMap, 60 * 60 * 24);
            }
        }
        // 产品类型
        for (String fundType : fundTypeCodeMap.keySet()){
            List<String> codes = fundTypeCodeMap.get(fundType);
            List<HoldFundInfoRes> holdTypeFund = holdFund.stream().filter(e -> codes.contains(e.getFundCode())).collect(Collectors.toList());
            float typeMoney = getHoldFundMoney(holdTypeFund, dayFundNavRedis, fundInfoGroup, erValueRedis);
            if (StringUtils.isNotBlank(fundType)){
                fundTypeMap.put(fundType, decimalFormat.format(typeMoney));
                // 客户持有的基金名称和对应的金额放入缓存
                redisUtils.set(RedisKeys.getHoldFundTypeKey(customerId), fundTypeMap, 60 * 60 * 24);
            }
        }
        // 渠道
        Map<String, List<HoldFundInfoRes>> agencyGroup = holdFund.stream().collect(Collectors.groupingBy(HoldFundInfoRes::getAgencyCode));
        for (String agencyCode : agencyGroup.keySet()){
            List<HoldFundInfoRes> holdAgencyFund = agencyGroup.get(agencyCode);
            float agencyMoney = getHoldFundMoney(holdAgencyFund, dayFundNavRedis, fundInfoGroup, erValueRedis);
            List<Map<String,String>> agencyRedis = redisUtils.get(RedisKeys.getAgencyKey());
            String agencyName = agencyRedis.stream().filter(e -> e.get("agencycode").equals(agencyCode)).collect(Collectors.toList()).get(0).get("agencyname");
            if (StringUtils.isNotBlank(agencyName)){
                agencyMap.put(agencyName, decimalFormat.format(agencyMoney));
                // 客户持有的渠道名称和对应的金额放入缓存
                redisUtils.set(RedisKeys.getHoldAgencyKey(customerId), agencyMap, 60 * 60 * 24);
            }
        }
        // 保留2位小数
        position.setTotalAssets(decimalFormat.format(totalAssets));
        position.setDirectTotalAssets(decimalFormat.format(directTotalAssets));
        // 直销货币资产
        position.setDirectMoneyTotalAssets(decimalFormat.format(directMoneyTotalAssets));
        position.setHoldFund(fundcodeStr);
        position.setHoldFundName(fundNameStr);
        position.setCreateTime(new Date());
        position.setHoldTime(DateUtil.formatDate(new Date(), 1));
        // 调用损益t023接口拿到累计收益，持有收益，昨日收益，/v1/query4CRM
        List<String> fundAccountList = odsCustomerBaseDao.getSyFundAccountId(custId);
        // select FUNDACCOUNTID from ODS_FUNDACCT where CUSTID='20090805000012015261123' and  taid in('TA','ZD','S8');
        // 调取损益接口的时候，传其中一个就行了，损益接口是按人来拿取数据的
        if (!CollectionUtils.isEmpty(fundAccountList)) {
            String result = null;
            try {
                result = HttpClientUtils.get(syApi + "/v1/query4CRM?fundaccountid=" + fundAccountList.get(0));
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONObject jb = jsonObject.getJSONObject("data");
                // 累计收益
                String allIncome = jb.getString("allIncome");
                QueryWrapper<CustomerPositionCount> queryWrapper = new QueryWrapper();
                queryWrapper.eq("CUSTOMER_ID", customerId);
                List<CustomerPositionCount> customerPositionCounts = customerPositionCountDao.selectList(queryWrapper);
                if (!CollectionUtils.isEmpty(customerPositionCounts)){
                    CustomerPositionCount customerPositionCount = customerPositionCounts.get(0);
                    customerPositionCount.setTotalProfit(allIncome);
                    customerPositionCountDao.updateById(customerPositionCount);
                }
                // 持有收益
                Float holdIncome = jb.getFloat("holdIncome");
                // 日收益
                Float dayIncome = jb.getFloat("dayIncome");
                position.setHoldProfit(decimalFormat.format(holdIncome));
                position.setDayIncome(decimalFormat.format(dayIncome));
                // 持仓盈亏率
                position.setProfitRate(jb.getString("holdIncomeRate"));
            } catch (Exception e) {
                log.error("调用获取累计收益，持有收益，日收益的接口异常,{}", e.getMessage());
            }
        }
        return position;
    }


    /**
     * 获取持有金额
     * @param holdFundInfoRes
     * @param dayFundNavRedis
     * @param fundInfoGroup
     * @param erValueRedis
     * @return
     */
    public static float getHoldFundMoney(List<HoldFundInfoRes> holdFundInfoRes,List<HashMap<String,String>> dayFundNavRedis,
                                         Map<String, List<FundInfoRes>> fundInfoGroup, String erValueRedis){
        float fundMoney = 0;
        for (HoldFundInfoRes res : holdFundInfoRes){
            String fundCode = res.getFundCode();
            String holdshares = res.getHoldShares();;
            String midwayvolume = "";
            if (StringUtils.isEmpty(res.getMidwayVolume())){
                midwayvolume = "0";
            }else {
                midwayvolume = res.getMidwayVolume() ;
            }
            float shares = Float.parseFloat(holdshares) + Float.parseFloat(midwayvolume);
            // 净值
            List<HashMap<String,String>> dayFundNavList = dayFundNavRedis.stream().filter(e -> e.get("fundcode").equals(fundCode)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(dayFundNavList)){
                log.error("持有的基金产品净值不存在，{}",fundCode);
                continue;
            }
            String dayFundNav = String.valueOf(dayFundNavList.get(0).get("dayfundnav"));
            // currencytypeid 156人民币，840美元
            String ervalue = "1";
            List<FundInfoRes> resList = fundInfoGroup.get(fundCode);
            if (CollectionUtils.isEmpty(resList)) {
                log.error("持有的基金产品不存在，{}",fundCode);
                continue;
            }
            String currencyTypeid = resList.get(0).getCurrencyTypeid();
            if (ModelDataConstants.USRATE.equals(currencyTypeid)) {
                // 美元汇率
                ervalue = erValueRedis;
            }
            float fundAssets = shares * Float.parseFloat(dayFundNav) * Float.parseFloat(ervalue);
            fundMoney += fundAssets;
        }
        return fundMoney;
    }

    /**
     * 资产峰值，峰值时间，最近交易时间，近一年交易次数,最近一年交易金额 ,存到customer_position_count表中
     * @param customerId
     * @param custId
     * @throws Exception
     */
    @Override
    public void updateCustPositionCount(String customerId, String custId, List<CustomerPositionCount> customerPositionCountList) throws Exception {
        // 最近交易时间，近一年交易次数,最近一年交易金额
        List<CustomerPositionCount> customerPositionCounts = customerPositionCountList.stream().filter(e -> e.getCustomerId().equals(customerId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(customerPositionCounts)){
            // 客户资产峰值表为空就插入一条新数据
            CustomerPositionCount customerPositionCount = new CustomerPositionCount();
            customerPositionCount.setCustomerId(customerId);
            customerPositionCount = loadPositionCount(customerPositionCount, customerId, custId, odsCustomerBaseDao, userCustomerDao);
            customerPositionCount.setCreateTime(new Date());
            customerPositionCount.setUpdateTime(new Date());
            customerPositionCountDao.insert(customerPositionCount);
        }else{
            CustomerPositionCount customerPositionCount = customerPositionCounts.get(0);
            if (DateUtil.formatDate(customerPositionCount.getUpdateTime(),1).equals(DateUtil.formatDate(new Date(),1))){
                // 今天更新过的数据，今天就不再更新处理了
                return;
            }
            customerPositionCount = loadPositionCount(customerPositionCount, customerId, custId, odsCustomerBaseDao, userCustomerDao);
            customerPositionCount.setUpdateTime(new Date());
            customerPositionCountDao.updateById(customerPositionCount);
        }
    }

    @Override
    public void updateCustPositionCounts() throws Exception {
        long start = System.currentTimeMillis();
        // 客户id和custid集合放入缓存
        List<Map<String,Object>> customerList = new ArrayList<Map<String,Object>>();
        Long customerExpire = redisUtils.getExpire(RedisKeys.getCustomerListKey());
        if (customerExpire < 0){
            customerList = userCustomerDao.getCustIdRal();
            redisUtils.set(RedisKeys.getCustomerListKey(), customerList, 60 * 60 * 8);
        }else {
            customerList = redisUtils.get(RedisKeys.getCustomerListKey());
        }
        List<String> upPosCustomerIds = userCustomerDao.getUpPosCustomerIds();
        List<CustomerPositionCount> customerPositionCountList = customerPositionCountDao.selectList(null);
        for (String customerId : upPosCustomerIds){
            try {
                List<Map<String, Object>> ralList = customerList.stream().filter(e -> String.valueOf(e.get("id")).equals(customerId)).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(ralList)){
                    log.info("updateCustPosition id is not exist,{}", customerId);
                    return;
                }
                String custId = String.valueOf(ralList.get(0).get("custid"));
                updateCustPositionCount(customerId, custId, customerPositionCountList);
            } catch (Exception e) {
                log.info("updateCustPositionCounts,{}, exception is: {}",customerId, e.getMessage());
            }
        }
        long end = System.currentTimeMillis();
        log.info("updateCustPositions cost time = {}", end - start);
    }

    public static CustomerPositionCount loadPositionCount(CustomerPositionCount customerPositionCount, String customerId,String custId,
                                                          OdsCustomerBaseDao odsCustomerBaseDao, UserCustomerDao userCustomerDao){
        Map<String, Object> traInfo = odsCustomerBaseDao.getTraInfo(custId, DateUtil.getPastDate(365));
        String lastYearTradingNum = traInfo.get("lastyeartradingnum") == null ? null : traInfo.get("lastyeartradingnum").toString();
        Float lastYearTradingMoney = Float.valueOf(traInfo.get("lastyeartradingmoney") == null ? "0" : traInfo.get("lastyeartradingmoney").toString());
        // 峰值时间精确到年月日
        if (traInfo.get("latesttradingtime") != null){
            customerPositionCount.setLastTradTime((Date) traInfo.get("latesttradingtime"));
        }
        customerPositionCount.setLastYearTradNum(lastYearTradingNum);
        //直销最近一年交易次数
        Map<String, Object> directTraInfo = odsCustomerBaseDao.getDirectTraInfo(custId, DateUtil.getPastDate(365));
        String directLastYearTradingNum = directTraInfo.get("directlastyeartradingnum") == null ? null : directTraInfo.get("directlastyeartradingnum").toString();
        customerPositionCount.setDirectLastYearTradNum(directLastYearTradingNum);
        // 保留2位小数
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        customerPositionCount.setLastYearTradMoney(decimalFormat.format(lastYearTradingMoney));
        // 资产峰值，峰值时间
        List<Map<String, Object>> peakInfo = userCustomerDao.getPeakAssets(customerId);
        if (CollectionUtils.isNotEmpty(peakInfo)){
            String peakAmount = peakInfo.get(0).get("totalAssets") == null ? "" : peakInfo.get(0).get("totalAssets").toString();
            String holdTime = peakInfo.get(0).get("holdTime").toString();
            customerPositionCount.setPeakDate(holdTime);
            customerPositionCount.setAssetPeak(peakAmount);
        }
        List<Map<String, Object>> directPeakInfo = userCustomerDao.getDirectPeakAssets(customerId);
        if (CollectionUtils.isNotEmpty(directPeakInfo)){
            String directPeakAmount = directPeakInfo.get(0).get("directTotalAssets") == null ? "" : directPeakInfo.get(0).get("directTotalAssets").toString();
            customerPositionCount.setDirectAssetPeak(directPeakAmount);
        }
        return customerPositionCount;
    }

    /**
     * 获取每个客户的分红记录，前端展示是按照机构
     * @param customerId
     * @throws Exception
     */
    @Override
    public List<CustViewBonusRecordRes> getBonusInfo(String customerId) {
        // 分红数据
        // 客户id和custid集合放入缓存
        List<Map<String,Object>> customerList = new ArrayList<Map<String,Object>>();
        Long customerExpire = redisUtils.getExpire(RedisKeys.getCustomerListKey());
        if (customerExpire < 0){
            customerList = userCustomerDao.getCustIdRal();
            redisUtils.set(RedisKeys.getCustomerListKey(), customerList, 60 * 60 * 8);
        }else {
            customerList = redisUtils.get(RedisKeys.getCustomerListKey());
        }
        List<Map<String, Object>> ralList = customerList.stream().filter(e -> String.valueOf(e.get("id")).equals(customerId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ralList)){
            log.info("getAffirmBaseInfo id is not exist,{}", customerId);
            return null;
        }
        String custId = String.valueOf(ralList.get(0).get("custid"));
        String result = null;
        List<CustViewBonusRecordRes> bonusList = new ArrayList<>();
        try {
            result = HttpClientUtils.get(odsApi + "/v1/bonusDetail/query4CRM?custid=" + custId + "&pageSize=100000");
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONObject data = jsonObject.getJSONObject("data");
            JSONArray ja = data.getJSONArray("list");
            for(int i = 0; i < ja.size(); i++) {
                JSONObject childObject = ja.getJSONObject(i);
                // 确认日期,基金名称,分红方式,分配金额,分配份额,交易机构
                String confirmDate = childObject.getString("confirmdate");
                String fundName = childObject.getString("fundname");
                String fundCode = childObject.getString("fundcode");
                String wayId = childObject.getString("dividendmethod");
                String way = childObject.getString("dividendmethodname");
                String distributionAmount = childObject.getString("dividendtotalmoney");
                String distributionPortion = childObject.getString("reinvestvolume");
                // 机构代码，前端展示是按照机构名称来分组展示
                String agencyCode = childObject.getString("agencycode");
                String tradingInstitution = childObject.getString("agencyname");
                CustViewBonusRecordRes bonusRecordRes = new CustViewBonusRecordRes();
                bonusRecordRes.setConfirmDate(confirmDate);
                bonusRecordRes.setFundName(fundName);
                bonusRecordRes.setFundCode(fundCode);
                bonusRecordRes.setWayId(wayId);
                bonusRecordRes.setWay(way);
                bonusRecordRes.setDistributionAmount(distributionAmount);
                bonusRecordRes.setDistributionPortion(distributionPortion);
                bonusRecordRes.setAgencyCode(agencyCode);
                bonusRecordRes.setTradingInstitution(tradingInstitution);
                bonusList.add(bonusRecordRes);
            }
        } catch (Exception e) {
            log.error("获取分红数据ods的T012接口异常,{}", e.getMessage());
            CustViewBonusRecordRes bonusRecordRes = new CustViewBonusRecordRes();
            bonusRecordRes.setConfirmDate("--");
            bonusRecordRes.setFundCode("--");
            bonusRecordRes.setFundName("--");
            bonusRecordRes.setWayId("--");
            bonusRecordRes.setWay("--");
            bonusRecordRes.setDistributionAmount("--");
            bonusRecordRes.setDistributionPortion("--");
            bonusRecordRes.setAgencyCode("--");
            bonusRecordRes.setTradingInstitution("--");
            bonusList.add(bonusRecordRes);
            redisUtils.set(RedisKeys.getBonusKey(customerId), JSON.toJSONString(bonusList), 60);
            return bonusList;
        }
        redisUtils.set(RedisKeys.getBonusKey(customerId), JSON.toJSONString(bonusList), 60 * 60 * 8);
        return bonusList;
    }

    /**
     * 获取交易明细
     * @param customerId
     * @throws Exception
     * @return
     */
    @Override
    public List<CustViewTraRecordRes> getAffirmBaseInfo(String customerId) {
        List<CustViewTraRecordRes> affirmBaseList = new ArrayList<>();
        // 客户id和custid集合放入缓存
        List<Map<String,Object>> customerList = new ArrayList<Map<String,Object>>();
        Long customerExpire = redisUtils.getExpire(RedisKeys.getCustomerListKey());
        if (customerExpire < 0){
            customerList = userCustomerDao.getCustIdRal();
            redisUtils.set(RedisKeys.getCustomerListKey(), customerList, 60 * 60 * 8);
        }else {
            customerList = redisUtils.get(RedisKeys.getCustomerListKey());
        }
        List<Map<String, Object>> ralList = customerList.stream().filter(e -> String.valueOf(e.get("id")).equals(customerId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ralList)){
            log.info("getAffirmBaseInfo id is not exist,{}", customerId);
            return null;
        }
        String custId = String.valueOf(ralList.get(0).get("custid"));
        // 获取用户的基金账号列表
        List<String> fundAccountIdList = odsCustomerBaseDao.getFundAccountId(custId);
        // 循环得到每个基金账号的交易明细，随便一个基金账号去查询都是所有的交易记录
        if (CollectionUtils.isNotEmpty(fundAccountIdList)){
            String result = null;
            try {
                result = HttpClientUtils.get(odsApi + "/v1/affirmBase/queryListWeb?fundaccountid=" + fundAccountIdList.get(0)+ "&pageSize=100000");
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONObject data = jsonObject.getJSONObject("data");
                JSONArray ja = data.getJSONArray("data");
                for(int i = 0; i < ja.size(); i++) {
                    JSONObject childObject = ja.getJSONObject(i);
                    // 申请日期,确认日期,交易类型,基金名称,申请金额,申请份额,确认金额,确认份额,交易机构，付款方式，手续费，下单时间(企微)，申购时间(企微)
                    String applyDate = childObject.getString("applicationdate");
                    String confirmDate = childObject.getString("confirmdate");
                    String transactionType = childObject.getString("businessname");
                    String fundCode = childObject.getString("fundcode");
                    String fundName = childObject.getString("fundname");
                    String applyAccount = childObject.getString("applicationamount");
                    String confirmAccount = childObject.getString("confirmamount");
                    String confirmPortion = childObject.getString("confirmvolume");
                    String agencyName = childObject.getString("agencyname");
                    String fundAccountId = childObject.getString("fundaccountid");
                    // 机构代码，前端展示是按照机构名称来分组展示
                    String agencyCode = childObject.getString("agencycode");
                    String applyPortion = childObject.getString("applicationvolume");
                    // 交易流水号
                    // 去交易申请表b_ds_tr_trade_request关联流水号trade_request_id 找PAYMENT_CHANNEL_ID,再关联b_ds_ac_payment_channel pament_channel_id
                    String application = childObject.getString("applicationno");
                    String costAmount = childObject.getString("tradefee");
                    CustViewTraRecordRes traDetail = new CustViewTraRecordRes();
                    traDetail.setFundAccountId(fundAccountId);
                    traDetail.setApplyDate(applyDate);
                    traDetail.setConfirmDate(confirmDate);
                    traDetail.setTransactionType(transactionType);
                    traDetail.setFundCode(fundCode);
                    traDetail.setFundName(fundName);
                    traDetail.setApplyAccount(applyAccount);
                    traDetail.setApplyPortion(applyPortion);
                    traDetail.setConfirmAccount(confirmAccount);
                    traDetail.setConfirmPortion(confirmPortion);
                    traDetail.setAgencyCode(agencyCode);
                    traDetail.setTradingInstitution(agencyName);
                    traDetail.setCostAmount(costAmount);
                    Map<String, Object> tradeRequestInfo = odsCustomerBaseDao.getTradeRequestInfo(application);
                    // 付款方式
                    if (tradeRequestInfo != null){
                        String way = tradeRequestInfo.get("name") == null ? "" : tradeRequestInfo.get("name").toString();
                        // 下单时间(企微)
                        String businessTime = tradeRequestInfo.get("businessTime") == null ? "" : tradeRequestInfo.get("businessTime").toString();
                        traDetail.setWay(way);
                        traDetail.setBusinessTime(businessTime);
                    }
                    affirmBaseList.add(traDetail);
                }
            } catch (Exception e) {
                log.error("调用获取交易记录的ods接口T005异常,{}", e.getMessage());
                CustViewTraRecordRes traDetail = new CustViewTraRecordRes();
                traDetail.setFundAccountId(fundAccountIdList.get(0));
                traDetail.setApplyDate("--");
                traDetail.setConfirmDate("--");
                traDetail.setTransactionType("--");
                traDetail.setFundCode("--");
                traDetail.setFundName("--");
                traDetail.setApplyAccount("--");
                traDetail.setApplyPortion("--");
                traDetail.setConfirmAccount("--");
                traDetail.setConfirmPortion("--");
                traDetail.setAgencyCode("--");
                traDetail.setTradingInstitution("--");
                traDetail.setWay("--");
                traDetail.setCostAmount("--");
                traDetail.setBusinessTime("--");
                affirmBaseList.add(traDetail);
                redisUtils.set(RedisKeys.getAffirmBaseKey(customerId), JSON.toJSONString(affirmBaseList), 60);
                return affirmBaseList;
            }
        }
        redisUtils.set(RedisKeys.getAffirmBaseKey(customerId), JSON.toJSONString(affirmBaseList), 60 * 60 * 8);
        return affirmBaseList;
    }

    /**
     * 每个交易账户的定投计划（定投信息-数据表b_ds_te_trade_plan，累计定投金额，已投期数-数据表b_ds_tr_trade_request，持有收益-损益接口t026）
     * @throws Exception
     */
    @Override
    public List<CustViewInvestPlanRes> getPlanInfo(String customerId) {
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<CustViewInvestPlanRes> planList = new ArrayList<>();
        List<String> tradeAccountList = odsCustomerBaseDao.getTradeAccount(custId);
        // 每个交易账户的定投计划
        for (String tradeAccountNo : tradeAccountList){
            List<Map<String, Object>> planInfos = odsCustomerBaseDao.getPlanInfo(tradeAccountNo);
            for (Map<String,Object> planInfo : planInfos){
                String tradePlanId = planInfo.get("tradePlanId") == null ? "":planInfo.get("tradePlanId").toString();
                Map<String, Object> planInfoExp = odsCustomerBaseDao.getPlanInfoExp(tradePlanId);
                // 定投扩展信息，从其他表获取
                String totalPlanMount = planInfoExp.get("totalplanmount") == null ? "" : String.valueOf(planInfoExp.get("totalplanmount"));
                String periods = String.valueOf(planInfoExp.get("periods"));
                if (Integer.valueOf(periods) < 1){
                    continue;
                }
                // 定投信息
                String fundName = planInfo.get("fundName") == null ? "": planInfo.get("fundName").toString();
                String planAmount = planInfo.get("amount") == null ? "": planInfo.get("amount").toString();
                String deductChannel = planInfo.get("capitalChannelName") == null ? "": planInfo.get("capitalChannelName").toString();
                String status = planInfo.get("status") == null ? "": planInfo.get("status").toString();
                if (ModelDataConstants.PAUSE.equals(status)){
                    status = "暂停";
                }else if (ModelDataConstants.STOP.equals(status)){
                    status = "中止";
                }else if (ModelDataConstants.RUN.equals(status)){
                    status = "正在定投";
                }
                // 定投时间不取nextTradeDate，修改为取startTime
                String planDate = planInfoExp.get("startTime") == null ? "": planInfoExp.get("startTime").toString();
                String actualEndDate = planInfoExp.get("endTime") == null ? DateUtil.formatDate(new Date(), 1): planInfoExp.get("endTime").toString();
                String target = planInfo.get("profitRate") == null ? "": planInfo.get("profitRate").toString();
                CustViewInvestPlanRes planInfoRes = new CustViewInvestPlanRes();
                planInfoRes.setTradePlanId(tradePlanId);
                planInfoRes.setFundName(fundName);
                // 累计定投
                planInfoRes.setTotalPlanMount(totalPlanMount);
                // 定投金额
                planInfoRes.setPlanMount(planAmount);
                // 已投期数
                planInfoRes.setPeriods(periods);
                planInfoRes.setDeductChannel(deductChannel);
                planInfoRes.setStatus(status);
                planInfoRes.setPlanDate(planDate);
                // 定投总天数
                Long diffDays = 0L;
                if (StringUtils.isNotEmpty(planDate)){
                    diffDays = DateUtil.getDiffDays(DateUtil.parseUtilDate(actualEndDate, 1), DateUtil.parseUtilDate(planDate, 1));
                }
                planInfoRes.setPlanDay(String.valueOf(diffDays));
                DecimalFormat df = new DecimalFormat("0.00%");
                if (StringUtils.isNotBlank(target)){
                    target = df.format(Float.valueOf(target));
                }
                planInfoRes.setTarget(target);
                String result = null;
                try {
                    result = HttpClientUtils.get(syApi + "/v1/query4CRM/dt?plancode=" + tradePlanId);
                    JSONObject jsonObject = JSONObject.parseObject(result);
                    JSONObject jb = jsonObject.getJSONObject("data");
                    // 定投持有收益，从损益接口t026中获取
                    String holdIncome = jb.getString("holdIncome");
                    //持有收益
                    planInfoRes.setProfit(holdIncome);
                    // 金额=累计定投+持有收益
                    float amount = Float.parseFloat(totalPlanMount) + Float.parseFloat(holdIncome);
                    planInfoRes.setAmount(String.valueOf(amount));
                } catch (Exception e) {
                    log.info("调用定投收益接口异常，{}", e.getMessage());
                    planInfoRes.setProfit("--");
                    planInfoRes.setAmount("--");
                }finally {
                    planList.add(planInfoRes);
                }
            }
        }
        redisUtils.set(RedisKeys.getPlanKey(customerId), JSON.toJSONString(planList), 60 * 60 * 8);
        return planList;
    }

    /**
     * 获取最近动态中，账户信息变动信息
     * @param customerId
     * @return
     * @throws Exception
     */
    @Override
    public List<CustViewRecentDevelpRes> getAccountChangeInfo(String customerId) throws Exception {
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<CustViewRecentDevelpRes> recentDevList = new ArrayList<>();
        List<Map<String, Object>> accountChangeInfos = odsCustomerBaseDao.getAccountChangeInfo(custId);
        for (Map<String,Object> accountChangeInfo : accountChangeInfos){
            String businessname = accountChangeInfo.get("businessname") == null ? "" : accountChangeInfo.get("businessname").toString();
            String confirmamount = accountChangeInfo.get("confirmamount") == null ? "" : accountChangeInfo.get("confirmamount").toString();
            String confirmdate = accountChangeInfo.get("confirmdate") == null ? "" : accountChangeInfo.get("confirmdate").toString();
            CustViewRecentDevelpRes recentDev = new CustViewRecentDevelpRes();
            // 账户动态
            recentDev.setDevelpDate(confirmdate);
            recentDev.setType("5");
            String content = businessname + "金额" + confirmamount;
            recentDev.setContent(content);
            recentDevList.add(recentDev);
        }
        redisUtils.set(RedisKeys.getRecentAccountDevKey(customerId), JSON.toJSONString(recentDevList), 60 * 60 * 8);
        return recentDevList;
    }

    /**
     * 损益详情
     * @param customerId
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getFundProfitInfo(String customerId, String holdType) {
        // holdType:持仓类型(1:分产品类型，2：分产品，3：分渠道)
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<String> fundAccountList = odsCustomerBaseDao.getSyFundAccountId(custId);
        String result = null;
        // 所有基金产品信息
        List<FundInfoRes> fundInfoRes = new ArrayList<FundInfoRes>();
        Long fundExpire = redisUtils.getExpire(RedisKeys.getFundKey());
        if (fundExpire < 0){
            fundInfoRes = odsCustomerBaseDao.getFundInfo(null);
            redisUtils.set(RedisKeys.getFundKey(), JSON.toJSONString(fundInfoRes));
        }else {
            String fundInfo = redisUtils.get(RedisKeys.getFundKey());
            fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        }
        Map<String, List<FundInfoRes>> fundInfoGroup = fundInfoRes.stream().collect(Collectors.groupingBy(FundInfoRes::getFundCode));
        Map<String, Object> resultMap = new HashMap<String, Object>();
        if (!CollectionUtils.isEmpty(fundAccountList)) {
            if ("1".equals(holdType) || "2".equals(holdType)){
                try {
                    // 产品名称损益，损益接口t011
                    result = HttpClientUtils.get(syApi + "/v1/interval/total?fundaccountid=" + fundAccountList.get(0) + "&queryType=2"+ "&beginDate=" + DateUtil.getCurrYearFirst() + "&endDate=" + DateUtil.formatDate(new Date(), 1));
                    JSONObject jsonObject = JSONObject.parseObject(result);
                    JSONObject data = jsonObject.getJSONObject("data");
                    JSONArray total = data.getJSONArray("total");
                    JSONObject childObject = total.getJSONObject(0);
                    // 今年以来累计盈亏,今年以来的收益率
                    String totalProfit = childObject.getString("income");
                    String rate = childObject.getString("incomeRate");
                    JSONArray fundList = data.getJSONArray("fundList");
                    List<Map> list = new ArrayList<>();
                    Map<String,String> typeMap = new HashMap<String,String>();
                    for (int i = 0; i < fundList.size(); i++) {
                        JSONObject jb = fundList.getJSONObject(i);
                        String fundCode = jb.getString("fundCode");
                        String income = jb.getString("income");
                        if (StringUtils.isEmpty(income)){
                            continue;
                        }
                        Map map = new HashMap();
                        if ("1".equals(holdType)){
                            // 获取基金类型
                            List<FundInfoRes> resList = fundInfoGroup.get(fundCode);
                            if (!CollectionUtils.isEmpty(resList)){
                                String fundType = resList.get(0).getFundType();
                                if (typeMap.containsKey(fundType)){
                                    Float aFloat = Float.valueOf(typeMap.get(fundType));
                                    aFloat += Float.valueOf(income);
                                    income = String.valueOf(aFloat);
                                }
                                typeMap.put(fundType, income);
                            }
                        }else {
                            // 拿到基金的名称
                            List<FundInfoRes> collect = fundInfoRes.stream().filter(e -> e.getFundCode().equals(fundCode)).collect(Collectors.toList());
                            if (!CollectionUtils.isEmpty(collect)){
                                map.put("name", collect.get(0).getFundName());
                                map.put("value", income);
                                list.add(map);
                            }
                        }
                    }
                    if ("1".equals(holdType)){
                        for (String fundType : typeMap.keySet()){
                            Map map = new HashMap();
                            map.put("name", fundType);
                            map.put("value", typeMap.get(fundType));
                            list.add(map);
                        }
                    }
                    resultMap.put("chart", list);
                    resultMap.put("totalProfit", totalProfit);
                    resultMap.put("rate", rate);
                } catch (Exception e) {
                    log.error("调用损益t011接口异常,{}", e.getMessage());
                    resultMap.put("chart", "--");
                    resultMap.put("totalProfit", "--");
                    resultMap.put("rate", "--");
                }
                // 分产品的损益详情数据放入缓存
                if ("1".equals(holdType)){
                    redisUtils.set(RedisKeys.getFundTypeProfitKey(customerId), resultMap, 60 * 60 * 8);
                }else {
                    redisUtils.set(RedisKeys.getFundNameProfitKey(customerId), resultMap, 60 * 60 * 8);
                }
            }
            if ("3".equals(holdType)){
                // 渠道损益，通过损益t024接口
                try {
                    result = HttpClientUtils.get(syApi + "/v1/query4CRM/agency?fundaccountid=" + fundAccountList.get(0) + "&beginDate=" + DateUtil.getCurrYearFirst() + "&endDate=" + DateUtil.formatDate(new Date(), 1));
                    JSONObject jsonObject = JSONObject.parseObject(result);
                    JSONArray data = jsonObject.getJSONArray("data");
                    List<Map> list = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++) {
                        JSONObject jb = data.getJSONObject(i);
                        String agencyCode = jb.getString("agencyCode");
                        String income = jb.getString("income");
                        Map map = new HashMap();
                        // 渠道放入缓存
                        Long agencyExpire = redisUtils.getExpire(RedisKeys.getAgencyKey());
                        if (agencyExpire < 0){
                            List<Map<String, String>> agencyList = odsCustomerBaseDao.getAgency();
                            redisUtils.set(RedisKeys.getAgencyKey(), agencyList, 60 * 60 * 8);
                        }
                        List<Map<String,String>> agencyRedis = redisUtils.get(RedisKeys.getAgencyKey());
                        String agencyName = agencyRedis.stream().filter(e -> e.get("agencycode").equals(agencyCode)).collect(Collectors.toList()).get(0).get("agencyname");
                        if (StringUtils.isNotBlank(agencyName)){
                            map.put("name", agencyName);
                            map.put("value", income);
                            list.add(map);
                        }
                    }
                    Map<String, Object> redisMap = redisUtils.get(RedisKeys.getFundNameProfitKey(customerId));
                    String totalProfit = redisMap.get("totalProfit").toString();
                    String rate = redisMap.get("rate").toString();
                    resultMap.put("chart", list);
                    resultMap.put("totalProfit", totalProfit);
                    resultMap.put("rate", rate);
                } catch (Exception e) {
                    log.error("调用损益t024接口异常,{}", e.getMessage());
                    resultMap.put("chart", "--");
                    resultMap.put("totalProfit", "--");
                    resultMap.put("rate", "--");
                }
                // 分产品的损益详情数据放入缓存
                redisUtils.set(RedisKeys.getFundAgencyProfitKey(customerId), resultMap, 60 * 60 * 8);
            }
        }
        return resultMap;
    }

    /**
     * 累计收益曲线表数据
     * @param customerId
     * @param startTime
     * @param endTime
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> getIncomeChartInfo(String customerId, String startTime, String endTime,String timeFlag){
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<String> fundAccountList = odsCustomerBaseDao.getSyFundAccountId(custId);
        String result = null;
        List<String> calTimeList = new ArrayList<String>();
        // timeFlag,3:近三天,7:近一周,30：近一个月，90：近三个月，180：近半年
        if (StringUtils.isNotEmpty(timeFlag)){
            startTime = DateUtil.getPastDate(Integer.valueOf(timeFlag));
            endTime = DateUtil.getPastDate(1);
        }
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)){
            calTimeList = DateUtil.getBetweenDate(startTime,endTime);
        }
        Map<String,Object> resultMap = new HashMap<String, Object>();
        if (!CollectionUtils.isEmpty(fundAccountList)){
            // 调用损益t025接口获取
            try {
                result = HttpClientUtils.get(syApi + "/v1/query4CRM/interval?fundaccountid=" + fundAccountList.get(0) + "&beginDate=" + startTime + "&endDate=" + endTime);
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONArray data = jsonObject.getJSONArray("data");
                List<String> incomesList = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jb = data.getJSONObject(i);
                    String allIncome = jb.getString("allIncome");
                    incomesList.add(allIncome);
                }
                Map map = new HashMap();
                map.put("type", "累计收益");
                map.put("income", incomesList);
                List<Map> series = new ArrayList<>();
                series.add(map);
                resultMap.put("calTime", calTimeList);
                resultMap.put("series", series);
            } catch (Exception e) {
                log.error("调用损益t025接口异常,{}", e.getMessage());
                resultMap.put("calTime", calTimeList);
                resultMap.put("series", "--");
            }
            // 累计收益时间曲线图数据放入缓存
            if (StringUtils.isNotEmpty(timeFlag)){
                redisUtils.set(RedisKeys.getIncomeChartInfoKey(customerId, timeFlag), resultMap, 60 * 60 * 8);
            }
        }
        return resultMap;
    }

    /**
     * 持有基金收益详情数据，平均成本，持有总投入，最新市值
     * @param customerId
     * @param type
     * @return
     * @throws Exception
     */
    @Override
    public List<CustViewFundProfitDetailRes> getFundProfitDetail(String customerId, String type) {
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<String> fundAccountList = odsCustomerBaseDao.getSyFundAccountId(custId);
        // 所有基金产品信息
        List<FundInfoRes> fundInfoRes = new ArrayList<FundInfoRes>();
        Long fundExpire = redisUtils.getExpire(RedisKeys.getFundKey());
        if (fundExpire < 0){
            fundInfoRes = odsCustomerBaseDao.getFundInfo(null);
            redisUtils.set(RedisKeys.getFundKey(), JSON.toJSONString(fundInfoRes));
        }else {
            String fundInfo = redisUtils.get(RedisKeys.getFundKey());
            fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        }
        Long dayFundNavExpire = redisUtils.getExpire(RedisKeys.getDayFundNavKey());
        if (dayFundNavExpire < 0){
            List<Map<String, Object>> dayFundNav = odsCustomerBaseDao.getDayFundNav();
            redisUtils.set(RedisKeys.getDayFundNavKey(), dayFundNav, 60 * 60 * 8);
        }
        List<HashMap<String,String>> dayFundNavRedis = redisUtils.get(RedisKeys.getDayFundNavKey());
        List<HoldFundInfoRes> holdFundList = odsCustomerBaseDao.getHoldFund(custId);
        String result = null;
        List<CustViewFundProfitDetailRes> fundProfitDetailList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fundAccountList)){
            // 调用损益t003_v2接口服务，获取投入和持有平均
            try {
                result = HttpClientUtils.get(syApi + "/v2/mainPage/assets/allFund?fundaccountid=" + fundAccountList.get(0));
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jb = data.getJSONObject(i);
                    String fundCode = jb.getString("fundcode");
                    // 持有收益
                    String holdIncome = jb.get("holdIncome") == null ? null : String.valueOf(jb.get("holdIncome"));
                    // 最新市值
                    String value = jb.get("value") == null ? null : String.valueOf(jb.get("value"));
                    String holdNav = "";
                    String holdCost = "";
                    String dayFundNav = "";
                    String fundNavDate = "";
                    String fundName = "";
                    String volume = "";
                    try {
                        String result16 = HttpClientUtils.get(syApi + "/v1/fund/estimate/allAgency?fundaccountid=" + fundAccountList.get(0) + "&fundcode=" + fundCode);
                        JSONObject jsonObject16 = JSONObject.parseObject(result16);
                        JSONArray data16 = jsonObject16.getJSONArray("data");
                        if (data16.size() > 0){
                            JSONObject jb16 = data16.getJSONObject(0);
                            // 平均成本
                            holdNav = jb16.get("holdNav") == null ? null : String.valueOf(jb16.get("holdNav"));
                            // 持有总投入
                            holdCost = jb16.get("holdCost") == null ? null : String.valueOf(jb16.get("holdCost"));
                        }
                    }catch (Exception e){
                        log.error("调用损益t016接口异常,{}", e.getMessage());
                        holdNav = "--";
                        holdCost = "--";
                    }
                    // 净值日期,基金份额净值
                    List<HashMap<String,String>> dayFundNavList = dayFundNavRedis.stream().filter(e -> e.get("fundcode").equals(fundCode)).collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(dayFundNavList)){
                        log.error("持有的基金产品净值不存在，{}",fundCode);
                        dayFundNav = "--";
                        fundNavDate = "--";
                    }else {
                        dayFundNav = String.valueOf(dayFundNavList.get(0).get("dayfundnav"));
                        fundNavDate = dayFundNavList.get(0).get("fundnavdate");
                    }
                    // 拿到基金的名称
                    List<FundInfoRes> collect = fundInfoRes.stream().filter(e -> e.getFundCode().equals(fundCode)).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(collect)){
                        fundName = collect.get(0).getFundName();
                    }
                    List<HoldFundInfoRes> collects = holdFundList.stream().filter(e -> e.getFundCode().equals(fundCode)).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(collects)){
                        volume = collects.get(0).getHoldShares();
                    }
                    CustViewFundProfitDetailRes fundProfitDetailRes = new CustViewFundProfitDetailRes();
                    fundProfitDetailRes.setFundCode(fundCode);
                    fundProfitDetailRes.setFundName(fundName);
                    fundProfitDetailRes.setHoldPortion(volume);
                    fundProfitDetailRes.setAverageCost(holdNav);
                    fundProfitDetailRes.setHoldInvestment(holdCost);
                    fundProfitDetailRes.setNetWorthDate(fundNavDate);
                    fundProfitDetailRes.setNetWorth(dayFundNav);
                    fundProfitDetailRes.setLatestMarket(value);
                    fundProfitDetailRes.setProfit(holdIncome);
                    // 封装在list中
                    fundProfitDetailList.add(fundProfitDetailRes);
                }
            }catch (Exception e){
                log.error("调用损益t003_v2接口异常,{}", e.getMessage());
                CustViewFundProfitDetailRes fundProfitDetailRes = new CustViewFundProfitDetailRes();
                fundProfitDetailRes.setFundCode("--");
                fundProfitDetailRes.setFundName("--");
                fundProfitDetailRes.setHoldPortion("--");
                fundProfitDetailRes.setAverageCost("--");
                fundProfitDetailRes.setHoldInvestment("--");
                fundProfitDetailRes.setNetWorthDate("--");
                fundProfitDetailRes.setNetWorth("--");
                fundProfitDetailRes.setLatestMarket("--");
                fundProfitDetailRes.setProfit("--");
                fundProfitDetailList.add(fundProfitDetailRes);
            }
            if ("0".equals(type) ){
                // 持有基金收益详情
                fundProfitDetailList = fundProfitDetailList.stream().filter(e -> StringUtils.isNotEmpty(e.getHoldPortion())).collect(Collectors.toList());
            }
        }
        // 持有基金收益数据放入缓存
        redisUtils.set(RedisKeys.getHoldFundProfitKey(customerId, type), JSON.toJSONString(fundProfitDetailList), 60 * 60 * 8);
        return fundProfitDetailList;
    }

    /**
     * 企微-账户分析
     * @param customerId
     * @return
     * @throws Exception
     */
    @Override
    public List<Map<String, Object>> getWechatFundDetail(String customerId){
        // 损益系统t003_v2
        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<String> fundAccountList = odsCustomerBaseDao.getSyFundAccountId(custId);
        // 所有基金产品信息
        List<FundInfoRes> fundInfoRes = new ArrayList<FundInfoRes>();
        Long fundExpire = redisUtils.getExpire(RedisKeys.getFundKey());
        if (fundExpire < 0){
            fundInfoRes = odsCustomerBaseDao.getFundInfo(null);
            redisUtils.set(RedisKeys.getFundKey(), JSON.toJSONString(fundInfoRes));
        }else {
            String fundInfo = redisUtils.get(RedisKeys.getFundKey());
            fundInfoRes = JSON.parseArray(fundInfo, FundInfoRes.class);
        }
        Long dayFundNavExpire = redisUtils.getExpire(RedisKeys.getDayFundNavKey());
        if (dayFundNavExpire < 0){
            List<Map<String, Object>> dayFundNav = odsCustomerBaseDao.getDayFundNav();
            redisUtils.set(RedisKeys.getDayFundNavKey(), dayFundNav, 60 * 60 * 8);
        }
        List<HashMap<String,String>> dayFundNavRedis = redisUtils.get(RedisKeys.getDayFundNavKey());
        String result = null;
        if (!CollectionUtils.isEmpty(fundAccountList)){
            try {
                result = HttpClientUtils.get(syApi + "/v2/mainPage/assets/allFund?fundaccountid=" + fundAccountList.get(0));
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jb = data.getJSONObject(i);
                    String fundcode = jb.getString("fundcode");
                    String value = jb.get("value") == null ? null : String.valueOf(jb.get("value"));
                    String dayIncome = jb.getString("dayIncome");
                    String deadLine = jb.getString("deadLine");
                    String holdIncome = jb.getString("holdIncome");
                    String holdIncomeRate = jb.get("holdIncomeRate") == null ? null : String.valueOf(jb.get("holdIncomeRate"));
                    String allIncome = jb.getString("allIncome");
                    String allIncomeRate = jb.getString("allIncomeRate");
                    String dayUpDown = jb.get("dayUpDown") == null ? null : String.valueOf(jb.get("dayUpDown"));
                    // 净值，七日年化率(货币基金才有值)，万份日收益(货币基金才有值)
                    String dayFundNav = "";
                    String fundNavDate = "";
                    String weekProfitRatio = "";
                    String myriaddayProfit = "";
                    String accumulativeNav = "";
                    String fundName = "";
                    // 净值日期,基金份额净值
                    List<HashMap<String,String>> dayFundNavList = dayFundNavRedis.stream().filter(e -> e.get("fundcode").equals(fundcode)).collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(dayFundNavList)){
                        log.error("持有的基金产品净值不存在，{}",fundcode);
                        dayFundNav = "--";
                        fundNavDate = "--";
                        weekProfitRatio = "--";
                        myriaddayProfit = "--";
                        accumulativeNav = "--";
                    }else {
                        dayFundNav = String.valueOf(dayFundNavList.get(0).get("dayfundnav"));
                        fundNavDate = dayFundNavList.get(0).get("fundnavdate");
                        weekProfitRatio = String.valueOf(dayFundNavList.get(0).get("weekprofitratio"));
                        myriaddayProfit = String.valueOf(dayFundNavList.get(0).get("myriaddayprofit"));
                        accumulativeNav = String.valueOf(dayFundNavList.get(0).get("accumulativenav"));
                    }
                    // 拿到基金的名称
                    List<FundInfoRes> collect = fundInfoRes.stream().filter(e -> e.getFundCode().equals(fundcode)).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(collect)){
                        fundName = collect.get(0).getFundName();
                    }
                    Map<String,Object> resultMap = new HashMap<String,Object>();
                    resultMap.put("fundCode",fundcode);
                    resultMap.put("fundName",fundName);
                    resultMap.put("amount",value);
                    resultMap.put("profit",dayIncome);
                    resultMap.put("profitDate",deadLine);
                    resultMap.put("holdIncome",holdIncome);
                    resultMap.put("holdIncomeRate",holdIncomeRate);
                    resultMap.put("allIncome",allIncome);
                    resultMap.put("allIncomeRate",allIncomeRate);
                    resultMap.put("dayFundNav",dayFundNav);
                    resultMap.put("dayUpDown",dayUpDown);
                    resultMap.put("accumulativeNav",accumulativeNav);
                    resultMap.put("weekProfitRatio",weekProfitRatio);
                    resultMap.put("myriaddayProfit",myriaddayProfit);
                    // 调用损益系统t002获取持仓成本
                    try {
                        String holdNavResult = HttpClientUtils.get(syApi + "/v1/fundPage/total?fundaccountid=" + fundAccountList.get(0) + "&fundcode=" + fundcode);
                        JSONObject holdNavJb = JSONObject.parseObject(holdNavResult).getJSONObject("data");
                        String holdNav = holdNavJb.getString("holdNav");
                        resultMap.put("holdNav",holdNav);
                    }catch (Exception e){
                        log.error("调用损益t002接口异常,{}", e.getMessage());
                        resultMap.put("holdNav","--");
                    }
                    resultList.add(resultMap);
                }
            } catch (Exception e) {
                log.error("调用损益t003_v2接口异常,{}", e.getMessage());
                Map<String,Object> resultMap = new HashMap<String,Object>();
                resultMap.put("fundCode","--");
                resultMap.put("fundName","--");
                resultMap.put("amount","--");
                resultMap.put("profit","--");
                resultMap.put("profitDate","--");
                resultMap.put("holdIncome","--");
                resultMap.put("holdIncomeRate","--");
                resultMap.put("allIncome","--");
                resultMap.put("allIncomeRate","--");
                resultMap.put("dayFundNav","--");
                resultMap.put("dayUpDown","--");
                resultMap.put("accumulativeNav","--");
                resultMap.put("weekProfitRatio","--");
                resultMap.put("myriaddayProfit","--");
                resultMap.put("holdNav","--");
                resultList.add(resultMap);
            }
        }
        redisUtils.set(RedisKeys.getWechatKey(customerId), resultList, 60 * 60 * 8);
        return resultList;
    }

    /**
     * 企微-账户分析-展开详情
     * @param customerId
     * @param fundCode
     * @return
     */
    @Override
    public List<Map<String, Object>> getWechatAgencyFundDetail(String customerId, String fundCode) {
        // 损益t004接口
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<String> fundAccountList = odsCustomerBaseDao.getSyFundAccountId(custId);
        String result = null;
        List<Map<String,Object>> resultList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fundAccountList)){
            try {
                result = HttpClientUtils.get(syApi + "/v1/fundPage/detailByAgency?fundaccountid=" + fundAccountList.get(0) + "&fundcode=" + fundCode);
                log.info("企业微信账户分析展开详情调用syapi t004接口 result:{}", result);
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jb = data.getJSONObject(i);
                    // 机构代码，机构名称，市值（资产），持有份额，累计收益，持有收益，昨日收益
                    String agencyCode = jb.getString("agencyCode");
                    String agencyName = jb.getString("agencyName");
                    String value = jb.getString("value");
                    String volume = jb.getString("volume");
                    String allIncome = jb.getString("allIncome");
                    String holdIncome = jb.getString("holdIncome");
                    String dayIncome = jb.getString("dayIncome");
                    Map<String,Object> map = new HashMap<>();
                    map.put("agencyCode", agencyCode);
                    map.put("agencyName", agencyName);
                    map.put("value", value);
                    map.put("volume", volume);
                    map.put("allIncome", allIncome);
                    map.put("holdIncome", holdIncome);
                    map.put("dayIncome", dayIncome);
                    resultList.add(map);
                }
            }catch (Exception e){
                log.error("企业微信账户分析展开详情调用syapi t004接口,{}", e.getMessage());
                Map<String,Object> map = new HashMap<>();
                map.put("agencyCode", "--");
                map.put("agencyName", "--");
                map.put("value", "--");
                map.put("volume", "--");
                map.put("allIncome", "--");
                map.put("holdIncome", "--");
                map.put("dayIncome", "--");
                resultList.add(map);
            }
        }
        redisUtils.set(RedisKeys.getWechatDetailKey(customerId, fundCode), resultList, 60 * 60 * 8);
        return resultList;
    }

    /**
     * 企微中某个基金产品的累计收益图表
     * @param customerId
     * @param fundCode
     * @param chartDay
     * @return
     * @throws Exception
     */
    @Override
    public List<Map<String, Object>> getWechatProfitChart(String customerId, String fundCode, String chartDay) throws Exception {
        // chartDay,3:近三天,7:近一周,30：近一个月，90：近三个月，180：近半年
        String beginDate = DateUtil.getPastDate(Integer.valueOf(chartDay));
        String endDate = DateUtil.getPastDate(1);
        // 损益接口t005
        Customer customer = customerDao.selectById(customerId);
        String custId = customer.getCUSTID();
        List<String> fundAccountList = odsCustomerBaseDao.getSyFundAccountId(custId);
        String result = null;
        List<Map<String,Object>> resultList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fundAccountList)){
            try {
                result = HttpClientUtils.get(syApi + "/v1/query/historyByFund?fundaccountid=" + fundAccountList.get(0) + "&fundcode=" + fundCode + "&beginDate=" + beginDate + "&endDate=" + endDate);
                log.info("企业微信账户分析展开详情调用syapi t005接口 result:{}", result);
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jb = data.getJSONObject(i);
                    // 时间，累计收益
                    String deadLine = jb.getString("deadLine");
                    String allIncome = jb.getString("allIncome");
                    Map<String,Object> map = new HashMap<>();
                    map.put("time", deadLine);
                    map.put("tem", allIncome);
                    resultList.add(map);
                }
            }catch (Exception e){
                log.error("企业微信账户分析展开详情调用syapi t005接口,{}", e.getMessage());
                Map<String,Object> map = new HashMap<>();
                map.put("time", "--");
                map.put("tem", "--");
                resultList.add(map);
            }
        }
        redisUtils.set(RedisKeys.getWechatProfitChartKey(customerId, fundCode, chartDay), resultList, 60 * 60 * 8);
        return resultList;
    }

    @Override
    public List<Map<String, Object>> getWechatWeekChart(String fundCode, String chartDay) throws Exception {
        // chartDay,3:近三天,7:近一周,30：近一个月，90：近三个月，180：近半年
        String beginDate = DateUtil.getPastDate(Integer.valueOf(chartDay));
        String endDate = DateUtil.getPastDate(1);
        List<Map<String, Object>> weekProfitRatioList = odsCustomerBaseDao.getWeekProfitRatio(fundCode, beginDate, endDate);
        redisUtils.set(RedisKeys.getWechatWeekChartKey(fundCode, chartDay), weekProfitRatioList, 60 * 60 * 2);
        return weekProfitRatioList;
    }

    @Override
    public int insertPosition(CustomerPosition customerPosition) {
        int insert = customerPositionDao.insert(customerPosition);
        return insert;
    }
}
