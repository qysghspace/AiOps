package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ops_metric_point")
public class OpsMetricPoint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceName;
    private Long targetId;
    private String metricType;
    private Double metricValue;
    private String status;
    private Long probeTime;
    private String detail;
    private LocalDateTime createdAt;
}
