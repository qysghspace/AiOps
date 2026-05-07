<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const API = 'http://localhost:8080'
const tokenKey = 'aiops_token'

const query = ref('')
const loading = ref(false)
const targets = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = ref({ name: '', ip: '', port: 80, intervalSeconds: 30 })

const targetStats = computed(() => {
  const total = targets.value.length
  const online = targets.value.filter((x) => isOnline(x)).length
  const offline = Math.max(0, total - online)
  const stopped = targets.value.filter((x) => normalizeEnabled(x.enabled) !== 'Y').length
  return { total, online, offline, stopped }
})

function normalizeEnabled(v) {
  if (v === true || v === 1) return 'Y'
  const s = String(v ?? 'Y').toUpperCase()
  return ['Y', 'YES', 'TRUE', '1', 'ENABLED', 'UP', 'RUNNING'].includes(s) ? 'Y' : 'N'
}

function isOnline(row) {
  const status = String(row?.status || '').toUpperCase()
  if (status === 'UP') return true
  if (status === 'DOWN') return false
  return normalizeEnabled(row?.enabled) === 'Y'
}

function headers(extra = {}) {
  const h = Object.assign({ 'Content-Type': 'application/json' }, extra)
  const token = localStorage.getItem(tokenKey)
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

async function req(path, opt = {}) {
  const res = await fetch(`${API}${path}`, { ...opt, headers: headers(opt.headers || {}) })
  const j = await res.json()
  if (!res.ok || (j && j.code && j.code !== '0')) throw new Error(j?.message || `请求失败: ${res.status}`)
  return j
}

async function loadTargets() {
  loading.value = true
  try {
    const j = await req('/api/monitor-targets')
    targets.value = (j?.data || []).map((x) => ({
      id: x.id,
      name: x.name || x.serviceName || '-',
      ip: x.ip || x.targetHost || '-',
      port: x.port || x.targetPort || '-',
      status: String(x.status || (normalizeEnabled(x.enabled) === 'Y' ? 'UP' : 'DOWN')).toUpperCase(),
      enabled: normalizeEnabled(x.enabled),
      intervalSeconds: x.intervalSeconds || x.intervalSec || 30,
      lastProbeTime: x.lastProbeTime || x.createdAt || '-',
    }))
  } catch (e) {
    targets.value = []
    ElMessage.error(e?.message || '加载监控目标失败')
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => targets.value.filter(t => !query.value || t.name?.includes(query.value) || t.ip?.includes(query.value)))

function openCreate() {
  editing.value = null
  form.value = { name: '', ip: '', port: 80, intervalSeconds: 30 }
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = { name: row.name, ip: row.ip, port: row.port, intervalSeconds: row.intervalSeconds || 30 }
  dialogVisible.value = true
}

async function saveTarget() {
  if (!form.value.name || !form.value.ip || !form.value.port) return ElMessage.warning('请填写完整目标信息')
  try {
    if (!editing.value) {
      await req('/api/monitor-targets', { method: 'POST', body: JSON.stringify(form.value) })
      ElMessage.success('目标已创建')
    } else {
      await req(`/api/monitor-targets/${editing.value.id}`, { method: 'PUT', body: JSON.stringify(form.value) })
      ElMessage.success('目标已更新')
    }
    dialogVisible.value = false
    await loadTargets()
  } catch (e) {
    ElMessage.error(e?.message || '保存目标失败')
  }
}

async function removeTarget(row) {
  await ElMessageBox.confirm(`确认删除目标 ${row.name} ?`, '提示', { type: 'warning' })
  await req(`/api/monitor-targets/${row.id}`, { method: 'DELETE' })
  ElMessage.success('已删除')
  await loadTargets()
}

async function probeNow(row) {
  await req(`/api/monitor-targets/${row.id}/probe`, { method: 'POST' })
  ElMessage.success('已触发手动探测')
  await loadTargets()
}

async function stopProbe(row) {
  try {
    await req(`/api/monitor-targets/${row.id}/stop`, { method: 'PATCH' })
  } catch {
    await req(`/api/monitor-targets/${row.id}/stop`, { method: 'POST' })
  }
  ElMessage.success('已停止自动探测')
  await loadTargets()
}

async function resumeProbe(row) {
  try {
    await req(`/api/monitor-targets/${row.id}/resume`, { method: 'PATCH' })
  } catch {
    await req(`/api/monitor-targets/${row.id}/resume`, { method: 'POST' })
  }
  ElMessage.success('已恢复自动探测')
  await loadTargets()
}

onMounted(loadTargets)
</script>

<template>
  <div class="page-header">
    <div class="page-title">监控目标管理</div>
    <div class="page-sub">工作台首页 / 监控目标管理</div>
  </div>

  <el-space direction="vertical" :size="16" style="width:100%">
    <el-card class="hero-card">
      <div>
        <div class="hero-eyebrow">监控接入治理</div>
        <div class="hero-title">监控目标管理</div>
        <div class="hero-sub">统一管理服务、IP、端口与探测策略，确保告警来源、探测频率和在线状态可追踪。</div>
      </div>
      <div class="hero-actions">
        <el-button @click="loadTargets">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增目标</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="6"><el-card class="stat-card stat-highlight-blue"><div class="stat-label">目标总数</div><div class="stat-value">{{ targetStats.total }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-green"><div class="stat-label">在线目标</div><div class="stat-value">{{ targetStats.online }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-red"><div class="stat-label">离线目标</div><div class="stat-value">{{ targetStats.offline }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-amber"><div class="stat-label">暂停探测</div><div class="stat-value">{{ targetStats.stopped }}</div></el-card></el-col>
    </el-row>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <strong>目标列表</strong>
          <div class="list-sub">建议为核心链路设置更短探测间隔和负责人</div>
        </div>
      </template>

      <el-row :gutter="8" style="margin-bottom:12px">
        <el-col :span="8"><el-input v-model="query" placeholder="按名称/IP搜索" /></el-col>
      </el-row>

      <el-table :data="filtered" v-loading="loading" stripe border table-layout="fixed" class="ops-table">
      <el-table-column prop="name" label="目标名称" min-width="180" />
      <el-table-column prop="ip" label="IP" width="150" align="center" />
      <el-table-column prop="port" label="端口" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :class="isOnline(scope.row) ? 'status-resolved' : 'status-open'">{{ isOnline(scope.row) ? '在线' : '离线' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="intervalSeconds" label="探测间隔(s)" width="120" align="center" />
      <el-table-column prop="lastProbeTime" label="最近探测时间" width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="300" fixed="right" align="center">
        <template #default="scope">
          <el-button type="primary" plain @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="danger" plain @click="removeTarget(scope.row)">删除</el-button>
          <el-button type="success" plain @click="probeNow(scope.row)">手动探测</el-button>
          <el-button v-if="normalizeEnabled(scope.row.enabled)==='Y'" type="warning" plain @click="stopProbe(scope.row)">停止探测</el-button>
          <el-button v-else type="success" plain @click="resumeProbe(scope.row)">继续探测</el-button>
        </template>
      </el-table-column>
      </el-table>
    </el-card>
  </el-space>

  <el-dialog v-model="dialogVisible" :title="editing ? '编辑目标' : '新增目标'" width="520px">
    <el-form label-width="110px">
      <el-form-item label="目标名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="IP 地址"><el-input v-model="form.ip" /></el-form-item>
      <el-form-item label="端口"><el-input v-model.number="form.port" type="number" /></el-form-item>
      <el-form-item label="探测间隔(s)"><el-input v-model.number="form.intervalSeconds" type="number" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible=false">取消</el-button>
      <el-button type="primary" @click="saveTarget">保存</el-button>
    </template>
  </el-dialog>
</template>