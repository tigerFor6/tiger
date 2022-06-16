package com.wisdge.cloud.calculate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wisdge.cloud.calculate.entity.TradeInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TradeInfoDao extends BaseMapper<TradeInfo> {

    int addTradeInfos(@Param("tradeList") List<TradeInfo> tradeList);
}
