CREATE DATABASE IF NOT EXISTS aiops DEFAULT CHARACTER SET utf8mb4;
USE aiops;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ops_service (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(128) NOT NULL,
  owner VARCHAR(64),
  environment VARCHAR(32) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_service_env(service_name, environment)
);

CREATE TABLE IF NOT EXISTS ops_log_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(128) NOT NULL,
  environment VARCHAR(32) NOT NULL,
  level VARCHAR(16) NOT NULL,
  message TEXT NOT NULL,
  trace_id VARCHAR(128),
  event_time BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_service_time(service_name, event_time),
  INDEX idx_trace_id(trace_id)
);

CREATE TABLE IF NOT EXISTS ops_alert (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(128) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  title VARCHAR(255) NOT NULL,
  detail TEXT,
  rule_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_alert_status(status, created_at)
);

CREATE TABLE IF NOT EXISTS ops_alert_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(128) NOT NULL,
  error_rate_threshold DECIMAL(6,2) NOT NULL,
  latency_threshold_ms BIGINT NOT NULL,
  dedup_window_sec BIGINT NOT NULL,
  suppress_window_sec BIGINT NOT NULL,
  enabled CHAR(1) NOT NULL DEFAULT 'Y',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_service(service_name)
);

CREATE TABLE IF NOT EXISTS ops_incident (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  incident_no VARCHAR(64) NOT NULL UNIQUE,
  alert_id BIGINT,
  summary VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL,
  assignee VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_incident_status(status, updated_at)
);

CREATE TABLE IF NOT EXISTS ops_incident_timeline (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  incident_id BIGINT NOT NULL,
  from_status VARCHAR(32) NOT NULL,
  to_status VARCHAR(32) NOT NULL,
  operator_name VARCHAR(64) NOT NULL,
  remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_incident_timeline(incident_id, created_at)
);

CREATE TABLE IF NOT EXISTS ops_ai_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  incident_id BIGINT NOT NULL,
  confidence DECIMAL(5,2),
  root_cause TEXT,
  suggestion TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_incident_id(incident_id)
);

CREATE TABLE IF NOT EXISTS wx_user_bind (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  open_id VARCHAR(128) NOT NULL UNIQUE,
  union_id VARCHAR(128),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_wx_username(username)
);

CREATE TABLE IF NOT EXISTS monitor_target (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(128) NOT NULL,
  target_url VARCHAR(500) NOT NULL,
  target_host VARCHAR(128),
  target_port INT,
  protocol VARCHAR(16) NOT NULL DEFAULT 'HTTP',
  expected_status VARCHAR(32) NOT NULL DEFAULT '2xx',
  timeout_ms INT NOT NULL DEFAULT 3000,
  interval_sec INT NOT NULL DEFAULT 30,
  enabled CHAR(1) NOT NULL DEFAULT 'Y',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_monitor_target_enabled(enabled),
  INDEX idx_monitor_target_service(service_name),
  INDEX idx_monitor_target_protocol(protocol)
);

CREATE TABLE IF NOT EXISTS ops_metric_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_name VARCHAR(128) NOT NULL,
  target_id BIGINT,
  metric_type VARCHAR(64) NOT NULL,
  metric_value DECIMAL(12,3) NOT NULL,
  status VARCHAR(16) NOT NULL,
  probe_time BIGINT NOT NULL,
  detail VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_metric_service_time(service_name, probe_time),
  INDEX idx_metric_target_time(target_id, probe_time)
);

CREATE TABLE IF NOT EXISTS notify_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT,
  content VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notify_user_time(username, created_at)
);

-- 已存在库升级时执行（MySQL 8+）
ALTER TABLE monitor_target ADD COLUMN IF NOT EXISTS target_host VARCHAR(128);
ALTER TABLE monitor_target ADD COLUMN IF NOT EXISTS target_port INT;
ALTER TABLE monitor_target ADD COLUMN IF NOT EXISTS protocol VARCHAR(16) NOT NULL DEFAULT 'HTTP';
CREATE INDEX IF NOT EXISTS idx_monitor_target_protocol ON monitor_target(protocol);
