package com.wisdge.cloud.calculate.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.wisdge.cloud.calculate.constant.DatasourceConstants;
import com.wisdge.cloud.calculate.entity.CustomerMotEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * MOTDao
 *
 * @author lsy
 * @date 2021-11-16
 */
@Mapper
public interface MotDao {
    /**
     * 身份证过期
     *
     * @return
     */
    @DS(DatasourceConstants.ORACLE)
    List<CustomerMotEntity> idCardExpired(@Param("custId") String custId);

    /**
     * 客户生日
     *
     * @return
     */
    @DS(DatasourceConstants.ORACLE)
    List<CustomerMotEntity> birthday(@Param("custId") String custId);

    /**
     * 风险问卷
     *
     * @return
     */
    @DS(DatasourceConstants.ORACLE)
    List<CustomerMotEntity> riskRuestionnaire(@Param("custId") String custId);

    /**
     * 百万资产
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> millionAssets(@Param("custId") String custId);

    /**
     * 流失预警
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> customerChurnWarning(@Param("custId") String custId);

    /**
     * 持仓波动
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> positionWave(@Param("custId") String custId);

    /**
     * 高点提示
     *
     * @param custId
     * @return
     */
    List<CustomerMotEntity> highPoint(@Param("custId") String custId);
}
