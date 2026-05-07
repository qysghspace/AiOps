package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ops_incident")
public class OpsIncident {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String incidentNo;
    private Long alertId;
    private String summary;
    private String status;
    private String assignee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
