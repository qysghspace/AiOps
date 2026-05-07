<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const API = 'http://localhost:8080'
const tokenKey = 'aiops_token'
const router = useRouter()

const loading = ref(false)
const todoCount = ref(0)
const highPriorityAlerts = ref([])
const myTodos = ref([])
const targetStats = ref({ total: 0, online: 0, offline: 0 })
const slaStats = ref({ timeout: 0, risk: 0, healthy: 0 })
const trend = ref({ todo: 3, high: 1, mine: 2 })
const quickActions = [
  { title: '接入监控目标', desc: '新增服务/IP/端口并配置探测间隔', path: '/targets' },
  { title: '处理高优告警', desc: '查看 P1/P2 告警并推进工单状态', path: '/alerts' },
  { title: 'AI 根因分析', desc: '基于告警上下文生成处置建议', path: '/incident' },
  { title: '复盘知识沉淀', desc: '回看历史分析并补充真实原因反馈', path: '/history' },
]

function authHeaders() {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem(tokenKey)
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

function statusClass(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'OPEN' || s === 'NEW') return 'status-open'
  if (s === 'IN_PROGRESS') return 'status-in_progress'
  if (s === 'RESOLVED' || s === 'CLOSED') return 'status-resolved'
  return 'status-new'
}

async function loadHome() {
  loading.value = true
  try {
    const [incidentsRes, targetsRes] = await Promise.all([
      fetch(`${API}/api/incidents`, { headers: authHeaders() }).then((r) => r.json()),
      fetch(`${API}/api/monitor-targets`, { headers: authHeaders() }).then((r) => r.json()),
    ])

    const incidents = incidentsRes?.data || []
    const targets = targetsRes?.data || []

    const alertGroups = await Promise.all(
      targets.map((t) => fetch(`${API}/api/monitor-targets/${t.id}/alerts`, { headers: authHeaders() }).then((r) => r.json()).then((x) => x?.data || []).catch(() => []))
    )

    const merged = alertGroups.flat().map((x) => ({
      id: x.id,
      title: x.content || x.title || `告警-${x.id}`,
      severity: String(x.severity || 'MEDIUM').toUpperCase(),
      status: String(x.status || 'OPEN').toUpperCase(),
      assignee: x.owner || x.assignee || '未指派',
      createdAt: x.time || '-',
    }))

    highPriorityAlerts.value = merged.filter((x) => ['P1', 'P2', 'HIGH', 'CRITICAL'].includes(x.severity)).slice(0, 6)
    myTodos.value = incidents
      .filter((x) => ['NEW', 'OPEN', 'IN_PROGRESS'].includes(String(x.status || '').toUpperCase()))
      .slice(0, 6)
      .map((x) => ({ id: x.id, title: x.title || `工单-${x.id}`, status: String(x.status || 'OPEN').toUpperCase() }))

    const onlineCount = targets.filter((x) => {
      const status = String(x.status || '').toUpperCase()
      return status === 'UP' || status === 'RUNNING' || x.enabled === true || x.enabled === 1 || String(x.enabled || '').toUpperCase() === 'Y'
    }).length

    targetStats.value = {
      total: targets.length,
      online: onlineCount,
      offline: Math.max(0, targets.length - onlineCount),
    }

    slaStats.value = {
      timeout: incidents.filter((x) => ['OPEN', 'NEW'].includes(String(x.status || '').toUpperCase())).length,
      risk: merged.filter((x) => ['P1', 'P2', 'CRITICAL', 'HIGH'].includes(x.severity)).length,
      healthy: merged.filter((x) => ['RESOLVED', 'CLOSED'].includes(x.status)).length,
    }

    todoCount.value = highPriorityAlerts.value.length + myTodos.value.length
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <div class="page-header">
    <div class="page-title">工作台首页</div>
    <div class="page-sub">工作台首页 / 运维驾驶舱</div>
  </div>

  <div class="dashboard-wrap" v-loading="loading">
    <el-space direction="vertical" :size="16" style="width:100%">
      <el-card class="hero-card dashboard-hero">
        <div>
          <div class="hero-eyebrow">值班总览</div>
          <div class="hero-title">AIOps 运维驾驶舱</div>
          <div class="hero-sub">围绕“监控目标 → 告警识别 → AI研判 → 工单协同 → 复盘沉淀”构建更贴近真实业务的统一工作台。</div>
        </div>
        <div class="hero-actions">
          <el-button class="main-action" type="primary" @click="router.push('/incident')">开始处理</el-button>
          <el-button plain @click="router.push('/targets')">接入新目标</el-button>
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-card class="stat-card clickable-card stat-highlight-blue">
            <div class="stat-label">今日待处理数</div>
            <div class="stat-value">{{ todoCount }}</div>
            <div class="stat-trend">较昨日 + {{ trend.todo }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card clickable-card stat-highlight-red">
            <div class="stat-label">高优先告警</div>
            <div class="stat-value">{{ highPriorityAlerts.length }}</div>
            <div class="stat-trend">较昨日 + {{ trend.high }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card clickable-card stat-highlight-green">
            <div class="stat-label">我的待办</div>
            <div class="stat-value">{{ myTodos.length }}</div>
            <div class="stat-trend">较昨日 + {{ trend.mine }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" align="stretch">
        <el-col :span="12">
          <el-card class="business-card">
            <template #header><strong>业务值守指标</strong></template>
            <div class="kpi-grid">
              <div class="kpi-item soft-blue">
                <div class="kpi-title">监控目标总数</div>
                <div class="kpi-value">{{ targetStats.total }}</div>
                <div class="kpi-sub">在线 {{ targetStats.online }} / 离线 {{ targetStats.offline }}</div>
              </div>
              <div class="kpi-item soft-red">
                <div class="kpi-title">超时待处理工单</div>
                <div class="kpi-value">{{ slaStats.timeout }}</div>
                <div class="kpi-sub">建议优先升级处理</div>
              </div>
              <div class="kpi-item soft-amber">
                <div class="kpi-title">风险告警量</div>
                <div class="kpi-value">{{ slaStats.risk }}</div>
                <div class="kpi-sub">P1/P2/高危告警待关注</div>
              </div>
              <div class="kpi-item soft-green">
                <div class="kpi-title">已恢复事件</div>
                <div class="kpi-value">{{ slaStats.healthy }}</div>
                <div class="kpi-sub">已进入已解决/已关闭阶段</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="business-card">
            <template #header><strong>快捷业务入口</strong></template>
            <div class="quick-grid">
              <div v-for="item in quickActions" :key="item.title" class="quick-card" @click="router.push(item.path)">
                <div class="quick-title">{{ item.title }}</div>
                <div class="quick-desc">{{ item.desc }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" align="top">
        <el-col :span="17">
          <el-card>
            <template #header><strong>高优先告警</strong></template>
            <el-empty v-if="!highPriorityAlerts.length" description="暂无高优先告警，去接入监控目标" />
            <el-space v-else direction="vertical" fill style="width:100%">
              <div class="list-item" v-for="x in highPriorityAlerts" :key="x.id">
                <div style="display:flex;align-items:center;gap:12px;min-width:0">
                  <el-tag :class="x.severity === 'P1' || x.severity === 'CRITICAL' ? 'status-open' : 'status-in_progress'">{{ x.severity }}</el-tag>
                  <div style="min-width:0">
                    <div class="list-title" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:440px">{{ x.title }}</div>
                    <div class="list-sub">{{ x.createdAt }} · 责任人：{{ x.assignee }}</div>
                  </div>
                </div>
                <div style="display:flex;align-items:center;gap:8px">
                  <el-tag :class="statusClass(x.status)">{{ x.status }}</el-tag>
                  <el-button type="primary" plain @click="router.push(`/incident/${x.id}`)">查看详情</el-button>
                </div>
              </div>
            </el-space>
          </el-card>
        </el-col>

        <el-col :span="7">
          <el-card>
            <template #header><strong>我的待办</strong></template>
            <el-empty v-if="!myTodos.length" description="暂无待办" />
            <el-space v-else direction="vertical" fill style="width:100%">
              <div class="list-item todo-item" v-for="x in myTodos.slice(0, 4)" :key="x.id" @click="router.push(`/incident/${x.id}`)">
                <div>
                  <div class="list-title" style="font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ x.title }}</div>
                  <div class="list-sub">建议尽快跟进处理</div>
                </div>
                <el-tag :class="statusClass(x.status)">{{ x.status }}</el-tag>
              </div>
            </el-space>
          </el-card>
        </el-col>
      </el-row>
    </el-space>
  </div>
</template>
