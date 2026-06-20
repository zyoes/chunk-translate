# Chunk Translate — AI 智能文档翻译平台

支持大文档自动分块翻译、实时查看翻译结果以及导出译文的 Web 系统。

> 阿里云机器翻译 → DeepSeek AI 润色，两级翻译流水线。

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.5.14 | 应用框架 |
| **Java** | 17 | 运行环境 |
| **Maven** | wrapper | 构建工具 |
| **MySQL** | 8.x | 关系型数据库 |
| **Flyway** | — | 数据库版本迁移 |
| **MyBatis-Plus** | 3.5.7 | ORM 框架 |
| **Redis** | — | 缓存（验证码存储） |
| **Spring Security** | — | 认证与授权 |
| **OAuth2 Client** | — | GitHub 第三方登录 |
| **jjwt** | 0.12.6 | JWT 令牌签发与校验 |
| **Spring Boot Mail** | — | SMTP 邮件发送 |
| **Apache POI** | 5.2.5 | Word / PowerPoint 文档解析 |
| **Apache PDFBox** | 3.0.2 | PDF 文档解析 |
| **iTextPDF** | 5.5.13.4 | PDF 译文导出 |
| **阿里云 alimt SDK** | 1.5.2 | 机器翻译 API |
| **DeepSeek API** | — | AI 润色（兼容 OpenAI 格式） |
| **OkHttp** | 4.12.0 | HTTP 客户端（调用 AI 接口） |
| **Fastjson2** | 2.0.51 | JSON 序列化 |
| **SpringDoc OpenAPI** | 2.8.5 | Swagger UI 在线文档 |
| **spring-dotenv** | 5.1.0 | 开发环境 `.env` 加载 |
| **Lombok** | — | 简化代码 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue** | 3.5 | UI 框架（Composition API） |
| **Vite** | 8 | 构建工具 |
| **Element Plus** | 2.14 | 组件库 |
| **Vue Router** | 4.6 | 路由管理 |
| **Pinia** | 3 | 状态管理 |
| **Axios** | 1.18 | HTTP 请求 |

---

## 项目结构

```
chunk-translate/
├── src/main/java/com/example/chunktranslate/
│   ├── common/              # 通用组件（枚举、异常、统一响应体）
│   ├── config/              # Spring 配置（Security、Redis、线程池等）
│   ├── controller/          # REST 接口
│   ├── dto/                 # 请求/响应 DTO
│   ├── entity/              # 数据库实体
│   ├── mapper/              # MyBatis-Plus Mapper
│   ├── security/            # Spring Security 组件（JWT、OAuth2）
│   ├── service/
│   │   ├── auth/            # 认证服务
│   │   ├── document/        # 文档上传与解析
│   │   │   └── parser/      # 多格式解析器（策略模式）
│   │   ├── export/          # 译文导出
│   │   └── translation/     # 翻译流水线
│   └── util/                # 工具类
├── src/main/resources/
│   ├── application.yml      # 基础配置
│   ├── application-dev.yml  # 开发环境配置
│   └── db/migration/        # Flyway 数据库迁移脚本
├── frontend/
│   └── src/
│       ├── api/             # Axios 接口封装
│       ├── router/          # Vue Router 路由
│       └── views/           # 页面组件
├── .env.example             # 环境变量模板
└── pom.xml                  # Maven 依赖配置
```

---

## 快速开始

### 环境要求

- **JDK 17+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Node.js 20+**

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，填入你的数据库密码、API 密钥等
```

### 2. 启动后端

```bash
# 创建数据库（MySQL 中执行）
CREATE DATABASE IF NOT EXISTS chunk_translate DEFAULT CHARACTER SET utf8mb4;

# 启动 Spring Boot（Flyway 自动建表）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，后端 API 自动代理到 `http://localhost:8080`。

### 4. API 文档

后端启动后访问 `http://localhost:8080/swagger-ui.html`。

---

## 翻译流水线

```
上传文档 → 格式解析 → 分块拆分 → 阿里云机器翻译 → DeepSeek AI 润色 → 导出译文
```

- **阿里云机翻**：产出初版译文
- **DeepSeek 润色**：优化自然度和可读性（超过 8000 字符的分块跳过润色）
- 每个分块最多重试 3 次，支持中途取消

### 支持的文件格式

| 格式 | 解析策略 |
|------|----------|
| `.docx` | Apache POI，按标题层级构建嵌套树结构 |
| `.md` | 正则匹配标题语法，构建多级嵌套树 |
| `.pdf` | Apache PDFBox，按页拆分 |
| `.pptx` | Apache POI，按幻灯片拆分 |
| `.txt` | 按空行分段，自动识别段落标题 |

### 导出格式

TXT / Markdown / DOCX / PDF

---

## 默认账号

Flyway 迁移 `V7` 自动插入以下测试账号（密码已 BCrypt 加密）：

| 角色 | 邮箱 | 用户名 | 密码 |
|------|------|--------|------|
| 管理员 | admin@example.com | admin | admin123 |
| 普通用户 | user@example.com | user | user123 |

## 认证机制

JWT 双令牌认证：

- **Access Token**（JWT，15 分钟有效）：附加在 `Authorization: Bearer` 头中，每次请求验证
- **Refresh Token**（UUID，7 天有效）：存储在 DB + 前端，access token 过期后静默换新
- **轮换机制**：每次刷新吊销旧 token、签发新 token，防止重放攻击
- 支持 GitHub OAuth2 第三方登录和邮箱验证码注册

## 后台管理

管理员账号（admin@example.com）登录后，Header 左侧会出现「后台管理」入口：

- **系统概览**：用户总数 / 文档总数 / 翻译任务 / 已完成
- **用户管理**：列表查看、启用/禁用、重置密码
- **文档管理**：列表查看（含上传者）、软删除
- **任务管理**：列表查看（含文档名和上传者）、删除（不影响文档）

## 功能概览

- 文档上传与多格式解析
- 文档目录树可视化
- 翻译进度实时查看
- 按分块编辑译文
- GitHub OAuth2 登录 + JWT 双令牌认证
- 邮箱验证码注册 / 密码重置
- 个人主页：资料编辑、头像上传、翻译历史
- 译文多格式导出（TXT / Markdown / DOCX / PDF）
- 后台管理系统（统计、用户管理、文档/任务管理）
