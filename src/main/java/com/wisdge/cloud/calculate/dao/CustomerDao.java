package com.wisdge.cloud.calculate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wisdge.cloud.calculate.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerDao extends BaseMapper<Customer> {

}
