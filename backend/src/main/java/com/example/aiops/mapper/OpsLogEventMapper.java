package com.example.aiops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiops.entity.OpsLogEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpsLogEventMapper extends BaseMapper<OpsLogEvent> {
}
