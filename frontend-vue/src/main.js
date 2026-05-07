import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'

const TOKEN_KEY = 'aiops_token'
const rawFetch = window.fetch.bind(window)
let handling401 = false

window.fetch = async (...args) => {
  const res = await rawFetch(...args)
  if (res.status === 401 && !handling401) {
    handling401 = true
    localStorage.removeItem(TOKEN_KEY)
    const current = window.location.pathname + window.location.search
    if (current !== '/') {
      window.location.href = '/'
    } else {
      window.location.reload()
    }
  }
  return res
}

createApp(App).use(router).use(ElementPlus).mount('#app')
