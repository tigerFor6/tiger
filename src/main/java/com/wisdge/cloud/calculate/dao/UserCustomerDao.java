package com.wisdge.cloud.calculate.dao;

import com.wisdge.cloud.calculate.dto.UserCustomerDTO;
import com.wisdge.cloud.calculate.entity.CustomerPosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户-客户关联信息Dao
 *
 * @author lsy
 * @date 2021-11-19
 */
@Mapper
public interface UserCustomerDao {
    /**
     * 列表
     * <p>
     * 查询有效的客户和专员列表
     * <p/>
     *
     * @return
     */
    List<UserCustomerDTO> list();

    /**
     * 根据客户ID查询对应专员
     *
     * @return
     */
    List<UserCustomerDTO> list4CustomerId(@Param("customerId") Long customerId);

    List<String> getSynCustids();

    List<String> getUpPosCustomerIds();

    List<Map<String,Object>> getPeakAssets(@Param("customerId") String customerId);

    List<Map<String,Object>> getDirectPeakAssets(@Param("customerId") String customerId);

    int addHdHoldArcs(@Param("mapList") List<Map<String, Object>> mapList);

    int addCustomerPositions(@Param("positions") List<CustomerPosition> positions);

    List<Map<String,Object>> getCustIdRal();

    List<String> getCustomerLoseIds(String beginTime, String endTime);

}
