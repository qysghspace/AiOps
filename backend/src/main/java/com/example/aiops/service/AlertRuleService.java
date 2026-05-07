package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.AlertRuleUpsertRequest;
import com.example.aiops.entity.OpsAlertRule;
import com.example.aiops.mapper.OpsAlertRuleMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertRuleService {

    private final OpsAlertRuleMapper opsAlertRuleMapper;

    @Value("${aiops.alert.default-error-rate-threshold:5.0}")
    private Double defaultErrorRateThreshold;

    @Value("${aiops.alert.default-latency-threshold-ms:2000}")
    private Long defaultLatencyThresholdMs;

    @Value("${aiops.alert.default-dedup-window-sec:300}")
    private Long defaultDedupWindowSec;

    @Value("${aiops.alert.default-suppress-window-sec:120}")
    private Long defaultSuppressWindowSec;

    public AlertRuleService(OpsAlertRuleMapper opsAlertRuleMapper) {
        this.opsAlertRuleMapper = opsAlertRuleMapper;
    }

    public OpsAlertRule resolveRule(String serviceName) {
        OpsAlertRule rule = opsAlertRuleMapper.selectOne(new LambdaQueryWrapper<OpsAlertRule>()
                .eq(OpsAlertRule::getServiceName, serviceName)
                .eq(OpsAlertRule::getEnabled, "Y")
                .last("limit 1"));

        if (rule != null) {
            return rule;
        }

        OpsAlertRule fallback = new OpsAlertRule();
        fallback.setId(0L);
        fallback.setServiceName(serviceName);
        fallback.setErrorRateThreshold(defaultErrorRateThreshold);
        fallback.setLatencyThresholdMs(defaultLatencyThresholdMs);
        fallback.setDedupWindowSec(defaultDedupWindowSec);
        fallback.setSuppressWindowSec(defaultSuppressWindowSec);
        fallback.setEnabled("Y");
        return fallback;
    }

    public List<OpsAlertRule> listAll() {
        return opsAlertRuleMapper.selectList(new LambdaQueryWrapper<OpsAlertRule>()
                .orderByDesc(OpsAlertRule::getId));
    }

    public Map<String, Object> createRule(AlertRuleUpsertRequest request) {
        OpsAlertRule row = new OpsAlertRule();
        BeanUtils.copyProperties(request, row);
        normalizeEnabled(row);
        opsAlertRuleMapper.insert(row);
        return mapRuleResult(row);
    }

    public Map<String, Object> updateRule(Long id, AlertRuleUpsertRequest request) {
        OpsAlertRule row = opsAlertRuleMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("rule not found: " + id);
        }
        BeanUtils.copyProperties(request, row);
        row.setId(id);
        normalizeEnabled(row);
        opsAlertRuleMapper.updateById(row);
        return mapRuleResult(row);
    }

    public Map<String, Object> toggleRule(Long id, String enabled) {
        OpsAlertRule row = opsAlertRuleMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("rule not found: " + id);
        }
        row.setEnabled("Y".equalsIgnoreCase(enabled) ? "Y" : "N");
        opsAlertRuleMapper.updateById(row);
        return mapRuleResult(row);
    }

    private void normalizeEnabled(OpsAlertRule row) {
        row.setEnabled("Y".equalsIgnoreCase(row.getEnabled()) ? "Y" : "N");
    }

    private Map<String, Object> mapRuleResult(OpsAlertRule row) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", row.getId());
        result.put("serviceName", row.getServiceName());
        result.put("enabled", row.getEnabled());
        return result;
    }
}
