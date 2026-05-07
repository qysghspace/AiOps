package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.MobileIncidentBrief;
import com.example.aiops.entity.OpsIncident;
import com.example.aiops.mapper.OpsIncidentMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MobileIncidentService {

    private final OpsIncidentMapper opsIncidentMapper;

    public MobileIncidentService(OpsIncidentMapper opsIncidentMapper) {
        this.opsIncidentMapper = opsIncidentMapper;
    }

    public List<MobileIncidentBrief> listMyIncidents(String username, String status) {
        LambdaQueryWrapper<OpsIncident> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpsIncident::getAssignee, username);
        if (status != null && !status.isBlank()) {
            wrapper.eq(OpsIncident::getStatus, status);
        }
        wrapper.orderByDesc(OpsIncident::getId);

        return opsIncidentMapper.selectList(wrapper).stream()
                .map(it -> new MobileIncidentBrief(
                        it.getId(),
                        it.getIncidentNo(),
                        it.getSummary(),
                        it.getStatus(),
                        it.getAssignee()))
                .collect(Collectors.toList());
    }
}
