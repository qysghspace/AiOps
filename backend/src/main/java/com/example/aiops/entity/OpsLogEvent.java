package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ops_log_event")
public class OpsLogEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceName;
    private String environment;
    private String level;
    private String message;
    private String traceId;
    private Long eventTime;
}
