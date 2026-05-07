package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.WxLoginRequest;
import com.example.aiops.entity.WxUserBind;
import com.example.aiops.mapper.WxUserBindMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MobileAuthService {

    private final WxUserBindMapper wxUserBindMapper;

    public MobileAuthService(WxUserBindMapper wxUserBindMapper) {
        this.wxUserBindMapper = wxUserBindMapper;
    }

    public Map<String, Object> wxLogin(WxLoginRequest request) {
        String openId = "mock_" + request.getCode();

        WxUserBind bind = wxUserBindMapper.selectOne(new LambdaQueryWrapper<WxUserBind>()
                .eq(WxUserBind::getOpenId, openId)
                .last("limit 1"));

        if (bind == null) {
            bind = new WxUserBind();
            bind.setUsername(request.getUsername());
            bind.setOpenId(openId);
            bind.setUnionId("mock_union_" + request.getCode());
            wxUserBindMapper.insert(bind);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", "mock-jwt-" + bind.getUsername() + "-" + System.currentTimeMillis());
        result.put("username", bind.getUsername());
        result.put("openId", bind.getOpenId());
        return result;
    }
}
