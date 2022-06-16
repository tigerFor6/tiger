package com.wisdge.cloud.calculate.service.impl;

import com.wisdge.cloud.calculate.dao.UserCustomerDao;
import com.wisdge.cloud.calculate.dto.UserCustomerDTO;
import com.wisdge.cloud.calculate.service.UserCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 用户-客户关联信息service
 *
 * @author lsy
 * @date 2021-11-19
 */
@Service("userCustomerService")
public class UserCustomerServiceImpl implements UserCustomerService {

    @Autowired
    private UserCustomerDao userCustomerDao;

    /**
     * 列表
     * <p>
     * 查询有效的客户和专员列表
     * <p/>
     *
     * @return
     */
    @Override
    public List<UserCustomerDTO> list() {
        return userCustomerDao.list();
    }

    /**
     * 根据客户ID查询对应专员
     *
     * @return
     */
    @Override
    public List<UserCustomerDTO> list4CustomerId(Long customerId) {
        return userCustomerDao.list4CustomerId(customerId);
    }
}
