package com.wisdge.cloud.calculate.enums;

import com.baomidou.dynamic.datasource.toolkit.StringUtils;

/**
 * mot任务code枚举
 *
 * @author lsy
 * @date 2021-11-18
 */
public enum MotTaksCodeEnum {
    RIGHTS_EXCEED("1", "权益超配"),
    MILLION_ASSETS("2", "资产首次超过100W"),
    BIG_PURCHASE("3", "大额申购"),
    BIG_REDEEM("4", "大额赎回"),
    CUSTOMER_CHURN_WARNING("5", "流失预警"),
    POSITION_WAVE("6", "持仓波动"),
    HIGH_POINT("7", "高点提示"),
    BIRTHDAY("9", "客户生日"),
    ID_CARD_EXPIRED("10", "证件过期"),
    RISK_RUESTIONNAIRE("11", "风险问卷过期");

    private final String code;
    private final String name;

    MotTaksCodeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return name;
    }

    /**
     * 根据编号获取对应枚举实例
     *
     * @param code 任务编号
     * @return
     */
    public static MotTaksCodeEnum getEnum(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (MotTaksCodeEnum result : MotTaksCodeEnum.values()) {
            if (result.getCode().equals(code)) {
                return result;
            }
        }
        return null;
    }
}
