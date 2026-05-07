package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ops_alert_rule")
public class OpsAlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceName;
    private Double errorRateThreshold;
    private Long latencyThresholdMs;
    private Long dedupWindowSec;
    private Long suppressWindowSec;
    private String enabled;
}
