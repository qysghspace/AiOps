package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.ChangePasswordRequest;
import com.example.aiops.entity.LoginRequest;
import com.example.aiops.entity.SysUser;
import com.example.aiops.entity.UserLoginLog;
import com.example.aiops.mapper.SysUserMapper;
import com.example.aiops.mapper.UserLoginLogMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${aiops.auth.token-ttl-seconds:7200}")
    private Long tokenTtlSeconds;

    public AuthService(SysUserMapper sysUserMapper,
                       UserLoginLogMapper userLoginLogMapper,
                       StringRedisTemplate stringRedisTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.userLoginLogMapper = userLoginLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Map<String, Object> login(LoginRequest request, String ip, String userAgent) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));

        if (user == null || !request.getPassword().equals(user.getPasswordHash())) {
            writeLoginLog(request.getUsername(), "FAILED", ip, userAgent);
            throw new IllegalArgumentException("invalid username or password");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = buildTokenKey(token);
        stringRedisTemplate.opsForValue().set(tokenKey, user.getUsername(), tokenTtlSeconds, TimeUnit.SECONDS);
        writeLoginLog(user.getUsername(), "SUCCESS", ip, userAgent);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("expiresIn", tokenTtlSeconds);
        return result;
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        stringRedisTemplate.delete(buildTokenKey(token));
    }

    public String verifyAndGetUsername(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String key = buildTokenKey(token);
        String username = stringRedisTemplate.opsForValue().get(key);
        if (username != null) {
            stringRedisTemplate.expire(key, tokenTtlSeconds, TimeUnit.SECONDS);
        }
        return username;
    }

    public Map<String, Object> changePassword(String username, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        if (!request.getOldPassword().equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("old password incorrect");
        }
        if (request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("new password must be at least 6 chars");
        }
        user.setPasswordHash(request.getNewPassword());
        sysUserMapper.updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("changed", true);
        return result;
    }

    public List<UserLoginLog> recentLoginLogs(String username) {
        return userLoginLogMapper.selectList(new LambdaQueryWrapper<UserLoginLog>()
                .eq(UserLoginLog::getUsername, username)
                .orderByDesc(UserLoginLog::getId)
                .last("limit 20"));
    }

    private void writeLoginLog(String username, String result, String ip, String userAgent) {
        UserLoginLog row = new UserLoginLog();
        row.setUsername(username == null ? "unknown" : username);
        row.setLoginResult(result);
        row.setIp(ip);
        row.setUserAgent(userAgent);
        row.setCreatedAt(LocalDateTime.now());
        userLoginLogMapper.insert(row);
    }

    private String buildTokenKey(String token) {
        return "login:token:" + token;
    }
}
