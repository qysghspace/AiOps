package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify_record")
public class NotifyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String channel;
    private String bizType;
    private Long bizId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
