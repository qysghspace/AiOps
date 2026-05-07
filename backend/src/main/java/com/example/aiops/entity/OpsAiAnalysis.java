package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ops_ai_analysis")
public class OpsAiAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long incidentId;
    private BigDecimal confidence;
    private String rootCause;
    private String suggestion;
    private LocalDateTime createdAt;
}
