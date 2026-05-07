<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const API = 'http://localhost:8080'
const tokenKey = 'aiops_token'
const route = useRoute()

const currentStep = ref(0)
const stepLabels = ['接收告警', '确认影响', 'AI 辅助排查', '处置与复盘']

const alerts = ref([])
const current = ref(null)
const aiResult = ref(null)
const loading = ref(false)

const impact = ref({
  scope: '',
  priority: 'P2',
  owner: '',
})

const template = ref('帮我分析该告警根因并给出排查步骤')
const templates = [
  '帮我分析该告警根因并给出排查步骤',
  '基于现有信息生成处置建议（含验证步骤）',
  '生成适合复盘沉淀的一页总结',
]

const expertMode = ref(false)
const expertParams = ref({ sessionId: '', stream: false, checkpoint: '' })

function authHeaders() {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem(tokenKey)
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

const canNext = computed(() => {
  if (currentStep.value === 0) return !!current.value
  if (currentStep.value === 1) return !!impact.value.scope && !!impact.value.owner
  if (currentStep.value === 2) return !!aiResult.value
  return true
})

const canPrev = computed(() => currentStep.value > 0)

function statusType(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'NEW') return 'primary'
  if (s === 'IN_PROGRESS') return 'warning'
  if (s === 'RESOLVED' || s === 'CLOSED') return 'success'
  return 'info'
}

async function loadAlerts() {
  loading.value = true
  try {
    const res = await fetch(`${API}/api/incidents`, { headers: authHeaders() })
    const j = await res.json()
    alerts.value = (j?.data || []).map((x) => ({
      id: x.id,
      title: x.title || `告警-${x.id}`,
      status: String(x.status || 'NEW').toUpperCase(),
      severity: x.severity || 'MEDIUM',
      owner: x.owner || x.assignee || '未指派',
      detail: x,
    }))

    const routeIncidentId = route.params.incidentId
    if (routeIncidentId) {
      const found = alerts.value.find((x) => String(x.id) === String(routeIncidentId))
      if (found) current.value = found
    }
    if (!current.value && alerts.value.length) current.value = alerts.value[0]
  } finally {
    loading.value = false
  }
}

function chooseAlert(item) {
  current.value = item
  currentStep.value = 0
  aiResult.value = null
}

function nextStep() {
  if (!canNext.value) {
    ElMessage.warning('请先完成当前步骤必填信息')
    return
  }
  if (currentStep.value < 3) currentStep.value += 1
}

function prevStep() {
  if (currentStep.value > 0) currentStep.value -= 1
}

async function runAi() {
  if (!current.value) {
    ElMessage.error('请先选择告警')
    return
  }
  const body = {
    sessionId: expertMode.value ? (expertParams.value.sessionId || `incident-${current.value.id}`) : `incident-${current.value.id}`,
    incidentId: current.value.id,
    message: template.value,
  }
  const res = await fetch(`${API}/api/agent/chat`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(body),
  })
  const j = await res.json()
  const ans = j?.data?.answer || j?.answer || 'AI 暂无明确结论'
  aiResult.value = {
    summary: String(ans).slice(0, 260),
    actions: [
      '检查监控与日志时间点是否一致',
      '验证核心依赖可达性与错误率趋势',
      '执行恢复动作并观察 15 分钟',
    ],
    postmortem: '本次告警建议纳入容量预警阈值优化。',
  }
  ElMessage.success('AI 分析完成')
}

function generatePostmortem() {
  if (!aiResult.value) {
    ElMessage.warning('请先完成 AI 分析')
    return
  }
  currentStep.value = 3
  ElMessage.success('已生成复盘摘要')
}

onMounted(loadAlerts)
</script>

<template>
  <div class="page-header">
    <div class="page-title">告警详情与处置</div>
    <div class="page-sub">工作台首页 / 告警详情与AI分析</div>
  </div>

  <el-space direction="vertical" :size="16" style="width:100%" v-loading="loading">
    <el-card class="hero-card incident-hero">
      <div>
        <div class="hero-eyebrow">事件处置流程</div>
        <div class="hero-title">告警详情与处置</div>
        <div class="hero-sub">按真实值班流程完成告警确认、影响评估、AI 辅助分析、处置闭环与复盘沉淀。</div>
      </div>
      <div class="incident-summary" v-if="current">
        <div class="summary-label">当前告警</div>
        <div class="summary-title">{{ current.title }}</div>
        <div class="summary-meta">{{ current.severity }} · {{ current.owner }} · {{ current.status }}</div>
      </div>
    </el-card>

    <el-card class="step-card modern-step-card">
      <el-steps :active="currentStep" align-center finish-status="success">
        <el-step v-for="(s, idx) in stepLabels" :key="s" :title="`${idx + 1}. ${s}`" />
      </el-steps>
    </el-card>

    <el-row :gutter="18" class="incident-layout">
      <el-col :span="8">
        <el-card class="incident-list-card">
          <template #header><strong>告警列表</strong></template>
          <el-empty v-if="!alerts.length" description="暂无告警，去接入告警源" />
          <div v-else class="incident-alert-list-scroll">
            <el-space direction="vertical" fill style="width:100%">
              <div class="list-item" :class="{ active: current?.id === item.id }" v-for="item in alerts" :key="item.id" @click="chooseAlert(item)">
                <div>
                  <div class="list-title">{{ item.title }}</div>
                  <div style="font-size:12px;color:#6b7280">{{ item.severity }} · {{ item.owner }}</div>
                </div>
                <el-tag :type="statusType(item.status)">{{ item.status }}</el-tag>
              </div>
            </el-space>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="incident-process-card">
          <template #header>
            <div class="section-header">
              <strong>当前告警处理</strong>
              <el-tag v-if="current" :type="statusType(current.status)">{{ current.status }}</el-tag>
            </div>
          </template>

          <el-empty v-if="!current" description="请选择左侧告警开始处理" />
          <template v-else>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
              <el-descriptions-item label="状态"><el-tag :type="statusType(current.status)">{{ current.status }}</el-tag></el-descriptions-item>
              <el-descriptions-item label="等级">{{ current.severity }}</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ current.owner }}</el-descriptions-item>
            </el-descriptions>

            <el-divider />

            <template v-if="currentStep === 0">
              <div class="step-panel soft-blue">
                <div class="step-panel-title">已接收告警</div>
                <div class="step-panel-desc">请确认告警来源、负责人和当前状态，随后进入影响范围确认。建议优先查看是否存在重复告警或同源工单。</div>
              </div>
            </template>

            <template v-else-if="currentStep === 1">
              <el-form label-width="88px" class="impact-form">
                <el-form-item label="影响范围">
                  <el-input v-model="impact.scope" placeholder="例如：支付链路 / 华北可用区 / 核心订单服务" />
                </el-form-item>
                <el-row :gutter="12">
                  <el-col :span="8">
                    <el-form-item label="优先级">
                      <el-select v-model="impact.priority" style="width:100%">
                        <el-option label="P1" value="P1" />
                        <el-option label="P2" value="P2" />
                        <el-option label="P3" value="P3" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="16">
                    <el-form-item label="负责人">
                      <el-input v-model="impact.owner" placeholder="填写处理负责人" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </template>

            <template v-else-if="currentStep === 2">
              <el-space direction="vertical" fill style="width:100%">
                <el-select v-model="template" placeholder="选择问题模板">
                  <el-option v-for="t in templates" :key="t" :label="t" :value="t" />
                </el-select>
                <el-button type="primary" class="main-action" @click="runAi">让 AI 分析</el-button>
                <el-card shadow="never" v-if="aiResult">
                  <template #header>结构化结果</template>
                  <div>{{ aiResult.summary }}</div>
                  <el-divider />
                  <ul>
                    <li v-for="a in aiResult.actions" :key="a">{{ a }}</li>
                  </ul>
                  <el-button type="success" @click="generatePostmortem">基于结果生成处置建议</el-button>
                </el-card>
              </el-space>
            </template>

            <template v-else>
              <el-result icon="success" title="处置完成" sub-title="状态已流转，复盘摘要已沉淀。" />
              <el-card shadow="never">
                <template #header>复盘摘要</template>
                <div>{{ aiResult?.postmortem || '暂无摘要' }}</div>
              </el-card>
              <el-space style="margin-top:12px">
                <el-button type="primary" @click="current.status='RESOLVED'">标记 RESOLVED</el-button>
                <el-button @click="current.status='IN_PROGRESS'">回到 IN_PROGRESS</el-button>
              </el-space>
            </template>

            <el-divider />
            <el-collapse>
              <el-collapse-item title="专家模式（高级设置）" name="expert">
                <el-switch v-model="expertMode" active-text="启用专家模式" />
                <el-form label-width="100px" style="margin-top:8px">
                  <el-form-item label="sessionId"><el-input v-model="expertParams.sessionId" /></el-form-item>
                  <el-form-item label="checkpoint"><el-input v-model="expertParams.checkpoint" /></el-form-item>
                  <el-form-item label="流式"><el-switch v-model="expertParams.stream" /></el-form-item>
                </el-form>
              </el-collapse-item>
            </el-collapse>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="incident-footer-card incident-footer-sticky">
      <div style="display:flex;justify-content:flex-end;gap:8px">
        <el-button :disabled="!canPrev" @click="prevStep">上一步</el-button>
        <el-button type="primary" class="main-action" @click="nextStep">下一步</el-button>
      </div>
    </el-card>
  </el-space>
</template>