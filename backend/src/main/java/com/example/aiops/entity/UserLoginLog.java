package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_login_log")
public class UserLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String loginResult;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
