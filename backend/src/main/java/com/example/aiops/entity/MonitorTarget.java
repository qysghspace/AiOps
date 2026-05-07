package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("monitor_target")
public class MonitorTarget {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceName;
    private String targetUrl;
    private String targetHost;
    private Integer targetPort;
    private String protocol;
    private String expectedStatus;
    private Integer timeoutMs;
    private Integer intervalSec;
    private String enabled;
    private LocalDateTime createdAt;
}
