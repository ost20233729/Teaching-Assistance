# AI 教学辅助平台

面向高校教师的 AI 教学内容生成与课程管理平台。系统采用 Vue 3、Spring Boot、MyBatis-Plus、MySQL 与 OpenAI 兼容大模型接口，实现课程创建、审核、教学目标生成、课程大纲生成、讲义生成、课件提纲生成、版本恢复和调用统计等功能。

> 本项目由本人独立完成需求分析、系统设计、前后端开发、AI 接口接入、联调测试与工程化整理。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Element Plus |
| 后端 | Java 17、Spring Boot、MyBatis-Plus |
| 数据库 | MySQL 8 |
| 身份认证 | JWT |
| AI 服务 | OpenAI 兼容接口、可配置模型与服务地址 |
| 工程工具 | Maven、Yarn |

## 核心功能

### 教师端

- 教师注册、登录与 JWT 身份认证
- 课程创建、编辑、删除、查询和审核状态查看
- AI 生成课程介绍与教学目标
- AI 生成课程大纲、教学讲义和课件提纲
- Prompt 模板选择与生成参数配置
- 教学内容手动编辑、保存和 Markdown 导出
- 教学内容历史版本查看与恢复
- 课程审核结果和功能限制通知
- 教师个人资料维护

### 管理员端

- 教师账号创建、查询、编辑和删除
- 课程审核、审核意见填写和状态管理
- 按教师配置功能使用限制
- 用户、课程和大模型调用统计
- LLM 调用成功率、模块分布和最近调用记录查看

## 接口结构

系统接口统一使用 `/api/v1` 前缀：

| 接口分组 | 路径前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/v1/auth` | 教师登录、管理员登录、注册和账号检查 |
| 教师端 | `/api/v1/teacher` | 课程、教学内容、模板、通知和个人资料 |
| 管理员端 | `/api/v1/admin` | 用户管理、课程审核、限制管理和统计 |
| LLM 工具 | `/api/v1/llm` | 内容生成、Markdown 转换和健康检查 |

除登录、注册和健康检查外，其余接口需要携带：

```http
Authorization: Bearer <token>
```

## 项目结构

```text
Teaching-Assistance/
├── frontend/                       Vue 3 前端
├── backend/                        Spring Boot 后端
│   ├── src/main/java/              业务代码
│   ├── src/main/resources/         配置与 Prompt 模板
│   └── src/test/                   单元测试与集成测试
├── docs/                           数据库脚本与设计文档
├── .env.example                    环境变量示例
└── README.md                       项目说明
```

## 快速开始

### 1. 环境要求

- JDK 17
- Maven 3.8 或更高版本
- Node.js LTS
- Yarn 1.x
- MySQL 8

### 2. 配置环境变量

项目不会在代码中保存数据库密码或大模型 API Key。需要配置以下环境变量：

- `TEACHING_DB_URL`：MySQL JDBC 地址
- `TEACHING_DB_USERNAME`：MySQL 用户名
- `TEACHING_DB_PASSWORD`：MySQL 密码
- `OPENAI_API_URL`：OpenAI 兼容接口地址
- `OPENAI_MODEL`：模型名称
- `OPENAI_API_KEY`：大模型 API Key

Windows PowerShell 示例：

```powershell
$env:TEACHING_DB_USERNAME = "root"
$env:TEACHING_DB_PASSWORD = "你的数据库密码"
$env:OPENAI_API_KEY = "你的大模型 API Key"
```

> `.env.example` 仅用于展示变量名称，不包含真实密码或 Key。真实配置不得提交到 Git。

### 3. 初始化数据库

按顺序执行：

1. `docs/database.sql`
2. `docs/demo-seed.sql`

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

### 5. 启动前端

```bash
cd frontend
yarn install
yarn dev
```

默认地址：`http://localhost:5173`

## 演示账号

执行演示数据脚本后可使用：

- 管理员：`admin / 123456`
- 教师：`teacher / 123456`

以上账号仅用于本地演示，不应部署到生产环境。

## 测试与验证

本展示版本已完成以下验证：

- 后端执行 `mvn test`：`123` 项测试通过
- 真实外部 LLM 集成测试：`10` 项按设计跳过，避免默认消耗外部额度
- 前端执行 `yarn install --frozen-lockfile` 和 `yarn build`：构建成功

展示版本额外修复了以下问题：

- 补齐控制器测试缺失的 `MaterialService` Mock
- 修正讲义生成测试缺少 `teacherId` 参数的问题
- 将非法 JSON 请求正确映射为 `400 Bad Request`
- 将数据库和大模型配置改为环境变量读取

## 敏感信息处理

- 删除真实大模型 API Key
- 删除数据库真实密码和本地配置
- 删除课程报告、日志、IDE 配置和构建产物
- 不复制原仓库 Git 历史，避免旧凭据继续出现在历史提交中
- 默认禁用真实外部 LLM 调用测试

## 开发说明

本项目的核心功能与工程实现由本人独立完成，包括业务需求梳理、系统架构设计、数据库设计、Spring Boot 后端、Vue 前端、大模型接口接入、接口联调、测试验证和仓库脱敏整理。

更详细的开发与安全说明见 `NOTICE.md`。
