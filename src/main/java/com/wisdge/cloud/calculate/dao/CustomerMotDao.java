package com.wisdge.cloud.calculate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wisdge.cloud.calculate.entity.CustomerMotEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户mot信息Dao
 *
 * @author lsy
 * @date 2021-11-16
 */
@Mapper
public interface CustomerMotDao extends BaseMapper<CustomerMotEntity> {
}
