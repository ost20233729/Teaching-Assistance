# AI 教学辅助平台

面向高校教师的教学内容生成与课程管理平台，将课程审核、教学目标、大纲、讲义、课件提纲、版本管理和大模型调用统计整合到统一工作流中。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot、MyBatis-Plus、Druid、Springdoc OpenAPI |
| 数据库 | MySQL 8 |
| 身份认证 | JWT、Spring MVC 拦截器 |
| AI 接入 | OpenAI 兼容接口、可配置模型与 Prompt 模板 |
| 前端 | Vue 3、TypeScript、Vite、Element Plus |
| 工程化 | Maven、Yarn、JUnit、MockMvc |

## 核心功能

- 教师注册、登录、JWT 身份认证和个人资料维护。
- 课程创建、编辑、查询、删除、提交审核和审核状态跟踪。
- 生成课程介绍、教学目标、课程大纲、教学讲义和课件提纲。
- 支持 Prompt 模板选择、生成参数配置、手动编辑和 Markdown 导出。
- 保存教学内容历史版本并支持版本查看与恢复。
- 管理员维护教师账号、审核课程、配置功能限制并发送通知。
- 统计用户、课程和大模型调用情况，展示成功率、模块分布与调用记录。

### 技术亮点

- 使用 `/api/v1` 组织教师端、管理员端和 LLM 工具接口，统一响应与异常处理。
- 将课程、教学目标、大纲、讲义、课件和历史版本拆分为独立领域模型，降低内容生成流程之间的耦合。
- 封装大模型配置、Prompt 模板和调用日志，支持切换 OpenAI 兼容服务地址与模型。
- 通过内容版本快照实现生成结果的可追溯和恢复，避免再次生成覆盖人工修改。
- 使用环境变量管理数据库和大模型凭据，真实外部 LLM 测试默认跳过，防止误消耗额度。

## 个人工作

本项目由本人独立完成，主要工作包括：

- 完成需求梳理、系统架构、数据库模型和前后端接口设计。
- 开发教师端课程管理、内容生成、版本恢复、通知和个人资料等后端模块。
- 开发管理员用户管理、课程审核、功能限制和调用统计模块。
- 接入 OpenAI 兼容大模型服务，整理 Prompt 模板并实现调用记录与异常处理。
- 完成 Vue 页面联调、接口测试、测试问题修复和公开仓库脱敏整理。

## 本地运行

### 环境要求

- JDK 17
- Maven 3.8+
- Node.js LTS
- Yarn 1.x
- MySQL 8

### 启动步骤

1. 配置环境变量：

```powershell
$env:TEACHING_DB_URL = "jdbc:mysql://localhost:3306/teaching_assistance?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:TEACHING_DB_USERNAME = "root"
$env:TEACHING_DB_PASSWORD = "你的数据库密码"
$env:OPENAI_API_URL = "https://你的大模型兼容接口/v1/chat/completions"
$env:OPENAI_MODEL = "你的模型名称"
$env:OPENAI_API_KEY = "你的大模型 API Key"
```

2. 按顺序执行数据库脚本：

```text
docs/database.sql
docs/demo-seed.sql
```

3. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

4. 启动前端：

```bash
cd frontend
yarn install
yarn dev
```

默认访问地址为 `http://localhost:5173`，后端默认监听 `http://localhost:8080`。

演示数据脚本提供本地账号 `admin / 123456` 和 `teacher / 123456`，仅用于本地演示。

## 项目结构

```text
Teaching-Assistance/
├── backend/
│   ├── src/main/java/              业务接口与服务
│   ├── src/main/resources/         配置、Prompt 与内容模板
│   └── src/test/                   单元测试与接口测试
├── frontend/                       Vue 3 前端
├── docs/                           数据库与演示数据脚本
├── .env.example                    环境变量示例
└── NOTICE.md                       开发与安全说明
```

## 测试与安全

- 后端执行 `mvn test`：`123` 项测试通过。
- `10` 项真实外部 LLM 集成测试按设计跳过，避免默认消耗外部额度。
- 前端执行 `yarn install --frozen-lockfile` 和 `yarn build`：生产构建通过。
- 数据库密码、API Key、`.env`、日志、IDE 配置和构建产物均不进入版本控制。
- 公开版本修复了控制器测试缺失 Mock、讲义生成参数缺失和非法 JSON 响应码等问题。
