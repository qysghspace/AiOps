package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ops_alert_analysis_feedback")
public class OpsAlertAnalysisFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertId;

    private Long incidentId;

    private Long aiAnalysisId;

    private String selectedReasons;

    private String reasonText;

    private Boolean falsePositive;

    private String createdBy;

    private LocalDateTime createdAt;
}
