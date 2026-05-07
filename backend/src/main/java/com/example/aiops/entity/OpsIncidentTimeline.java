package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ops_incident_timeline")
public class OpsIncidentTimeline {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long incidentId;
    private String fromStatus;
    private String toStatus;
    private String operatorName;
    private String remark;
}
