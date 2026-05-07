<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const API = 'http://localhost:8080'
const tokenKey = 'aiops_token'
const router = useRouter()

const activeTab = ref('alerts')
const loading = ref(false)
const query = ref({ keyword: '', status: '', severity: '' })
const pageByTab = ref({ alerts: 1, workorders: 1 })
const pageSize = ref(10)
const stats = ref({ alerts: 0, workorders: 0, critical: 0, pending: 0 })

const currentPage = computed({
  get: () => pageByTab.value[activeTab.value] || 1,
  set: (v) => { pageByTab.value[activeTab.value] = Number(v) || 1 },
})

const alerts = ref([])
const workorders = ref([])
const incidentByAlertId = ref({})

const statusOptions = ['NEW', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'ACK', 'REVIEWED']
const severityOptions = ['P1', 'P2', 'P3', 'P4', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

async function api(path) {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem(tokenKey)
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${API}${path}`, { headers })
  return res.json()
}

const normalize = (v, d) => (v ? String(v).toUpperCase() : d)

async function loadData() {
  loading.value = true
  try {
    const [targetResp, incidentResp] = await Promise.all([api('/api/monitor-targets'), api('/api/incidents')])
    const targets = targetResp?.data || []
    const incidents = incidentResp?.data || []

    const alertGroups = await Promise.all(
      targets.map((t) => api(`/api/monitor-targets/${t.id}/alerts`).then((r) => r?.data || []).catch(() => []))
    )

    alerts.value = alertGroups.flat().map((x) => ({
      id: x.id,
      title: x.content || x.title || `告警-${x.id}`,
      severity: normalize(x.severity, 'MEDIUM'),
      status: normalize(x.status, 'OPEN'),
      source: x.serviceName || x.source || 'monitor-probe',
      createdAt: x.time || x.createdAt || '-',
    }))

    workorders.value = incidents.map((x) => ({
      id: x.incidentNo || `WO-${x.id}`,
      incidentId: x.id,
      alertId: x.alertId,
      title: x.summary || x.title || `工单-${x.id}`,
      assignee: x.assignee || x.owner || '未指派',
      status: normalize(x.status, 'OPEN'),
      updatedAt: x.updatedAt || x.gmtModified || x.createdAt || '-',
    }))

    incidentByAlertId.value = workorders.value.reduce((acc, w) => {
      if (w.alertId !== null && w.alertId !== undefined) acc[String(w.alertId)] = w
      return acc
    }, {})

    stats.value = {
      alerts: alerts.value.length,
      workorders: workorders.value.length,
      critical: alerts.value.filter((x) => ['P1', 'P2', 'CRITICAL', 'HIGH'].includes(x.severity)).length,
      pending: workorders.value.filter((x) => ['NEW', 'OPEN', 'IN_PROGRESS'].includes(x.status)).length,
    }
  } finally {
    loading.value = false
  }
}

const filteredAlerts = computed(() => alerts.value.filter((x) => {
  const k = query.value.keyword
  return (!k || `${x.title}${x.id}`.includes(k)) &&
    (!query.value.status || x.status === query.value.status) &&
    (!query.value.severity || x.severity === query.value.severity)
}))

const filteredWorkorders = computed(() => workorders.value.filter((x) => {
  const k = query.value.keyword
  return (!k || `${x.title}${x.id}${x.incidentId}`.includes(k)) &&
    (!query.value.status || x.status === query.value.status)
}))

const pagedAlerts = computed(() => filteredAlerts.value.slice((pageByTab.value.alerts - 1) * pageSize.value, pageByTab.value.alerts * pageSize.value))
const pagedWorkorders = computed(() => filteredWorkorders.value.slice((pageByTab.value.workorders - 1) * pageSize.value, pageByTab.value.workorders * pageSize.value))
const total = computed(() => activeTab.value === 'alerts' ? filteredAlerts.value.length : filteredWorkorders.value.length)

function onSearch() { currentPage.value = 1 }
function onReset() { query.value = { keyword: '', status: '', severity: '' }; currentPage.value = 1 }
function onPageSizeChange() { currentPage.value = 1 }
function goDetailByIncidentId(id) { router.push(`/incident/${id}`) }

function statusClass(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'OPEN' || s === 'NEW') return 'status-open'
  if (s === 'IN_PROGRESS') return 'status-in_progress'
  if (s === 'RESOLVED' || s === 'CLOSED') return 'status-resolved'
  return 'status-new'
}

async function ensureIncidentForAlert(row) {
  const existed = incidentByAlertId.value[String(row.id)]
  if (existed?.incidentId) return existed.incidentId
  ElMessage.warning('该告警尚未关联工单，请先创建/关联')
  return null
}

async function goDetailByAlert(row) {
  const id = await ensureIncidentForAlert(row)
  if (id) goDetailByIncidentId(id)
}

watch([filteredAlerts, filteredWorkorders, activeTab], () => {
  const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

onMounted(loadData)
</script>

<template>
  <div class="page-header">
    <div class="page-title">告警与工单列表</div>
    <div class="page-sub">工作台首页 / 告警与工单列表</div>
  </div>

  <el-space direction="vertical" :size="16" style="width:100%">
    <el-card class="hero-card">
      <div>
        <div class="hero-eyebrow">事件协同中心</div>
        <div class="hero-title">告警与工单统一视图</div>
        <div class="hero-sub">支持按风险等级、状态、工单关联情况快速定位问题，满足真实值班场景下的筛选与处置需求。</div>
      </div>
      <el-button @click="loadData">刷新数据</el-button>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="6"><el-card class="stat-card stat-highlight-blue"><div class="stat-label">告警总数</div><div class="stat-value">{{ stats.alerts }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-red"><div class="stat-label">高风险告警</div><div class="stat-value">{{ stats.critical }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-green"><div class="stat-label">工单总数</div><div class="stat-value">{{ stats.workorders }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-amber"><div class="stat-label">处理中工单</div><div class="stat-value">{{ stats.pending }}</div></el-card></el-col>
    </el-row>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <strong>告警 / 工单列表</strong>
          <div class="list-sub">可基于关键字、状态、等级进行组合筛选</div>
        </div>
      </template>

      <el-row :gutter="12" style="margin-bottom: 12px;">
        <el-col :span="8"><el-input v-model="query.keyword" placeholder="关键字（标题/ID）" @keyup.enter="onSearch" /></el-col>
        <el-col :span="4"><el-select v-model="query.status" placeholder="状态" clearable><el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" /></el-select></el-col>
        <el-col :span="4"><el-select v-model="query.severity" placeholder="等级" clearable :disabled="activeTab !== 'alerts'"><el-option v-for="s in severityOptions" :key="s" :label="s" :value="s" /></el-select></el-col>
        <el-col :span="8" style="text-align:right;"><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="onReset">重置</el-button></el-col>
      </el-row>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="告警列表" name="alerts">
        <el-table :data="pagedAlerts" v-loading="loading" stripe border empty-text="暂无告警数据" table-layout="fixed" class="ops-table">
          <el-table-column prop="id" label="告警ID" width="80" align="center" />
          <el-table-column prop="title" label="告警内容" min-width="320" show-overflow-tooltip />
          <el-table-column label="等级" width="80" align="center"><template #default="scope"><el-tag :class="scope.row.severity === 'P1' || scope.row.severity === 'CRITICAL' ? 'status-open' : 'status-in_progress'">{{ scope.row.severity }}</el-tag></template></el-table-column>
          <el-table-column label="状态" width="100" align="center"><template #default="scope"><el-tag :class="statusClass(scope.row.status)">{{ scope.row.status }}</el-tag></template></el-table-column>
          <el-table-column prop="source" label="来源" width="140" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right" align="center">
            <template #default="scope">
              <el-dropdown>
                <el-button type="primary" plain>操作</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="goDetailByAlert(scope.row)">查看详情</el-dropdown-item>
                    <el-dropdown-item @click="goDetailByAlert(scope.row)">流转状态</el-dropdown-item>
                    <el-dropdown-item style="color:#E53E3E">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="工单列表" name="workorders">
        <el-table :data="pagedWorkorders" v-loading="loading" stripe border empty-text="暂无工单数据" table-layout="fixed" class="ops-table">
          <el-table-column prop="id" label="工单ID" width="110" />
          <el-table-column prop="alertId" label="告警ID" width="80" align="center" />
          <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
          <el-table-column prop="assignee" label="处理人" width="100" align="center" />
          <el-table-column label="状态" width="100" align="center"><template #default="scope"><el-tag :class="statusClass(scope.row.status)">{{ scope.row.status }}</el-tag></template></el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right" align="center"><template #default="scope"><el-button type="primary" plain @click="goDetailByIncidentId(scope.row.incidentId)">查看详情</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

      <div style="display:flex;justify-content:flex-end;margin-top:12px;">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next" @size-change="onPageSizeChange" />
      </div>
    </el-card>
  </el-space>
</template>