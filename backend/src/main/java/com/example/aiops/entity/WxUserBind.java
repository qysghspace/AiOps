package com.example.aiops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wx_user_bind")
public class WxUserBind {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String openId;
    private String unionId;
}
