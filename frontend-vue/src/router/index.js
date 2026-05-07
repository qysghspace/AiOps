import { createRouter, createWebHistory } from 'vue-router'
import DashboardPage from '../pages/DashboardPage.vue'
import AlertsWorkordersPage from '../pages/AlertsWorkordersPage.vue'
import IncidentDetailPage from '../pages/IncidentDetailPage.vue'
import TargetsPage from '../pages/TargetsPage.vue'
import AgentWorkbenchPage from '../pages/AgentWorkbenchPage.vue'
import HistoryReviewPage from '../pages/HistoryReviewPage.vue'
import SettingsPage from '../pages/SettingsPage.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: DashboardPage },
  { path: '/alerts', component: AlertsWorkordersPage },
  { path: '/incident/:incidentId?', component: IncidentDetailPage },
  { path: '/targets', component: TargetsPage },
  { path: '/workbench', component: AgentWorkbenchPage },
  { path: '/history', component: HistoryReviewPage },
  { path: '/settings', component: SettingsPage },
]

export default createRouter({ history: createWebHistory(), routes })
