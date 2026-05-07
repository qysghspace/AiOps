<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const API = 'http://localhost:8080'
const tokenKey = 'aiops_token'

const loading = ref(false)
const sessions = ref([])
const keyword = ref('')
const detailVisible = ref(false)
const detail = ref(null)

function authHeaders() {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem(tokenKey)
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

function fmtTime(v) {
  if (!v) return '-'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  return d.toLocaleString()
}

async function loadHistory() {
  loading.value = true
  try {
    const res = await fetch(`${API}/api/incidents/analysis-history`, { headers: authHeaders() })
    const j = await res.json()
    if (j?.code !== '0') {
      ElMessage.error(j?.message || '加载历史失败')
      return
    }
    sessions.value = (j?.data || []).map((x) => ({
      id: x.id,
      time: fmtTime(x.createdAt || x.gmtCreate || x.updatedAt),
      owner: x.owner || x.assignee || '未指派',
      conclusion: (x.rootCause || '未填写结论').slice(0, 56),
      status: x.suggestion ? 'RESOLVED' : 'IN_PROGRESS',
      raw: x,
    }))
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  const k = keyword.value.trim()
  if (!k) return sessions.value
  return sessions.value.filter((x) => `${x.id}${x.time}${x.owner}${x.conclusion}${JSON.stringify(x.raw)}`.includes(k))
})

const historyStats = computed(() => ({
  total: sessions.value.length,
  resolved: sessions.value.filter((x) => x.status === 'RESOLVED').length,
  pending: sessions.value.filter((x) => x.status !== 'RESOLVED').length,
  owners: new Set(sessions.value.map((x) => x.owner).filter(Boolean)).size,
}))

function openDetail(item) {
  detail.value = item
  detailVisible.value = true
}

onMounted(loadHistory)
</script>

<template>
  <div class="page-header">
    <div class="page-title">会话历史与复盘</div>
    <div class="page-sub">工作台首页 / 会话历史与复盘</div>
  </div>

  <el-space direction="vertical" :size="16" style="width:100%" v-loading="loading">
    <el-card class="hero-card">
      <div>
        <div class="hero-eyebrow">知识复盘中心</div>
        <div class="hero-title">会话历史与复盘</div>
        <div class="hero-sub">沉淀 AI 分析结论、真实处置原因与负责人信息，便于后续相似故障召回和经验复用。</div>
      </div>
      <div class="hero-actions">
        <el-input v-model="keyword" placeholder="搜索时间 / 结论 / 处理人" style="width:260px" />
        <el-button @click="loadHistory">刷新</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="6"><el-card class="stat-card stat-highlight-blue"><div class="stat-label">复盘总数</div><div class="stat-value">{{ historyStats.total }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-green"><div class="stat-label">已闭环</div><div class="stat-value">{{ historyStats.resolved }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-amber"><div class="stat-label">待完善</div><div class="stat-value">{{ historyStats.pending }}</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card stat-highlight-blue"><div class="stat-label">涉及处理人</div><div class="stat-value">{{ historyStats.owners }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col v-for="item in filtered" :key="item.id" :span="8">
        <el-card class="review-card" @click="openDetail(item)">
          <div class="review-time">{{ item.time }}</div>
          <div class="review-title">{{ item.conclusion }}</div>
          <div class="review-owner">处理人：{{ item.owner }}</div>
          <el-tag :type="item.status === 'RESOLVED' ? 'success' : 'warning'" style="margin-top:8px">{{ item.status }}</el-tag>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!filtered.length" description="暂无复盘记录，去完成一条处置并生成摘要" />
  </el-space>

  <el-drawer v-model="detailVisible" title="复盘详情" size="50%">
    <el-timeline v-if="detail">
      <el-timeline-item :timestamp="detail.time" type="primary">完成告警处置并沉淀复盘结论</el-timeline-item>
      <el-timeline-item type="success">结论：{{ detail.conclusion }}</el-timeline-item>
      <el-timeline-item type="info">处理人：{{ detail.owner }}</el-timeline-item>
    </el-timeline>

    <el-divider />
    <div style="font-size:13px;color:#6b7280;margin-bottom:8px">技术详情 JSON（默认折叠到详情中）</div>
    <pre class="json-block">{{ JSON.stringify(detail?.raw || {}, null, 2) }}</pre>
  </el-drawer>
</template>