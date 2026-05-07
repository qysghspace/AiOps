<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const API = 'http://localhost:8080'
const tokenKey = 'aiops_token'

const token = ref(localStorage.getItem(tokenKey) || '')
const sessionId = ref('default-session')
const incidentId = ref('')
const message = ref('')
const sessions = ref([{ id: 'default-session', name: '默认会话', createdAt: new Date().toLocaleString() }])
const renameText = ref('')
const rollbackSteps = ref(1)
const checkpointSelect = ref('')

const uploadFile = ref(null)
const uploadProgress = ref(0)
const imageResult = ref({})
const memory = ref({})
const toolState = ref('等待调用')
const sessionSearch = ref('')
const webSearchEnabled = ref(true)

const chatList = ref([])
const checkpointIds = ref([])

const templates = [
  '分析最近支付超时异常，并给出排查步骤和根因建议',
  '基于当前 incidentId，生成本次故障的复盘报告',
  '根据告警内容，给出首轮处置动作建议',
  '上传的错误日志截图，帮我定位关键报错点和修复方案',
]

const isAuthed = computed(() => !!token.value)
const filteredSessions = computed(() => sessions.value.filter(s => !sessionSearch.value || s.id.includes(sessionSearch.value) || s.name.includes(sessionSearch.value)))

async function api(path, opt = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, opt.headers || {})
  if (token.value) headers.Authorization = `Bearer ${token.value}`
  const res = await fetch(`${API}${path}`, { ...opt, headers })
  const data = await res.json()
  if (res.status === 401) {
    localStorage.removeItem(tokenKey)
    token.value = ''
    throw new Error('unauthorized')
  }
  return data
}

function addMsg(role, title, content) {
  chatList.value.unshift({ role, title, content, ts: new Date().toLocaleString() })
}

function renderStructured(text) {
  const t = typeof text === 'string' ? text : JSON.stringify(text, null, 2)
  return {
    understanding: t.slice(0, 220),
    causes: ['候选1：下游依赖超时（置信度中）', '候选2：数据库慢查询（待确认）'],
    steps: ['确认影响范围', '检查最近变更', '查看日志和监控指标'],
    next: '建议先执行首轮处置并关联工单',
  }
}

function useTemplate(tpl) { message.value = tpl }

function createSession() {
  const id = `session-${Date.now()}`
  const s = { id, name: `会话 ${sessions.value.length + 1}`, createdAt: new Date().toLocaleString() }
  sessions.value.unshift(s)
  sessionId.value = id
}

function switchSession(id) {
  sessionId.value = id
  loadMemory()
}

function deleteSession(id) {
  sessions.value = sessions.value.filter(s => s.id !== id)
  if (sessionId.value === id) sessionId.value = sessions.value[0]?.id || 'default-session'
}

function renameSession() {
  const t = renameText.value.trim()
  if (!t) return
  const cur = sessions.value.find(s => s.id === sessionId.value)
  if (cur) cur.name = t
  renameText.value = ''
}

async function sendChat(stream = false) {
  const body = { sessionId: sessionId.value.trim(), incidentId: incidentId.value.trim() || null, message: message.value, webSearchEnabled: webSearchEnabled.value }
  addMsg('user', '用户输入', { raw: body.message })
  toolState.value = '工具调用中：get_incident_context / suggest_first_actions'

  try {
    if (!stream) {
      const j = await api('/api/agent/chat', { method: 'POST', body: JSON.stringify(body) })
      const d = j.data || j
      if (d?.error) ElMessage.error(d.error)
      const answer = d?.answer || d?.structured?.understanding || '后端返回为空'
      addMsg('ai', 'AI 分析结果', renderStructured(answer))
      toolState.value = '工具调用成功，结果已合并'
      return
    }

    const headers = { 'Content-Type': 'application/json' }
    if (token.value) headers.Authorization = `Bearer ${token.value}`
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), 65000)
    const res = await fetch(`${API}/api/agent/chat-stream`, { method: 'POST', headers, body: JSON.stringify(body), signal: controller.signal })
    clearTimeout(timer)
    if (!res.ok || !res.body) throw new Error(`流式请求失败：${res.status}`)

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buf = '', text = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      const chunks = buf.split('\n\n')
      buf = chunks.pop() || ''
      for (const block of chunks) {
        const line = block.split('\n').find((x) => x.startsWith('data: '))
        if (line) text += line.slice(6)
      }
    }
    addMsg('ai', 'AI 流式结果', renderStructured(text || '后端返回为空'))
    toolState.value = '工具调用成功，结果已合并'
  } catch (e) {
    const msg = e?.name === 'AbortError' ? '请求超时（65s）' : (e?.message || '请求失败')
    toolState.value = `调用失败：${msg}`
    ElMessage.error(msg)
    addMsg('ai', 'AI 调用失败', { raw: msg })
  }
}

async function doUpload(file) {
  const f = file || uploadFile.value
  if (!f) return
  uploadProgress.value = 20
  const form = new FormData()
  form.append('file', f)
  form.append('sessionId', sessionId.value.trim())
  const headers = {}
  if (token.value) headers.Authorization = `Bearer ${token.value}`
  uploadProgress.value = 50
  const res = await fetch(`${API}/api/agent/image`, { method: 'POST', headers, body: form })
  uploadProgress.value = 80
  const j = await res.json()
  const d = j.data || j
  imageResult.value = d
  addMsg('user', '图片上传', { raw: `已上传：${f.name}` })
  addMsg('ai', '多模态分析', renderStructured(d.analysis || d))
  uploadProgress.value = 100
  setTimeout(() => (uploadProgress.value = 0), 1200)
}

async function loadMemory() {
  const j = await api(`/api/agent/sessions/${encodeURIComponent(sessionId.value)}/memory`)
  const d = j.data || j
  memory.value = d
  checkpointIds.value = d.checkpointIds || []
}

async function clearMemory() {
  await api(`/api/agent/sessions/${encodeURIComponent(sessionId.value)}/memory`, { method: 'DELETE' })
  memory.value = {}
  checkpointIds.value = []
}

async function rollbackCheckpoint() {
  if (!checkpointSelect.value) return alert('请先选择 checkpoint')
  await api(`/api/agent/sessions/${encodeURIComponent(sessionId.value)}/rollback`, {
    method: 'POST', body: JSON.stringify({ toCheckpointId: checkpointSelect.value }),
  })
  await loadMemory()
}

async function rollbackBySteps() {
  await api(`/api/agent/sessions/${encodeURIComponent(sessionId.value)}/rollback`, {
    method: 'POST', body: JSON.stringify({ steps: Number(rollbackSteps.value || 1) }),
  })
  await loadMemory()
}

function onDrop(e) {
  e.preventDefault()
  const f = e.dataTransfer.files?.[0]
  if (f) {
    uploadFile.value = f
    doUpload(f)
  }
}

function exportMarkdown() {
  const lines = []
  lines.push('# AIOps 会话导出\n')
  lines.push(`- sessionId: ${sessionId.value}`)
  lines.push(`- incidentId: ${incidentId.value || '-'}\n`)
  chatList.value.slice().reverse().forEach((m) => {
    lines.push(`## ${m.title} (${m.ts})`)
    lines.push(`角色: ${m.role}`)
    lines.push('```json')
    lines.push(JSON.stringify(m.content, null, 2))
    lines.push('```\n')
  })
  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `session-${sessionId.value || 'default'}.md`
  a.click()
  URL.revokeObjectURL(a.href)
}
</script>

<template>
  <div v-if="!isAuthed">请先登录后使用 AI 工作台。</div>
  <template v-else>
    <div class="page-header">
      <div class="page-title">AI 工作台</div>
      <div class="page-sub">工作台首页 / AI 工作台</div>
    </div>

    <el-space direction="vertical" :size="16" style="width:100%">
      <el-card class="hero-card workbench-hero">
        <div>
          <div class="hero-eyebrow">AI Copilot</div>
          <div class="hero-title">AI 工作台</div>
          <div class="hero-sub">面向值班工程师的多轮诊断空间，支持告警上下文分析、联网搜索、图片日志识别、会话记忆与回滚。</div>
        </div>
        <div class="tool-status-card">{{ toolState }}</div>
      </el-card>

      <section class="workspace">
        <el-card class="workbench-card chat-panel">
          <template #header>
            <div class="section-header">
              <strong>智能对话区</strong>
              <el-switch v-model="webSearchEnabled" active-text="联网搜索" />
            </div>
          </template>

          <div class="tpl-grid">
            <el-button v-for="tpl in templates" :key="tpl" class="tpl" plain @click="useTemplate(tpl)">{{ tpl }}</el-button>
          </div>

          <el-row :gutter="12" class="form-row">
            <el-col :span="12"><el-input v-model="sessionId" placeholder="sessionId" /></el-col>
            <el-col :span="12"><el-input v-model="incidentId" placeholder="incidentId（可选）" /></el-col>
          </el-row>

          <el-input v-model="message" type="textarea" :rows="5" placeholder="请输入需要 AI 协助分析的问题，例如：结合当前告警上下文给出根因候选和处置步骤" />

          <div class="action-row">
            <el-button type="primary" @click="sendChat(false)">发送（非流式）</el-button>
            <el-button type="primary" plain @click="sendChat(true)">发送（流式）</el-button>
          </div>

          <div class="upload-card" @dragover.prevent @drop="onDrop">
            <div>
              <div class="upload-title">图片 / 日志截图分析</div>
              <div class="upload-sub">拖拽图片到此处，或选择截图后上传，让 AI 提取关键报错点。</div>
            </div>
            <input type="file" accept="image/*" @change="(e)=>uploadFile=e.target.files[0]" />
            <div class="action-row compact">
              <el-button type="success" @click="doUpload()">上传并分析</el-button>
              <el-button @click="doUpload(uploadFile)">失败重试</el-button>
            </div>
            <el-progress v-if="uploadProgress" :percentage="uploadProgress" />
          </div>

          <div class="chat-list">
            <el-empty v-if="!chatList.length" description="暂无对话，选择模板或输入问题开始分析" />
            <div v-for="(m,idx) in chatList" :key="idx" class="msg" :class="m.role">
              <div class="mh">{{ m.title }} · {{ m.ts }}</div>
              <div v-if="m.content.understanding" class="structured-result">
                <div class="result-block"><strong>问题理解</strong><p>{{ m.content.understanding }}</p></div>
                <div class="result-block"><strong>根因候选</strong><ul><li v-for="c in m.content.causes" :key="c">{{ c }}</li></ul></div>
                <div class="result-block"><strong>排查步骤</strong><ol><li v-for="s in m.content.steps" :key="s">{{ s }}</li></ol></div>
                <div class="result-block"><strong>下一步动作</strong><p>{{ m.content.next }}</p></div>
              </div>
              <div v-else>{{ m.content.raw || m.content }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="workbench-card session-panel">
          <template #header><strong>会话管理</strong></template>

          <el-row :gutter="10" class="form-row">
            <el-col :span="12"><el-button style="width:100%" @click="createSession">新建会话</el-button></el-col>
            <el-col :span="12"><el-button style="width:100%" @click="loadMemory">刷新记忆</el-button></el-col>
          </el-row>
          <el-row :gutter="10" class="form-row">
            <el-col :span="12"><el-button style="width:100%" @click="clearMemory">清空记忆</el-button></el-col>
            <el-col :span="12"><el-button type="success" style="width:100%" @click="exportMarkdown">导出 Markdown</el-button></el-col>
          </el-row>

          <el-input v-model="renameText" placeholder="当前会话重命名" class="mb-10">
            <template #append><el-button @click="renameSession">重命名</el-button></template>
          </el-input>

          <el-input v-model="sessionSearch" placeholder="搜索会话" class="mb-10" />
          <div class="session-list">
            <div v-for="s in filteredSessions" :key="s.id" class="session-item" :class="{active:s.id===sessionId}">
              <div @click="switchSession(s.id)" style="cursor:pointer;min-width:0">
                <div><strong>{{ s.name }}</strong></div>
                <div class="meta">{{ s.id }} · {{ s.createdAt }}</div>
              </div>
              <el-button size="small" @click="deleteSession(s.id)">删除</el-button>
            </div>
          </div>

          <el-select v-model="checkpointSelect" placeholder="选择 checkpoint 回滚" style="width:100%;margin-bottom:10px">
            <el-option v-for="cp in checkpointIds" :key="cp" :label="cp" :value="cp" />
          </el-select>
          <el-button type="primary" plain style="width:100%;margin-bottom:10px" @click="rollbackCheckpoint">回滚到选中 checkpoint</el-button>
          <el-input-number v-model="rollbackSteps" :min="1" style="width:100%;margin-bottom:10px" />
          <el-button type="primary" plain style="width:100%" @click="rollbackBySteps">按步回滚</el-button>

          <div class="small-title">图片分析结果</div><pre class="log">{{ JSON.stringify(imageResult,null,2) }}</pre>
          <div class="small-title">会话记忆</div><pre class="log">{{ JSON.stringify(memory,null,2) }}</pre>
        </el-card>
      </section>
    </el-space>
  </template>
</template>

<style scoped>
.workspace{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(360px,.95fr);gap:18px;align-items:start}
.workbench-card{min-height:520px}.chat-panel{overflow:hidden}.session-panel{position:sticky;top:18px}
.section-header{display:flex;justify-content:space-between;align-items:center;gap:12px}
.tool-status-card{position:relative;z-index:1;min-width:220px;padding:14px 16px;border-radius:14px;background:#fff;color:#1d4ed8;font-size:13px;font-weight:700;box-shadow:0 10px 24px rgba(37,99,235,.12)}
.tpl-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:14px}.tpl{height:auto;min-height:46px;white-space:normal;line-height:1.45;background:#f8fbff!important;border-color:#dbeafe!important;color:#0f172a!important}
.form-row{margin-bottom:12px}.action-row{display:flex;justify-content:flex-end;gap:10px;margin:12px 0}.action-row.compact{justify-content:flex-start}.upload-card{display:grid;gap:10px;margin-top:14px;padding:16px;border:1px dashed #93c5fd;border-radius:16px;background:linear-gradient(135deg,#f8fbff,#fff)}
.upload-title{font-size:15px;font-weight:800;color:#0f172a}.upload-sub{font-size:12px;color:#64748b;margin-top:4px}.chat-list{max-height:520px;overflow:auto;display:grid;gap:12px;margin-top:14px;padding-right:4px}.msg{border:1px solid #e2e8f0;border-radius:16px;padding:14px;background:#fff}.msg.user{border-color:#bfdbfe;background:#eff6ff}.msg.ai{border-color:#bbf7d0;background:#f0fdf4}.mh{font-size:12px;color:#64748b;margin-bottom:8px}.structured-result{display:grid;gap:10px}.result-block{padding:10px;border-radius:12px;background:rgba(255,255,255,.72)}.result-block p{margin:6px 0 0;line-height:1.7}.result-block ul,.result-block ol{margin:8px 0 0;padding-left:20px;line-height:1.8}.meta{font-size:12px;color:#64748b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:4px}.mb-10{margin-bottom:10px}.small-title{font-size:12px;color:#64748b;margin:12px 0 6px}.log{max-height:170px;overflow:auto;background:#0f172a;color:#e2e8f0;border-radius:12px;padding:10px;font-size:12px}.session-list{max-height:230px;overflow:auto;display:grid;gap:8px;margin-bottom:12px}.session-item{display:flex;justify-content:space-between;align-items:center;gap:8px;border:1px solid #e2e8f0;border-radius:14px;padding:10px;background:#fff}.session-item.active{border-color:#2563eb;background:#eff6ff;box-shadow:0 8px 18px rgba(37,99,235,.1)}
@media (max-width:1200px){.workspace{grid-template-columns:1fr}.session-panel{position:static}.tpl-grid{grid-template-columns:1fr}}
</style>