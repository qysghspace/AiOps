package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.common.AuthContext;
import com.example.aiops.entity.NotifyRecord;
import com.example.aiops.entity.OpsIncident;
import com.example.aiops.mapper.NotifyRecordMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotifyRecordMapper notifyRecordMapper;

    public NotificationService(NotifyRecordMapper notifyRecordMapper) {
        this.notifyRecordMapper = notifyRecordMapper;
    }

    public void recordIncidentNotification(OpsIncident incident, String channel, String content) {
        NotifyRecord record = new NotifyRecord();
        record.setUsername(incident.getAssignee() == null || incident.getAssignee().isBlank()
                ? AuthContext.getUsername()
                : incident.getAssignee());
        record.setChannel(channel);
        record.setBizType("INCIDENT");
        record.setBizId(incident.getId());
        record.setContent(content);
        record.setStatus("SENT");
        notifyRecordMapper.insert(record);
    }

    public Map<String, Object> overview() {
        List<NotifyRecord> records = notifyRecordMapper.selectList(new LambdaQueryWrapper<NotifyRecord>()
                .orderByDesc(NotifyRecord::getId));
        long sent = records.stream().filter(x -> "SENT".equalsIgnoreCase(x.getStatus())).count();
        long failed = records.size() - sent;
        Map<String, Object> result = new HashMap<>();
        result.put("total", records.size());
        result.put("sent", sent);
        result.put("failed", failed);
        result.put("recent", records.stream().limit(10).toList());
        return result;
    }
}
