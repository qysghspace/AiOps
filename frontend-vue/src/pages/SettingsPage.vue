<script setup>
import { ref } from 'vue'

const users = ref([
  { username: 'admin', role: '管理员', wechat: 'ops_admin', createdAt: '2026-01-10' },
  { username: 'alice', role: '普通用户', wechat: 'alice_ops', createdAt: '2026-03-12' },
])

const tools = ref([
  { name: 'get_incident_context', enabled: true },
  { name: 'suggest_first_actions', enabled: true },
  { name: 'similar_cause_retrieval', enabled: true },
])

const notificationRules = ref([
  { level: 'P1/CRITICAL', channel: '企业微信 + 短信', escalation: '5分钟未确认自动升级', enabled: true },
  { level: 'P2/HIGH', channel: '企业微信', escalation: '15分钟未确认提醒值班长', enabled: true },
  { level: 'P3/P4', channel: '邮件', escalation: '工作时间内汇总通知', enabled: false },
])
</script>

<template>
  <div class="page-header">
    <div class="page-title">系统设置与用户中心</div>
    <div class="page-sub">工作台首页 / 系统设置</div>
  </div>

  <el-space direction="vertical" :size="16" style="width:100%">
    <el-card class="hero-card">
      <div>
        <div class="hero-eyebrow">平台治理配置</div>
        <div class="hero-title">系统设置与用户中心</div>
        <div class="hero-sub">统一管理账号角色、AI 工具开关、健康检查、告警通知和升级策略，保障平台运行可控。</div>
      </div>
    </el-card>

  <el-row :gutter="16">
    <el-col :span="12">
      <el-card>
        <template #header><strong>用户管理</strong></template>
        <el-table :data="users" stripe border table-layout="fixed" class="ops-table">
          <el-table-column prop="username" label="用户名" width="120" align="center" />
          <el-table-column prop="role" label="角色" width="120" align="center" />
          <el-table-column prop="wechat" label="微信" min-width="140" />
          <el-table-column prop="createdAt" label="创建时间" width="140" align="center" />
        </el-table>
      </el-card>
    </el-col>

    <el-col :span="12">
      <el-card>
        <template #header><strong>工具与平台配置</strong></template>
        <el-table :data="tools" stripe border table-layout="fixed" class="ops-table" style="margin-bottom:12px">
          <el-table-column prop="name" label="工具" min-width="220" />
          <el-table-column label="开关" width="120" align="center">
            <template #default="scope"><el-switch v-model="scope.row.enabled" /></template>
          </el-table-column>
        </el-table>

        <el-form label-width="140px" class="settings-form">
          <el-form-item label="Redis 健康检查"><el-switch :model-value="true" /></el-form-item>
          <el-form-item label="默认探测间隔"><el-input model-value="30秒" /></el-form-item>
          <el-form-item label="告警通知方式">
            <el-select style="width:100%" model-value="wecom">
              <el-option label="邮件" value="email" />
              <el-option label="企业微信" value="wecom" />
              <el-option label="短信 + 企业微信" value="sms_wecom" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>

    <el-card>
      <template #header><strong>告警通知与升级策略</strong></template>
      <el-table :data="notificationRules" stripe border table-layout="fixed" class="ops-table">
        <el-table-column prop="level" label="适用等级" width="160" align="center" />
        <el-table-column prop="channel" label="通知渠道" min-width="180" />
        <el-table-column prop="escalation" label="升级规则" min-width="260" />
        <el-table-column label="启用" width="120" align="center">
          <template #default="scope"><el-switch v-model="scope.row.enabled" /></template>
        </el-table-column>
      </el-table>
    </el-card>
  </el-space>
</template>