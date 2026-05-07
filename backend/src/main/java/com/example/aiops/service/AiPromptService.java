package com.example.aiops.service;

import com.example.aiops.entity.IncidentAnalyzeRequest;
import com.example.aiops.entity.HelpAskRequest;
import org.springframework.stereotype.Service;

@Service
public class AiPromptService {

    public String buildHelpSystemPrompt() {
        return "你是AIOps项目的新手指导助手。严格使用中文并使用简洁步骤型输出。"
                + "请按固定结构回答：\n"
                + "问题判断：一句话\n"
                + "可能原因：最多3条\n"
                + "排查步骤：按1/2/3列出\n"
                + "建议操作：最多3条\n"
                + "如果问题涉及数据输入，明确指出数据来源（监控/日志/APM/人工输入）。\n"
                + "避免空泛描述，不要输出代码块。";
    }

    public String buildHelpUserPrompt(HelpAskRequest request) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("用户问题：").append(request.getQuestion());
        if (request.getContext() != null && !request.getContext().isBlank()) {
            userPrompt.append("\n上下文：").append(request.getContext());
        }
        return userPrompt.toString();
    }

    public String buildIncidentSystemPrompt() {
        return "你是AIOps故障分析助手。请基于故障摘要输出结构化分析，严格中文，简洁明确。"
                + "请按固定结构回答：\n"
                + "根因候选：最多3条\n"
                + "建议操作：最多5条\n"
                + "置信度：0-100之间的整数或小数\n"
                + "不要输出与故障无关的内容。";
    }

    public String buildIncidentUserPrompt(IncidentAnalyzeRequest request) {
        return "工单编号或ID：" + request.getIncidentId() + "\n故障摘要：" + request.getSummary();
    }
}
