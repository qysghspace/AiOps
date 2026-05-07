# AIOps 智能运维平台

基于 **Spring Boot + Vue 3 + FastAPI Agent** 构建的 AIOps 智能运维演示平台，围绕真实运维场景中的“监控接入、告警识别、工单协同、AI 根因分析、处置流转、历史复盘、反馈闭环”形成完整业务链路。

---

## 1. 项目简介

传统运维系统通常存在以下问题：

- 告警数量多，缺乏优先级和上下文聚合；
- 工单流转与告警分析割裂；
- 故障排查依赖人工经验，处置效率低；
- 复盘结果难以沉淀为可复用知识；
- AI 分析结果缺少人工反馈闭环。

本项目通过引入 AIOps 思路，将监控目标管理、告警工单流转、AI 分析、相似原因召回、历史复盘和反馈机制集成到统一平台中，帮助运维人员完成从“发现问题”到“闭环复盘”的完整流程。

---

## 2. 核心功能

### 2.1 运维驾驶舱

前端首页提供统一值班视图，展示：

- 今日待处理数；
- 高优先级告警数量；
- 我的待办工单；
- 监控目标总数；
- 在线 / 离线监控目标；
- 超时待处理工单；
- 风险告警量；
- 已恢复事件；
- 快捷业务入口。

驾驶舱用于模拟真实运维值班场景，帮助用户快速判断当前平台风险状态。

### 2.2 监控目标管理

支持对业务系统、服务实例、IP 和端口进行统一管理：

- 新增监控目标；
- 编辑监控目标；
- 删除监控目标；
- 手动探测；
- 停止自动探测；
- 恢复自动探测；
- 统计目标总数、在线目标、离线目标、暂停探测数量。

该模块是告警生成和后续 AIOps 分析的基础入口。

### 2.3 告警与工单统一视图

平台提供告警与工单的统一协同页面，支持：

- 查看告警列表；
- 查看工单列表；
- 根据关键字搜索；
- 根据状态筛选；
- 根据告警等级筛选；
- 查看告警详情；
- 根据告警跳转到关联工单；
- 展示告警总数、高风险告警、工单总数、处理中工单。

该页面用于模拟实际运维中的事件协同中心。

### 2.4 告警详情与处置流程

告警详情页采用流程化处置设计，分为四个步骤：

1. 接收告警；
2. 确认影响范围；
3. AI 辅助排查；
4. 处置与复盘。

支持填写：

- 影响范围；
- 优先级；
- 负责人；
- AI 分析问题模板；
- 专家模式参数，例如 `sessionId`、`checkpoint`、是否流式。

AI 分析完成后可生成结构化结果和复盘摘要。

### 2.5 AI 工作台

AI 工作台用于多轮智能分析和会话管理，支持：

- 普通问答；
- 流式问答；
- 基于 `incidentId` 的上下文分析；
- 模板问题快速填充；
- 是否启用联网搜索；
- 图片上传与分析；
- 会话创建、切换、重命名、删除；
- 会话记忆查看；
- 清空会话记忆；
- checkpoint 回滚；
- 按步数回滚；
- 导出 Markdown 分析记录。

该模块用于模拟真实 AIOps Agent 的分析工作台。

### 2.6 AI 分析与历史入库

后端支持将 AI 分析结果保存到数据库，并提供历史查询能力：

- 事件分析；
- 分析结果入库；
- 历史分析查询；
- 复盘详情查看；
- 结构化 JSON 展示。

### 2.7 反馈闭环

平台支持用户对 AI 分析结果进行人工反馈：

- 提交真实原因；
- 标记误报；
- 记录选择的原因标签；
- 根据历史反馈查询相似原因；
- 为后续相似告警召回提供数据基础。

### 2.8 系统设置与用户中心

系统设置页面覆盖：

- 用户管理；
- 角色展示；
- 企业微信账号；
- AI 工具开关；
- Redis 健康检查；
- 默认探测间隔；
- 告警通知方式；
- 告警通知与升级策略。

示例通知策略包括：

- P1 / CRITICAL：企业微信 + 短信，5 分钟未确认自动升级；
- P2 / HIGH：企业微信，15 分钟未确认提醒值班长；
- P3 / P4：邮件，工作时间内汇总通知。

---

## 3. 技术栈

### 3.1 后端

- Java 17
- Spring Boot 3.3.4
- Spring Web
- Spring Validation
- Spring Actuator
- Spring Data Redis
- MyBatis-Plus 3.5.7
- MySQL Connector/J 8.4.0
- Lombok
- Maven

### 3.2 前端

- Vue 3.5+
- Vite 8
- Vue Router 4
- Element Plus 2.11+
- 原生 CSS
- Fetch API

### 3.3 AI Agent

- Python 3.10+
- FastAPI
- Uvicorn
- Pydantic
- Python Multipart
- LangChain Core
- LangChain OpenAI
- LangGraph
- Tavily Python

### 3.4 外部依赖

- MySQL 8+
- Redis 6+
- Ollama，可选
- DeepSeek-R1 模型，可选
- Tavily 搜索服务，可选

---

## 4. 项目结构

```text
aiops-platform/
├─ backend/                         # Spring Boot 后端服务
│  ├─ src/main/java/com/example/aiops/
│  │  ├─ controller/                 # 控制器层
│  │  ├─ service/                    # 业务服务层
│  │  ├─ mapper/                     # MyBatis-Plus Mapper
│  │  └─ entity/                     # 实体与请求对象
│  ├─ src/main/resources/
│  │  └─ application.yml             # 后端配置文件
│  └─ pom.xml                        # Maven 配置
│
├─ frontend-vue/                     # Vue 3 前端项目
│  ├─ src/
│  │  ├─ pages/                      # 页面组件
│  │  │  ├─ DashboardPage.vue        # 运维驾驶舱
│  │  │  ├─ AlertsWorkordersPage.vue # 告警与工单列表
│  │  │  ├─ IncidentDetailPage.vue   # 告警详情与处置
│  │  │  ├─ TargetsPage.vue          # 监控目标管理
│  │  │  ├─ AgentWorkbenchPage.vue   # AI 工作台
│  │  │  ├─ HistoryReviewPage.vue    # 历史复盘
│  │  │  └─ SettingsPage.vue         # 系统设置
│  │  ├─ router/                     # 前端路由
│  │  ├─ utils/                      # 工具函数
│  │  ├─ App.vue                     # 应用入口布局
│  │  ├─ main.js                     # Vue 挂载入口
│  │  └─ style.css                   # 全局样式
│  ├─ package.json
│  └─ vite.config.js
│
└─ ai-agent/                         # Python AI Agent 服务
   ├─ app/
   │  └─ main.py                     # FastAPI 入口
   └─ requirements.txt
```

---

## 5. 系统架构

整体架构如下：

```text
+-------------------+
|   Vue 3 Frontend  |
|  Element Plus UI  |
+---------+---------+
          |
          | HTTP / REST / Stream
          v
+-------------------+        +-------------------+
| Spring Boot API   | -----> |   MySQL Database  |
| Auth / Incident   |        | Alerts / Incidents |
| Monitor / AI BFF  |        | AI Analysis / FB   |
+---------+---------+        +-------------------+
          |
          | Redis / Agent API
          v
+-------------------+        +-------------------+
|   Redis Cache     |        | FastAPI AI Agent   |
| Health / Session  |        | LangChain/LangGraph|
+-------------------+        +---------+---------+
                                      |
                                      v
                             +-------------------+
                             | Ollama / LLM API  |
                             | Tavily Search     |
                             +-------------------+
```

---

## 6. 运行环境要求

请提前安装：

| 环境 | 推荐版本 |
|---|---|
| JDK | 17 |
| Maven | 3.9+ |
| Node.js | 18+ |
| npm | 9+ |
| Python | 3.10+ |
| MySQL | 8+ |
| Redis | 6+ |
| Ollama | 可选 |

---

## 7. 配置说明

后端配置文件位于：

```text
backend/src/main/resources/application.yml
```

当前关键配置示例：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aiops?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: 你的数据库名
    password: 你的数据库密码
  data:
    redis:
      host: 127.0.0.1
      port: 6379

ai:
  agent:
    base-url: http://localhost:8000
  ollama:
    base-url: http://localhost:11434
    model: deepseek-r1:7b
```

> 注意：生产环境请不要将数据库密码写死在配置文件中，建议改为环境变量或配置中心管理。

---

## 8. 数据库初始化建议

项目默认使用数据库：

```sql
CREATE DATABASE IF NOT EXISTS aiops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 8.1 反馈表

```sql
CREATE TABLE IF NOT EXISTS `ops_alert_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `alert_id` BIGINT NOT NULL,
  `incident_id` BIGINT DEFAULT NULL,
  `ai_result_id` BIGINT DEFAULT NULL,
  `selected_reasons` JSON DEFAULT NULL,
  `reason_text` TEXT,
  `is_false_positive` TINYINT(1) NOT NULL DEFAULT 0,
  `created_by` VARCHAR(64) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_id` (`alert_id`),
  KEY `idx_incident_id` (`incident_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.2 工单状态迁移

如果历史数据中存在旧状态，可执行：

```sql
UPDATE ops_incident SET status = 'OPEN' WHERE status = 'NEW';
UPDATE ops_incident SET status = 'IN_PROGRESS' WHERE status = 'ACK';
UPDATE ops_incident SET status = 'CLOSED' WHERE status = 'REVIEWED';
```

建议统一使用以下工单状态：

```text
OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
```

---

## 9. 启动方式

建议按以下顺序启动：

1. MySQL；
2. Redis；
3. AI Agent；
4. Backend；
5. Frontend。

### 9.1 启动 Redis

根据本机环境启动 Redis，默认连接地址：

```text
127.0.0.1:6379
```

### 9.2 启动 MySQL

确保数据库 `aiops` 已创建，并修改 `application.yml` 中的账号密码。

### 9.3 启动 AI Agent

进入 `ai-agent` 目录：

```bash
cd ai-agent
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

默认服务地址：

```text
http://localhost:8000
```

### 9.4 启动后端服务

进入 `backend` 目录：

```bash
cd backend
mvn spring-boot:run
```

默认服务地址：

```text
http://localhost:8080
```

### 9.5 启动前端服务

进入 `frontend-vue` 目录：

```bash
cd frontend-vue
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

### 9.6 前端生产构建

```bash
cd frontend-vue
npm run build
npm run preview
```

---

## 10. 前端路由说明

| 路由 | 页面 | 说明 |
|---|---|---|
| `/dashboard` | 工作台首页 | 运维驾驶舱与业务指标 |
| `/alerts` | 告警与工单列表 | 告警、工单统一协同视图 |
| `/incident/:incidentId?` | 告警详情与处置 | 流程化处置与 AI 分析 |
| `/targets` | 监控目标管理 | 目标新增、探测、暂停、恢复 |
| `/workbench` | AI 工作台 | 多轮对话、图片分析、会话管理 |
| `/history` | 会话历史与复盘 | 历史 AI 分析与复盘查看 |
| `/settings` | 系统设置 | 用户、工具、通知策略配置 |

---

## 11. 后端接口概览

### 11.1 认证接口

| 方法 | 地址 | 说明 |
|---|---|---|
| `POST` | `/api/auth/login` | 用户登录，返回 Token |

### 11.2 监控目标接口

| 方法 | 地址 | 说明 |
|---|---|---|
| `GET` | `/api/monitor-targets` | 查询监控目标列表 |
| `POST` | `/api/monitor-targets` | 新增监控目标 |
| `PUT/PATCH` | `/api/monitor-targets/{id}` | 更新监控目标 |
| `DELETE` | `/api/monitor-targets/{id}` | 删除监控目标 |
| `POST` | `/api/monitor-targets/{id}/probe` | 手动探测 |
| `PATCH/POST` | `/api/monitor-targets/{id}/stop` | 停止自动探测 |
| `PATCH/POST` | `/api/monitor-targets/{id}/resume` | 恢复自动探测 |
| `GET` | `/api/monitor-targets/{id}/alerts` | 查询目标关联告警 |

### 11.3 工单与事件接口

| 方法 | 地址 | 说明 |
|---|---|---|
| `GET` | `/api/incidents` | 查询工单 / 事件列表 |
| `GET` | `/api/incidents/{id}/detail` | 查询事件详情 |
| `PATCH` | `/api/incidents/{id}/status` | 修改事件状态 |
| `POST` | `/api/incidents/analyze` | 事件 AI 分析 |
| `POST` | `/api/incidents/analyze-stream` | 流式事件 AI 分析 |
| `GET` | `/api/incidents/analysis-history` | 查询 AI 分析历史 |

### 11.4 AI Agent 代理接口

| 方法 | 地址 | 说明 |
|---|---|---|
| `POST` | `/api/agent/chat` | AI 普通对话 |
| `POST` | `/api/agent/chat-stream` | AI 流式对话 |
| `POST` | `/api/agent/image` | 图片上传分析 |
| `GET` | `/api/agent/sessions/{sessionId}/memory` | 查询会话记忆 |
| `DELETE` | `/api/agent/sessions/{sessionId}/memory` | 清空会话记忆 |
| `POST` | `/api/agent/sessions/{sessionId}/rollback` | 会话回滚 |

### 11.5 反馈接口

| 方法 | 地址 | 说明 |
|---|---|---|
| `GET` | `/api/incidents/feedback/reasons` | 查询反馈原因选项 |
| `POST` | `/api/incidents/feedback` | 提交分析反馈 |
| `GET` | `/api/incidents/{alertId}/feedbacks` | 查询告警反馈历史 |
| `GET` | `/api/incidents/{alertId}/similar-causes` | 查询相似真实原因 |

---

## 12. 自动状态流转

后端内置工单状态自动流转调度任务，可通过配置开启：

```yaml
aiops:
  incident:
    auto:
      enabled: true
      scan-interval-ms: 60000
      resolve-after-minutes: 5
      close-after-minutes: 30
```

默认流转逻辑：

```text
OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
```

该机制用于模拟实际运维中工单生命周期自动推进或超时处理。

---

## 13. AI Agent 能力说明

AI Agent 主要承担以下职责：

- 根据事件上下文生成根因分析；
- 生成排查步骤；
- 生成首轮处置动作；
- 支持多轮会话；
- 支持会话记忆；
- 支持 checkpoint 回滚；
- 支持图片分析；
- 支持联网搜索开关；
- 支持导出会话分析记录。

典型分析输入：

```json
{
  "sessionId": "incident-1001",
  "incidentId": 1001,
  "message": "帮我分析该告警根因并给出排查步骤",
  "webSearchEnabled": true
}
```

典型输出包括：

- 问题理解；
- 候选根因；
- 排查步骤；
- 下一步动作；
- 复盘建议。

---

## 14. 页面设计说明

前端已统一为现代运维控制台风格：

- 登录页使用渐变背景和毛玻璃卡片；
- 主界面采用侧边栏 + 顶部栏 + 内容区布局；
- 每个核心页面都有业务 Hero 区域；
- 首页、告警页、目标页、历史页都有统计卡片；
- 状态标签统一使用颜色区分；
- 表格使用统一圆角卡片和阴影；
- AI 工作台采用双栏布局；
- 告警详情页采用流程化步骤条。

---

## 15. 常见问题

### 15.1 前端登录失败

请检查：

- 后端是否启动；
- 地址是否为 `http://localhost:8080`；
- `/api/auth/login` 是否正常返回；
- 浏览器控制台是否有跨域或网络错误。

### 15.2 工单列表没有数据

请检查：

- 数据库中是否存在 `ops_incident` 数据；
- `/api/incidents` 是否有返回；
- 工单状态是否为系统支持的状态；
- 后端日志是否有 SQL 错误。

### 15.3 告警能看到但无法进入详情

可能原因：

- 告警没有关联工单；
- `ops_incident.alert_id` 为空；
- 前端根据告警跳转时找不到对应 `incidentId`。

### 15.4 AI 分析失败

请检查：

- AI Agent 是否启动；
- `ai.agent.base-url` 是否配置正确；
- LLM / Ollama 是否可用；
- Agent 日志中是否存在模型调用异常；
- 请求是否超时。

### 15.5 Redis 健康检查失败

请检查：

- Redis 是否启动；
- 端口是否为 `6379`；
- `application.yml` 中 Redis 配置是否正确；
- 本机防火墙是否阻止访问。

---

## 16. 开发建议

后续可继续扩展：

- 增加完整 RBAC 权限模型；
- 增加真实告警源接入，例如 Prometheus Alertmanager；
- 增加 WebSocket 实时告警推送；
- 增加向量数据库，用于相似故障检索；
- 将反馈数据接入 RAG 知识库；
- 增加工单 SLA 超时升级；
- 增加短信、邮件、企业微信真实通知；
- 增加 Docker Compose 一键部署；
- 增加 Nginx 前端部署配置；
- 增加单元测试和接口测试；
- 增加系统操作审计日志。

---

## 17. 安全注意事项

当前项目为演示 / 原型项目，生产使用前建议：

- 数据库密码改为环境变量；
- Token 使用标准 JWT 并设置签名密钥；
- 增加接口权限校验；
- 增加登录失败限制；
- 增加 HTTPS；
- 增加敏感信息脱敏；
- 增加审计日志；
- 禁止将 `.env`、密钥、数据库密码提交到代码仓库。

---

## 18. 项目价值

本项目完整覆盖了 AIOps 平台的核心业务闭环：

```text
监控目标接入
   ↓
自动探测与告警产生
   ↓
告警与工单统一协同
   ↓
AI 根因分析与处置建议
   ↓
状态流转与人工确认
   ↓
历史复盘与反馈沉淀
   ↓
相似故障经验复用
```

通过该平台可以展示：

- 前后端分离开发能力；
- Spring Boot 后端工程能力；
- Vue 3 前端工程能力；
- AI Agent 集成能力；
- 数据库设计与业务建模能力；
- 智能运维业务理解能力。
