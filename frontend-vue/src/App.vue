<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isFirstUse, markFeatureUsed } from './utils/firstUse'

const API = 'http://localhost:8080'
const tokenKey = 'aiops_token'

const route = useRoute()
const router = useRouter()

const token = ref(localStorage.getItem(tokenKey) || '')
const username = ref('')
const password = ref('')

const isAuthed = computed(() => !!token.value)
const userDisplayName = computed(() => username.value || '值班工程师')

const menus = [
  { path: '/dashboard', label: '工作台首页', feature: 'dashboard' },
  { path: '/alerts', label: '告警与工单列表', feature: 'alerts' },
  { path: '/incident', label: '告警详情 + AI分析', feature: 'incident' },
  { path: '/targets', label: '监控目标管理', feature: 'targets' },
  { path: '/workbench', label: 'AI 工作台', feature: 'workbench' },
  { path: '/history', label: '会话历史与复盘', feature: 'history' },
  { path: '/settings', label: '系统配置与用户中心', feature: 'settings' },
]

async function doLogin() {
  try {
    const res = await fetch(`${API}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    })
    const j = await res.json()
    if (!res.ok || j?.code !== '0' || !j?.data?.token) {
      ElMessage.error(j?.message || '登录失败，请检查账号密码')
      return
    }
    token.value = j.data.token
    localStorage.setItem(tokenKey, token.value)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error('登录请求失败，请检查后端服务')
  }
}

async function onMenuFirstUse(m) {
  if (!m?.feature) return
  if (!isFirstUse(m.feature)) return

  const tips = {
    dashboard: '这里是运维驾驶舱：先看全局状态，再从快捷入口进入告警或AI分析。',
    alerts: '这里可以查看告警并推进状态流转，建议先处理 NEW/高优先级告警。',
    incident: '这里是核心AI分析页：先加载告警上下文，再使用模板问题发起分析。',
    targets: '先新增监控目标并绑定端口与探测策略，告警链路才会完整。',
    workbench: '这里可进行多轮 AI 对话、图片分析、会话回滚，并可切换联网搜索开关。',
    history: '这里可回看分析会话并做复盘，支持关联告警/工单。',
    settings: '这里管理系统工具与通知配置，建议先确认后端服务健康状态。',
  }

  await ElMessageBox.alert(tips[m.feature] || '首次使用提示', `首次使用：${m.label}`, {
    confirmButtonText: '我知道了',
    type: 'info',
  })
  markFeatureUsed(m.feature)
}

function logout() {
  localStorage.removeItem(tokenKey)
  token.value = ''
  router.push('/')
}
</script>

<template>
  <div v-if="!isAuthed" class="login-wrap">
    <div class="login-bg"></div>
    <el-card class="login-card glass-card">
      <div class="login-brand">AIOps 智能运维平台</div>
      <div class="login-title">欢迎登录统一运维工作台</div>
      <div class="login-sub">覆盖告警接入、工单协同、AI分析、监控治理与复盘闭环</div>
      <el-form label-width="70" @submit.prevent>
        <el-form-item label="账号"><el-input v-model="username" placeholder="请输入账号" /></el-form-item>
        <el-form-item label="密码"><el-input type="password" v-model="password" placeholder="请输入密码" show-password /></el-form-item>
        <el-button type="primary" class="full-width" @click="doLogin">登录工作台</el-button>
      </el-form>
      <div class="login-footer-tip">建议使用管理员账号查看完整业务链路</div>
    </el-card>
  </div>

  <el-container v-else class="layout-shell">
    <el-aside width="252px" class="aside">
      <div class="brand-block">
        <div class="brand-mark">AIOps</div>
        <div>
          <div class="brand">智能运维平台</div>
          <div class="brand-sub">Production Operations Console</div>
        </div>
      </div>
      <div class="nav-section-title">功能导航</div>
      <el-menu :default-active="route.path" router unique-opened class="side-menu">
        <el-menu-item
          v-for="m in menus"
          :key="m.path"
          :index="m.path"
          @click="onMenuFirstUse(m)"
        >
          {{ m.label }}
        </el-menu-item>
      </el-menu>
      <div class="aside-footer glass-card">
        <div class="aside-footer-title">当前值班建议</div>
        <div class="aside-footer-text">优先关注 P1/P2 告警、超时未处理工单与离线监控目标。</div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>
          <div class="header-title">端口监听 → 告警聚合 → AI分析 → 工单闭环</div>
          <div class="header-sub">让监控、处置、复盘更接近真实业务运维流程</div>
        </div>
        <div class="header-actions">
          <div class="user-chip">{{ userDisplayName }}</div>
          <el-button @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.login-wrap{position:relative;display:flex;align-items:center;justify-content:center;min-height:100vh;padding:24px;background:linear-gradient(135deg,#0f172a 0%,#1d4ed8 45%,#60a5fa 100%);overflow:hidden}
.login-bg{position:absolute;inset:0;background:radial-gradient(circle at 20% 20%,rgba(255,255,255,.18),transparent 30%),radial-gradient(circle at 80% 30%,rgba(255,255,255,.12),transparent 28%),radial-gradient(circle at 60% 80%,rgba(255,255,255,.1),transparent 24%)}
.login-card{position:relative;width:460px;padding:10px 6px}
.login-brand{display:inline-flex;align-items:center;padding:6px 12px;border-radius:999px;background:rgba(59,130,246,.12);color:#2563eb;font-size:12px;font-weight:700;margin-bottom:14px}
.login-title{font-size:28px;font-weight:800;color:#0f172a;line-height:1.2}
.login-sub{margin:10px 0 24px;color:#475569;line-height:1.7;font-size:14px}
.full-width{width:100%}
.login-footer-tip{margin-top:14px;font-size:12px;color:#64748b;text-align:center}
.layout-shell{min-height:100vh;background:linear-gradient(180deg,#f8fbff 0%,#f3f7fc 100%)}
.aside{display:flex;flex-direction:column;gap:16px;padding:20px 16px;border-right:1px solid rgba(148,163,184,.18);background:rgba(255,255,255,.72);backdrop-filter:blur(14px)}
.brand-block{display:flex;gap:12px;align-items:center;padding:8px 8px 2px}
.brand-mark{display:flex;align-items:center;justify-content:center;width:48px;height:48px;border-radius:16px;background:linear-gradient(135deg,#2563eb,#60a5fa);color:#fff;font-weight:800;box-shadow:0 12px 24px rgba(37,99,235,.25)}
.brand{font-weight:800;font-size:18px;color:#0f172a}
.brand-sub{font-size:12px;color:#64748b;margin-top:4px}
.nav-section-title{padding:0 8px;font-size:12px;font-weight:700;letter-spacing:.08em;color:#94a3b8}
.side-menu{border-right:none;background:transparent}
.aside-footer{margin-top:auto;padding:14px}
.aside-footer-title{font-size:13px;font-weight:700;color:#0f172a;margin-bottom:6px}
.aside-footer-text{font-size:12px;color:#64748b;line-height:1.7}
.header{display:flex;justify-content:space-between;align-items:center;padding:20px 28px;border-bottom:1px solid rgba(226,232,240,.9);background:rgba(255,255,255,.76);backdrop-filter:blur(10px)}
.header-title{font-size:16px;font-weight:800;color:#0f172a}
.header-sub{font-size:12px;color:#64748b;margin-top:6px}
.header-actions{display:flex;align-items:center;gap:12px}
.user-chip{padding:8px 14px;border-radius:999px;background:#eff6ff;color:#1d4ed8;font-size:13px;font-weight:700}
.main-content{padding:24px}
</style>
