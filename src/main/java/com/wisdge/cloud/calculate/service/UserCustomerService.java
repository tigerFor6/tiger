package com.wisdge.cloud.calculate.service;

import com.wisdge.cloud.calculate.dto.UserCustomerDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-客户关联信息service
 *
 * @author lsy
 * @date 2021-11-19
 */
public interface UserCustomerService {

    /**
     * 列表
     * <p>
     * 查询有效的客户和CUSTID集合
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
}
