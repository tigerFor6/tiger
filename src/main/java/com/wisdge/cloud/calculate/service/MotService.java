package com.wisdge.cloud.calculate.service;

import com.wisdge.cloud.calculate.entity.CustomerMotEntity;
import com.wisdge.cloud.calculate.enums.MotTaksCodeEnum;

import java.util.List;

/**
 * MotService
 *
 * @author lsy
 * @date 2021-11-16
 */
public interface MotService {

    /**
     * 身份证过期
     *
     * @return
     */
    List<CustomerMotEntity> idCardExpired(String custId);

    /**
     * 客户生日
     *
     * @return
     */
    List<CustomerMotEntity> birthday(String custId);

    /**
     * 风险问卷
     *
     * @return
     */
    List<CustomerMotEntity> riskRuestionnaire(String custId);

    /**
     * 百万资产
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> millionAssets(String custId);

    /**
     * 流失预警
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> customerChurnWarning(String custId);


    /**
     * 持仓波动
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> positionWave(String custId);


    /**
     * 高点提示
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> highPoint(String custId);

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
    List<CustomerMotEntity> rightsExceed(String custId, MotTaksCodeEnum code);

    /**
     * 大额申购
     * <p>
     * 当日累计申购金额≥10W提醒
     * 实时
     * </p>
     *
     * @return
     */
    List<CustomerMotEntity> bigPurchase(String custId);

    /**
     * 大额赎回
     * <p>
     * 赎回金额≥10W提醒，金额为按当日净值估算
     * 实时
     * </p>
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> bigRedeem(String custId);


    /**
     * mot任务
     *
     * @param taskId 任务id
     * @return
     */
    Integer motTask(String taskId);

    /**
     * 更新交易信息
     *
     * @return
     */
    Integer updateTradeInfo();
}
