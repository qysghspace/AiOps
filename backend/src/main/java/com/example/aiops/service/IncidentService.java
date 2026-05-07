package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.common.AuthContext;
import com.example.aiops.entity.IncidentCreateRequest;
import com.example.aiops.entity.IncidentStatusUpdateRequest;
import com.example.aiops.entity.OpsIncident;
import com.example.aiops.entity.OpsIncidentTimeline;
import com.example.aiops.mapper.OpsIncidentMapper;
import com.example.aiops.mapper.OpsIncidentTimelineMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private final OpsIncidentMapper opsIncidentMapper;
    private final OpsIncidentTimelineMapper opsIncidentTimelineMapper;
    private final IncidentStatusMachine incidentStatusMachine;
    private final NotificationService notificationService;

    public IncidentService(OpsIncidentMapper opsIncidentMapper,
                           OpsIncidentTimelineMapper opsIncidentTimelineMapper,
                           IncidentStatusMachine incidentStatusMachine,
                           NotificationService notificationService) {
        this.opsIncidentMapper = opsIncidentMapper;
        this.opsIncidentTimelineMapper = opsIncidentTimelineMapper;
        this.incidentStatusMachine = incidentStatusMachine;
        this.notificationService = notificationService;
    }

    public Map<String, Object> create(IncidentCreateRequest request) {
        OpsIncident incident = new OpsIncident();
        incident.setAlertId(request.getAlertId());
        incident.setSummary(request.getSummary());
        incident.setStatus("OPEN");

        String assignee = request.getAssignee();
        if (assignee == null || assignee.isBlank()) {
            assignee = AuthContext.getUsername();
        }
        incident.setAssignee(assignee);

        incident.setIncidentNo(generateIncidentNo());
        opsIncidentMapper.insert(incident);

        writeTimeline(incident.getId(), "-", "OPEN", assignee == null ? "system" : assignee, "incident created");
        notificationService.recordIncidentNotification(incident, "WEB", "新工单已创建: " + incident.getIncidentNo());

        Map<String, Object> result = new HashMap<>();
        result.put("id", incident.getId());
        result.put("incidentNo", incident.getIncidentNo());
        result.put("status", incident.getStatus());
        return result;
    }

    public Map<String, Object> updateStatus(Long id, IncidentStatusUpdateRequest request) {
        OpsIncident incident = getIncidentOrThrow(id);
        applyStatusUpdate(incident, request, "web-user", "manual update");
        return buildStatusResult(incident);
    }

    public Map<String, Object> updateStatusByAssignee(Long id, String username, IncidentStatusUpdateRequest request) {
        OpsIncident incident = getIncidentOrThrow(id);
        if (incident.getAssignee() == null || !incident.getAssignee().equals(username)) {
            throw new IllegalArgumentException("mobile user cannot update incident not assigned to self");
        }
        applyStatusUpdate(incident, request, username, "mobile quick action");
        return buildStatusResult(incident);
    }

    public List<OpsIncident> listByStatus(String status) {
        LambdaQueryWrapper<OpsIncident> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(OpsIncident::getStatus, status);
        }
        wrapper.orderByDesc(OpsIncident::getId);
        return opsIncidentMapper.selectList(wrapper);
    }

    public Map<String, Object> autoUpdateStatus(Long id, IncidentStatusUpdateRequest request) {
        OpsIncident incident = getIncidentOrThrow(id);
        applyStatusUpdate(incident, request, "system-auto", "auto transition");
        return buildStatusResult(incident);
    }

    private OpsIncident getIncidentOrThrow(Long id) {
        OpsIncident incident = opsIncidentMapper.selectById(id);
        if (incident == null) {
            throw new IllegalArgumentException("incident not found: " + id);
        }
        return incident;
    }

    private void applyStatusUpdate(OpsIncident incident,
                                   IncidentStatusUpdateRequest request,
                                   String operator,
                                   String remark) {
        String fromStatus = incident.getStatus();
        String toStatus = request.getStatus();
        incidentStatusMachine.validate(fromStatus, toStatus);

        incident.setStatus(toStatus);
        if (request.getAssignee() != null && !request.getAssignee().isBlank()) {
            incident.setAssignee(request.getAssignee());
        }
        opsIncidentMapper.updateById(incident);
        writeTimeline(incident.getId(), fromStatus, toStatus, operator, remark);
        notificationService.recordIncidentNotification(incident, "WEB", "工单状态变更: " + fromStatus + " -> " + toStatus);
    }

    private void writeTimeline(Long incidentId,
                               String fromStatus,
                               String toStatus,
                               String operator,
                               String remark) {
        OpsIncidentTimeline timeline = new OpsIncidentTimeline();
        timeline.setIncidentId(incidentId);
        timeline.setFromStatus(fromStatus);
        timeline.setToStatus(toStatus);
        timeline.setOperatorName(operator);
        timeline.setRemark(remark);
        opsIncidentTimelineMapper.insert(timeline);
    }

    private Map<String, Object> buildStatusResult(OpsIncident incident) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", incident.getId());
        result.put("status", incident.getStatus());
        result.put("assignee", incident.getAssignee());
        return result;
    }

    private String generateIncidentNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = (int) (Math.random() * 900 + 100);
        return "INC" + ts + suffix;
    }
}
