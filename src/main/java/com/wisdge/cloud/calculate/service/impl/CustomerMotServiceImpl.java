package com.wisdge.cloud.calculate.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wisdge.cloud.calculate.dao.CustomerMotDao;
import com.wisdge.cloud.calculate.entity.CustomerMotEntity;
import com.wisdge.cloud.calculate.service.CustomerMotService;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 客户mot信息service
 *
 * @author lsy
 * @date 2021-11-16
 */
@Service("customerMotService")
public class CustomerMotServiceImpl extends ServiceImpl<CustomerMotDao, CustomerMotEntity> implements CustomerMotService {
}
