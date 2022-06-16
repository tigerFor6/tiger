package com.wisdge.cloud.calculate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wisdge.cloud.calculate.entity.HdHoldArc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HdHoldArcDao extends BaseMapper<HdHoldArc> {

    int addHoldArcs(@Param("fundAccount") String fundAccount);
}
