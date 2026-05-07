package com.example.aiops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiops.entity.OpsIncident;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OpsIncidentMapper extends BaseMapper<OpsIncident> {
}
