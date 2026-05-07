package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ops_alert")
public class OpsAlert {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceName;
    private String severity;
    private String status;
    private String title;
    private String detail;
    private Long ruleId;
    private LocalDateTime createdAt;
}
