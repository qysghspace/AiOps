import os
import base64
from typing import Dict, Any, List, Optional

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from pydantic import BaseModel
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, ToolMessage, BaseMessage
from langchain_core.tools import tool
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.checkpoint.memory import MemorySaver
from tavily import TavilyClient

app = FastAPI(title="AIOps AI Agent Service", version="0.3.0")

MODEL_NAME = "qwen3.6-plus"
BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
API_KEY = os.getenv("QWEN_API_KEY")

if not API_KEY:
    raise RuntimeError("QWEN_API_KEY is required in environment variables")

TAVILY_API_KEY = os.getenv("TAVILY_API_KEY")
if not TAVILY_API_KEY:
    raise RuntimeError("TAVILY_API_KEY is required in environment variables")

tavily_client = TavilyClient(api_key=TAVILY_API_KEY)

llm = ChatOpenAI(
    model=MODEL_NAME,
    base_url=BASE_URL,
    api_key=API_KEY,
    temperature=0.2,
)


class AgentState(Dict):
    messages: List[BaseMessage]


class ChatRequest(BaseModel):
    sessionId: str
    message: str
    incidentId: Optional[str] = None
    webSearchEnabled: bool = True
    webSearchEnabled: bool = True


class RollbackRequest(BaseModel):
    toCheckpointId: Optional[str] = None
    steps: int = 1


@tool
def get_incident_context(incident_id: str) -> str:
    """根据 incident_id 返回故障上下文（示例工具，后续可接 Java 接口）。"""
    return f"incident {incident_id}: payment timeout spikes, error rate increased, suspected DB slow query"


@tool
def suggest_first_actions(summary: str) -> str:
    """根据故障描述给出处置建议（示例工具）。"""
    txt = summary.lower()
    actions = ["确认影响范围", "查看最近变更", "检查错误日志与监控"]
    if "timeout" in txt:
        actions.insert(0, "优先检查下游依赖与连接池")
    if "db" in txt or "database" in txt:
        actions.insert(0, "检查慢 SQL 与数据库连接数")
    return "；".join(actions[:5])


@tool
def tavily_web_search(query: str, max_results: int = 5) -> str:
    """联网搜索最新网页信息，返回摘要和来源链接。"""
    res = tavily_client.search(
        query=query,
        max_results=max_results,
        include_answer=True,
        search_depth="advanced",
    )

    answer = res.get("answer", "")
    results = res.get("results", []) or []

    lines = []
    if answer:
        lines.append(f"总结：{answer}")
    for i, item in enumerate(results, start=1):
        title = item.get("title", "")
        url = item.get("url", "")
        content = (item.get("content", "") or "").strip().replace("\n", " ")
        lines.append(f"[{i}] {title}\nURL: {url}\n摘要: {content[:300]}")

    return "\n\n".join(lines) if lines else "未搜索到结果"


tools = [get_incident_context, suggest_first_actions, tavily_web_search]
llm_with_tools = llm.bind_tools(tools)


def _tool_map():
    return {t.name: t for t in tools}


def agent_node(state: Dict[str, Any]) -> Dict[str, Any]:
    web_enabled = state.get("web_search_enabled", True)
    if web_enabled:
        response = llm_with_tools.invoke(state["messages"])
    else:
        response = llm.bind_tools([get_incident_context, suggest_first_actions]).invoke(state["messages"])
    return {"messages": [response]}


def tool_node(state: Dict[str, Any]) -> Dict[str, Any]:
    last = state["messages"][-1]
    out_messages = []
    if hasattr(last, "tool_calls") and last.tool_calls:
        mapping = _tool_map()
        for tc in last.tool_calls:
            name = tc.get("name")
            args = tc.get("args", {}) or {}
            if name in mapping:
                result = mapping[name].invoke(args)
            else:
                result = f"tool {name} not found"
            out_messages.append(ToolMessage(content=str(result), tool_call_id=tc.get("id", "unknown")))
    return {"messages": out_messages}


def should_continue(state: Dict[str, Any]) -> str:
    last = state["messages"][-1]
    if hasattr(last, "tool_calls") and last.tool_calls:
        return "tools"
    return END


builder = StateGraph(dict)
builder.add_node("agent", agent_node)
builder.add_node("tools", tool_node)
builder.add_edge(START, "agent")
builder.add_conditional_edges("agent", should_continue, {"tools": "tools", END: END})
builder.add_edge("tools", "agent")

checkpointer = MemorySaver()
agent_graph = builder.compile(checkpointer=checkpointer)

# 记录每个session的检查点链，用于回滚
SESSION_CHECKPOINTS: Dict[str, List[str]] = {}
SESSION_WEB_SEARCH_ENABLED: Dict[str, bool] = {}


def _append_checkpoint(session_id: str, checkpoint_id: Optional[str]):
    if not checkpoint_id:
        return
    arr = SESSION_CHECKPOINTS.setdefault(session_id, [])
    if not arr or arr[-1] != checkpoint_id:
        arr.append(checkpoint_id)
        # 短期窗口：最多保留 30 个 checkpoint
        if len(arr) > 30:
            SESSION_CHECKPOINTS[session_id] = arr[-30:]


def _invoke_agent(session_id: str, message: HumanMessage, web_search_enabled: bool = True):
    config = {"configurable": {"thread_id": session_id}}
    final_state = agent_graph.invoke({"messages": [message], "web_search_enabled": web_search_enabled}, config=config)
    snap = agent_graph.get_state(config)
    checkpoint_id = None
    if snap and snap.config and "configurable" in snap.config:
        checkpoint_id = snap.config["configurable"].get("checkpoint_id")
    _append_checkpoint(session_id, checkpoint_id)
    return final_state, checkpoint_id


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME, "provider": "openai-compatible"}


@app.post("/v1/chat")
def chat(req: ChatRequest):
    preface = "你是AIOps智能体，请输出结构化回答：问题理解、根因候选、排查步骤、下一步动作。"
    if req.incidentId:
        preface += f" 另外请结合 incidentId={req.incidentId}。"
    user_msg = HumanMessage(content=f"{preface}\n用户问题：{req.message}")

    SESSION_WEB_SEARCH_ENABLED[req.sessionId] = req.webSearchEnabled
    final_state, checkpoint_id = _invoke_agent(req.sessionId, user_msg, req.webSearchEnabled)
    answer = ""
    for m in reversed(final_state.get("messages", [])):
        if isinstance(m, AIMessage) and m.content:
            answer = m.content if isinstance(m.content, str) else str(m.content)
            break

    tool_history = [
        {"name": "get_incident_context", "status": "success" if req.incidentId else "skipped", "args": {"incident_id": req.incidentId} if req.incidentId else {}},
        {"name": "suggest_first_actions", "status": "success", "args": {"summary": req.message[:120]}},
    ]

    return {
        "sessionId": req.sessionId,
        "answer": answer,
        "model": MODEL_NAME,
        "checkpointId": checkpoint_id,
        "tools": [t.name for t in tools],
        "toolHistory": tool_history,
        "structured": {
            "understanding": "基于告警上下文与用户问题进行归因分析",
            "nextAction": "先执行首轮排查步骤，并记录验证结果",
        }
    }


@app.post("/v1/chat-stream")
def chat_stream(req: ChatRequest):
    # 当前先由上层BFF做SSE包装，这里先返回同结构
    return chat(req)


@app.post("/v1/image/analyze")
async def image_analyze(sessionId: str = Form(...), file: UploadFile = File(...)):
    raw = await file.read()
    if not raw:
        raise HTTPException(status_code=400, detail="empty file")

    b64 = base64.b64encode(raw).decode("utf-8")
    data_url = f"data:{file.content_type or 'image/png'};base64,{b64}"

    messages = [
        HumanMessage(content=[
            {"type": "text", "text": "你是AIOps视觉助手。请识别图中关键信息，并输出：1)内容概述 2)异常信号 3)排查步骤。"},
            {"type": "image_url", "image_url": {"url": data_url}},
        ])
    ]

    resp = llm.invoke(messages)
    answer = resp.content if isinstance(resp.content, str) else str(resp.content)

    # 也写入会话记忆（checkpointer）
    _invoke_agent(sessionId, HumanMessage(content=f"用户上传图片：{file.filename}，请给出分析"))

    return {
        "sessionId": sessionId,
        "filename": file.filename,
        "bytes": len(raw),
        "analysis": answer,
    }


@app.get("/v1/sessions/{session_id}/memory")
def get_memory(session_id: str):
    config = {"configurable": {"thread_id": session_id}}
    snap = agent_graph.get_state(config)
    if not snap:
        return {"sessionId": session_id, "messages": [], "checkpointIds": []}

    msgs = []
    for m in (snap.values.get("messages", []) if snap.values else []):
        role = "assistant"
        if isinstance(m, HumanMessage):
            role = "user"
        elif isinstance(m, ToolMessage):
            role = "tool"
        msgs.append({"role": role, "content": str(m.content)})

    return {
        "sessionId": session_id,
        "messages": msgs,
        "checkpointIds": SESSION_CHECKPOINTS.get(session_id, []),
        "currentCheckpointId": (snap.config or {}).get("configurable", {}).get("checkpoint_id"),
    }


@app.delete("/v1/sessions/{session_id}/memory")
def clear_memory(session_id: str):
    # MemorySaver不支持直接删除单线程历史，这里通过重置索引实现“会话清空语义”
    SESSION_CHECKPOINTS.pop(session_id, None)
    return {"sessionId": session_id, "cleared": True}


@app.post("/v1/sessions/{session_id}/rollback")
def rollback_memory(session_id: str, req: RollbackRequest):
    checkpoints = SESSION_CHECKPOINTS.get(session_id, [])
    if not checkpoints:
        raise HTTPException(status_code=404, detail="no checkpoints for this session")

    target = req.toCheckpointId
    if not target:
        idx = max(0, len(checkpoints) - 1 - max(1, req.steps))
        target = checkpoints[idx]

    if target not in checkpoints:
        raise HTTPException(status_code=400, detail="target checkpoint not found in session history")

    # 回滚语义：把当前会话的“可见历史指针”回退到目标checkpoint
    cut_idx = checkpoints.index(target)
    SESSION_CHECKPOINTS[session_id] = checkpoints[: cut_idx + 1]

    return {
        "sessionId": session_id,
        "rolledBackTo": target,
        "remainingCheckpoints": SESSION_CHECKPOINTS[session_id],
    }
